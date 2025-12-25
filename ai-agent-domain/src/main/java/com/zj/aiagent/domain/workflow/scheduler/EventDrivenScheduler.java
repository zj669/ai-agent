package com.zj.aiagent.domain.workflow.scheduler;

import com.zj.aiagent.domain.agent.dag.entity.EdgeType;
import com.zj.aiagent.domain.workflow.base.ControlSignal;
import com.zj.aiagent.domain.workflow.base.NodeExecutor;
import com.zj.aiagent.domain.workflow.base.StateUpdate;
import com.zj.aiagent.domain.workflow.base.WorkflowState;
import com.zj.aiagent.domain.workflow.entity.EdgeDefinitionEntity;
import com.zj.aiagent.domain.workflow.entity.ExecutionResult;
import com.zj.aiagent.domain.workflow.entity.RouterEntity;
import com.zj.aiagent.domain.workflow.entity.WorkflowGraph;
import com.zj.aiagent.domain.workflow.interfaces.Checkpointer;
import com.zj.aiagent.domain.workflow.interfaces.ConditionalEdge;
import com.zj.aiagent.domain.workflow.interfaces.ContextProvider;
import com.zj.aiagent.domain.workflow.interfaces.WorkflowStateListener;
import com.zj.aiagent.shared.constants.WorkflowRunningConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RequiredArgsConstructor
public class EventDrivenScheduler implements WorkflowScheduler {
    private final ExecutorService executor;
    private final ContextProvider contextProvider;
    private final WorkflowStateListener listener;
    private final Checkpointer checkpointer;
    private final ConditionalEdge conditionalEdge;

    @Override
    public ExecutionResult execute(WorkflowGraph graph, WorkflowState initialState) {
        log.info("事件驱动调度器开始执行, graphId: {}", graph.getDagId());
        DependencyTracker dependencyTracker = new DependencyTracker(graph);
        ConcurrentHashMap<String, Exception> failures = new ConcurrentHashMap<>();
        AtomicReference<String> pausedNodeId = new AtomicReference<>();
        AtomicInteger activeCount = new AtomicInteger(0);
        CountDownLatch completionLatch = new CountDownLatch(1);
        AtomicReference<ExecutionResult> resultRef = new AtomicReference<>();

        for (String nodeId : dependencyTracker.getReadyNodes()) {
            submitNode(nodeId, graph, initialState, dependencyTracker, failures, pausedNodeId,
                    activeCount, completionLatch, resultRef);
        }

        try {
            completionLatch.await();
            return resultRef.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ExecutionResult.error(null, "Execution interrupted");
        }
    }

    @Override
    public ExecutionResult resume(WorkflowGraph graph, WorkflowState state, String fromNodeId) {
        return null;
    }

    private void submitNode(String nodeId, WorkflowGraph graph, WorkflowState state,
            DependencyTracker tracker,
            ConcurrentHashMap<String, Exception> failures,
            AtomicReference<String> pausedNodeId,
            AtomicInteger activeCount,
            CountDownLatch completionLatch,
            AtomicReference<ExecutionResult> resultRef) {
        activeCount.incrementAndGet();
        CompletableFuture.runAsync(() -> {
            try {
                NodeExecutor node = graph.getNodes().get(nodeId);
                String executionId = state.get(WorkflowRunningConstants.Workflow.EXECUTION_ID_KEY, String.class);
                // 1. 执行节点
                listener.onNodeStarted(nodeId, node.getNodeName());
                // 加载上下文
                ConcurrentHashMap<String, Object> context = contextProvider.loadContext(executionId, state.getAll());
                state.update(context);
                StateUpdate update = node.execute(state, listener);
                // 更新上下文
                state.apply(update);
                contextProvider.saveContext(executionId, state.getAll());
                // 检查点
                checkpointer.save(executionId, nodeId, state);
                listener.onNodeCompleted(nodeId, state);

                // 2. 检查信号
                if (update.getSignal() == ControlSignal.PAUSE) {
                    pausedNodeId.set(nodeId);
                    resultRef.set(ExecutionResult.pause(nodeId));
                    completionLatch.countDown(); // 立即唤醒主线程
                    return;
                }
                if (update.getSignal() == ControlSignal.ERROR) {
                    failures.put(nodeId, new RuntimeException(update.getMessage()));
                    resultRef.set(ExecutionResult.error(nodeId, update.getMessage()));
                    completionLatch.countDown(); // 立即唤醒主线程
                    return;
                }

                // 3. 标记完成，获取下游节点
                Set<String> nextNodes = tracker.markCompleted(nodeId);

                // 4. 处理条件边
                Map<String, EdgeDefinitionEntity> edgeMap = graph.getNextNodes(nodeId);
                List<RouterEntity> routerEntities = new ArrayList<>();
                List<String> conditionalNext = new ArrayList<>();
                for (Map.Entry<String, EdgeDefinitionEntity> entry : edgeMap.entrySet()) {
                    EdgeDefinitionEntity edge = entry.getValue();
                    if (edge.getEdgeType() == EdgeType.CONDITIONAL) {
                        routerEntities.add(RouterEntity.builder()
                                .condition(edge.getCondition())
                                .nodeId(edge.getTarget())
                                .build());
                    } else {
                        conditionalNext.add(entry.getKey());
                    }
                }
                List<String> evaluate = conditionalEdge.evaluate(state, routerEntities);
                conditionalNext.addAll(new ArrayList<>(evaluate));
                for (String next : conditionalNext) {
                    if (tracker.isCompleted(next)) {
                        tracker.resetForLoop(next); // 循环
                    }
                    nextNodes.add(next);
                }
                // 5. 递归提交下游节点
                for (String next : nextNodes) {
                    submitNode(next, graph, state, tracker, failures,
                            pausedNodeId, activeCount, completionLatch, resultRef);
                }

            } catch (Exception e) {
                failures.put(nodeId, e);
                resultRef.set(ExecutionResult.error(nodeId, e.getMessage()));
                completionLatch.countDown(); // 异常时唤醒主线程
            } finally {
                int remaining = activeCount.decrementAndGet();
                // 所有任务完成时唤醒主线程
                if (remaining == 0 && tracker.isAllCompleted()) {
                    resultRef.set(ExecutionResult.success(state));
                    completionLatch.countDown();
                }
            }
        }, executor);
    }

    private void updateListenerState(WorkflowState state, AtomicInteger activeCount){
        // todo
    }

}
