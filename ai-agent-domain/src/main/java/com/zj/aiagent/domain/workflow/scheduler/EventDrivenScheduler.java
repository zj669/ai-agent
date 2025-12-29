package com.zj.aiagent.domain.workflow.scheduler;

import com.alibaba.fastjson2.JSONObject;
import com.zj.aiagent.domain.context.IContextProvider;
import com.zj.aiagent.domain.workflow.entity.*;
import com.zj.aiagent.domain.workflow.entity.config.FallbackConfig;
import com.zj.aiagent.domain.workflow.entity.config.RetryConfig;
import com.zj.aiagent.domain.workflow.interfaces.Checkpointer;
import com.zj.aiagent.domain.workflow.interfaces.ConditionalEdge;
import com.zj.aiagent.domain.workflow.interfaces.NodeExecutionInterceptor;
import com.zj.aiagent.shared.constants.WorkflowRunningConstants;
import com.zj.aiagent.shared.design.workflow.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class EventDrivenScheduler implements WorkflowScheduler {
    private final ExecutorService executor;
    private final IContextProvider contextProvider;
    private final Checkpointer checkpointer;
    private final ConditionalEdge conditionalEdge;
    private final List<NodeExecutionInterceptor> interceptors;

    public EventDrivenScheduler(
            ExecutorService executorService,
            IContextProvider contextProvider,
            Checkpointer checkpointer,
            ConditionalEdge conditionalEdge,
            List<NodeExecutionInterceptor> interceptors) {
        this.executor = executorService;
        this.contextProvider = contextProvider;
        this.checkpointer = checkpointer;
        this.conditionalEdge = conditionalEdge;
        this.interceptors = interceptors != null ? interceptors : new ArrayList<>();
    }

    private final ConcurrentHashMap<String, ExecutionControl> activeExecutions = new ConcurrentHashMap<>();

    private static class ExecutionControl {
        final CountDownLatch latch;
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        final AtomicReference<ExecutionResult> resultRef;

        ExecutionControl(CountDownLatch latch, AtomicReference<ExecutionResult> resultRef) {
            this.latch = latch;
            this.resultRef = resultRef;
        }
    }

    @PostConstruct
    private void init() {
        // 按优先级排序拦截器
        interceptors.sort(Comparator.comparingInt(NodeExecutionInterceptor::getOrder));
        log.info("已注册 {} 个拦截器", interceptors.size());
    }

    @Override
    public ExecutionResult execute(WorkflowGraph graph, WorkflowState initialState) {
        log.info("事件驱动调度器开始执行, graphId: {}", graph.getDagId());

        // 注册 Reducer（根据 StateSchema 或默认配置）
        registerDefaultReducers(initialState);

        DependencyTracker dependencyTracker = new DependencyTracker(graph);
        ConcurrentHashMap<String, Exception> failures = new ConcurrentHashMap<>();
        AtomicReference<String> pausedNodeId = new AtomicReference<>();
        AtomicInteger activeCount = new AtomicInteger(0);
        CountDownLatch completionLatch = new CountDownLatch(1);
        AtomicReference<ExecutionResult> resultRef = new AtomicReference<>();

        String conversationId = initialState.get(WorkflowRunningConstants.Workflow.EXECUTION_ID_KEY, String.class);
        if (conversationId != null) {
            activeExecutions.put(conversationId, new ExecutionControl(completionLatch, resultRef));
        }

        // 通知工作流开始
        WorkflowStateListener listener = initialState.getWorkflowStateListener();
        if (listener != null) {
            listener.onWorkflowStarted(graph.getNodes().size());
        }

        try {
            Set<String> readyNodes = dependencyTracker.getReadyNodes();

            if (readyNodes.isEmpty()) {
                log.error("没有可执行的起始节点，工作流无法启动");
                return ExecutionResult.error(null, "No ready nodes found - workflow cannot start");
            }

            for (String nodeId : readyNodes) {
                submitNode(nodeId, graph, initialState, dependencyTracker, failures, pausedNodeId,
                        activeCount, completionLatch, resultRef);
            }

            completionLatch.await();

            // 获取执行结果
            ExecutionResult result = resultRef.get();

            // 通知工作流完成（error为null表示成功）
            if (listener != null) {
                boolean success = (result != null && result.getError() == null);
                listener.onWorkflowCompleted(success);
            }

            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (listener != null) {
                listener.onWorkflowFailed(e);
            }
            return ExecutionResult.error(null, "Execution interrupted");
        } finally {
            if (conversationId != null) {
                activeExecutions.remove(conversationId);
            }
        }
    }

    @Override
    public ExecutionResult resume(WorkflowGraph graph, WorkflowState state, String fromNodeId) {
        log.info("事件驱动调度器恢复执行, graphId: {}, fromNodeId: {}", graph.getDagId(), fromNodeId);

        // 1. 注册默认 Reducer（确保状态合并策略一致）
        registerDefaultReducers(state);

        // 2. 创建依赖跟踪器并重建状态
        DependencyTracker dependencyTracker = new DependencyTracker(graph);

        // 3. 标记 fromNodeId 之前的所有上游节点为已完成
        markUpstreamCompleted(graph, fromNodeId, dependencyTracker);

        // 4. 初始化并发控制
        ConcurrentHashMap<String, Exception> failures = new ConcurrentHashMap<>();
        AtomicReference<String> pausedNodeId = new AtomicReference<>();
        AtomicInteger activeCount = new AtomicInteger(0);
        CountDownLatch completionLatch = new CountDownLatch(1);
        AtomicReference<ExecutionResult> resultRef = new AtomicReference<>();

        // 5. 从指定节点开始执行
        // 5. 从指定节点开始执行
        String conversationId = state.get(WorkflowRunningConstants.Workflow.EXECUTION_ID_KEY, String.class);
        if (conversationId != null) {
            activeExecutions.put(conversationId, new ExecutionControl(completionLatch, resultRef));
        }

        try {
            // 验证恢复节点是否存在
            if (!dependencyTracker.hasNode(fromNodeId)) {
                log.error("恢复节点不存在: fromNodeId={}", fromNodeId);
                return ExecutionResult.error(fromNodeId, "Resume node not found: " + fromNodeId);
            }

            submitNode(fromNodeId, graph, state, dependencyTracker, failures, pausedNodeId,
                    activeCount, completionLatch, resultRef);

            completionLatch.await();
            return resultRef.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ExecutionResult.error(fromNodeId, "Resume execution interrupted");
        } finally {
            if (conversationId != null) {
                activeExecutions.remove(conversationId);
            }
        }
    }

    @Override
    public void cancel(String conversationId) {
        ExecutionControl control = activeExecutions.get(conversationId);
        if (control != null) {
            log.info("取消工作流执行: {}", conversationId);
            control.cancelled.set(true);
            control.resultRef.set(ExecutionResult.error(null, "Execution cancelled by user"));
            control.latch.countDown();
        } else {
            log.warn("尝试取消不存在或已结束的工作流: {}", conversationId);
        }
    }

    /**
     * 标记指定节点的所有上游节点为已完成
     * <p>
     * 使用 BFS 反向遍历依赖图，找出所有需要标记为完成的节点
     */
    private void markUpstreamCompleted(WorkflowGraph graph, String targetNodeId, DependencyTracker tracker) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        // 从目标节点的依赖开始
        List<String> dependencies = graph.getNodeDependencies(targetNodeId);
        queue.addAll(dependencies);

        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            if (visited.contains(nodeId)) {
                continue;
            }
            visited.add(nodeId);

            // 标记为已完成（不触发下游节点）
            tracker.markCompletedWithoutPropagate(nodeId);
            log.debug("恢复执行: 标记节点 {} 为已完成", nodeId);

            // 继续遍历该节点的依赖
            List<String> upstreamNodes = graph.getNodeDependencies(nodeId);
            queue.addAll(upstreamNodes);
        }

        log.info("恢复执行: 已标记 {} 个上游节点为完成状态", visited.size());
    }

    private void submitNode(String nodeId, WorkflowGraph graph, WorkflowState state,
            DependencyTracker tracker,
            ConcurrentHashMap<String, Exception> failures,
            AtomicReference<String> pausedNodeId,
            AtomicInteger activeCount,
            CountDownLatch completionLatch,
            AtomicReference<ExecutionResult> resultRef) {
        activeCount.incrementAndGet();
        WorkflowStateListener listener = state.getWorkflowStateListener();
//        CompletableFuture.runAsync(() -> {
            try {
                NodeExecutor node = graph.getNodes().get(nodeId);
                JSONObject nodeConfig = graph.getNodeConfigs().getOrDefault(nodeId, new JSONObject());

                // 检查是否已取消
                String conversationId = state.get(WorkflowRunningConstants.Workflow.EXECUTION_ID_KEY, String.class);
                if (conversationId != null) {
                    ExecutionControl control = activeExecutions.get(conversationId);
                    if (control != null && control.cancelled.get()) {
                        log.info("节点 {} 取消执行: 工作流已被取消", nodeId);
                        return;
                    }
                }

                // 构建执行上下文
                NodeExecutionContext context = NodeExecutionContext.builder()
                        .nodeId(nodeId)
                        .nodeName(node.getNodeName())
                        .nodeType(node.getClass().getSimpleName())
                        .state(state)
                        .executor(node)
                        .nodeConfig(nodeConfig)
                        .currentRetryCount(0)
                        .build();

                // ========== 前置拦截器链 ==========
                for (NodeExecutionInterceptor interceptor : interceptors) {
                    InterceptResult result = interceptor.beforeExecution(context);
                    if (result.shouldPause()) {
                        handlePause(nodeId, result, pausedNodeId, resultRef, completionLatch);
                        return;
                    }
                    if (result.shouldSkip()) {
                        log.info("节点 {} 被跳过: {}", nodeId, result.getReason());
                        // 跳过节点，直接标记完成
                        tracker.markCompleted(nodeId);
                        return;
                    }
                    if (result.getAction() == InterceptResult.InterceptAction.ERROR) {
                        handleInterceptError(nodeId, result, failures, resultRef, completionLatch);
                        return;
                    }
                }

                // ========== 执行节点（带重试） ==========
                long startTime = System.currentTimeMillis();
                listener.onNodeStarted(nodeId, node.getNodeName());

                // 再次检查取消状态
                if (conversationId != null) {
                    ExecutionControl control = activeExecutions.get(conversationId);
                    if (control != null && control.cancelled.get()) {
                        log.info("节点 {} 取消执行: 工作流已被取消", nodeId);
                        return;
                    }
                }

                StateUpdate update = executeWithRetry(context);
                long durationMs = System.currentTimeMillis() - startTime;
                listener.onNodeCompleted(nodeId, node.getNodeName(), state, durationMs);

                // 【优化】保存节点元数据，用于上下文展示
                state.put(nodeId + "_name", node.getNodeName());
                state.put(nodeId + "_timestamp", System.currentTimeMillis());
                log.debug("[{}] 保存节点元数据: name={}, timestamp={}",
                        conversationId, node.getNodeName(), System.currentTimeMillis());
                // ========== 后置拦截器链 ==========
                for (NodeExecutionInterceptor interceptor : interceptors) {
                    InterceptResult result = interceptor.afterExecution(context, update);
                    if (result.shouldPause()) {
                        handlePause(nodeId, result, pausedNodeId, resultRef, completionLatch);
                        return;
                    }
                }

                // ========== 检查节点自身的信号 ==========
                if (update.getSignal() == ControlSignal.PAUSE) {
                    pausedNodeId.set(nodeId);
                    resultRef.set(ExecutionResult.pause(nodeId));
                    completionLatch.countDown();
                    return;
                }
                if (update.getSignal() == ControlSignal.ERROR) {
                    failures.put(nodeId, new RuntimeException(update.getMessage()));
                    resultRef.set(ExecutionResult.error(nodeId, update.getMessage()));
                    completionLatch.countDown();
                    return;
                }

                // 3. 标记完成，获取下游节点
                Set<String> nextNodes = tracker.markCompleted(nodeId);

                // 4. 处理条件边
                Map<String, EdgeDefinitionEntity> edgeMap = graph.getNextNodes(nodeId);
                List<RouterEntity> routerEntities = new ArrayList<>();
                List<String> conditionalNext = new ArrayList<>();
                if (edgeMap.size() > 1) {
                    for (Map.Entry<String, EdgeDefinitionEntity> entry : edgeMap.entrySet()) {
                        EdgeDefinitionEntity edge = entry.getValue();
                        String condition = edge.getCondition();
                        if (condition == null) {
                            condition = graph.getNodes().get(edge.getTarget()).getDescription();
                        }
                        routerEntities.add(RouterEntity.builder()
                                .condition(condition)
                                .nodeId(edge.getTarget())
                                .build());
                    }
                    List<String> evaluate = conditionalEdge.evaluate(state, routerEntities);
                    conditionalNext.addAll(new ArrayList<>(evaluate));
                } else {
                    conditionalNext.addAll(new ArrayList<>(edgeMap.keySet()));
                }
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
//        }, executor);
    }

    /**
     * 带重试的节点执行
     */
    private StateUpdate executeWithRetry(NodeExecutionContext context) {
        RetryConfig retryConfig = context.getConfig("RETRY", RetryConfig.class);

        if (retryConfig == null || !Boolean.TRUE.equals(retryConfig.getEnabled())) {
            // 无重试配置，直接执行
            return executeNodeOnce(context);
        }

        int maxRetries = retryConfig.getMaxRetries() != null ? retryConfig.getMaxRetries() : 3;
        long backoffMs = retryConfig.getBackoffMs() != null ? retryConfig.getBackoffMs() : 1000L;
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                context.setCurrentRetryCount(attempt);
                return executeNodeOnce(context);
            } catch (Exception e) {
                lastException = e;
                log.warn("节点 {} 执行失败（第 {}/{} 次）: {}",
                        context.getNodeId(), attempt + 1, maxRetries + 1, e.getMessage());

                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(backoffMs * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // 重试耗尽，尝试兜底
        FallbackConfig fallbackConfig = context.getConfig("FALLBACK", FallbackConfig.class);
        if (fallbackConfig != null && Boolean.TRUE.equals(fallbackConfig.getEnabled())) {
            return executeFallback(fallbackConfig, context);
        }

        String errorMsg = lastException != null ? lastException.getMessage() : "Unknown error";
        return StateUpdate.error("节点执行失败，已重试 " + maxRetries + " 次: " + errorMsg);
    }

    /**
     * 执行一次节点
     */
    private StateUpdate executeNodeOnce(NodeExecutionContext context) {
        String executionId = context.getState().get(WorkflowRunningConstants.Workflow.EXECUTION_ID_KEY, String.class);

        // 加载上下文
        ConcurrentHashMap<String, Object> stateData = contextProvider.loadContext(executionId,
                context.getState().getAll());
        context.getState().update(stateData);

        // 执行节点
        StateUpdate update = context.getExecutor().execute(context.getState());

        // 更新上下文
        context.getState().apply(update);
        contextProvider.saveContext(executionId, context.getState().getAll());

        // 检查点
        checkpointer.save(executionId, context.getNodeId(), context.getState());

        return update;
    }

    /**
     * 执行兜底策略
     */
    private StateUpdate executeFallback(FallbackConfig config, NodeExecutionContext context) {
        log.info("节点 {} 执行兜底策略: {}", context.getNodeId(), config.getStrategy());

        if ("DEFAULT_RESPONSE".equals(config.getStrategy())) {
            return StateUpdate.of(Map.of("fallback_message", config.getDefaultMessage()));
        }

        // 未来可以支持切换到备用节点
        return StateUpdate.error("兜底策略未实现: " + config.getStrategy());
    }

    /**
     * 处理暂停
     */
    private void handlePause(String nodeId, InterceptResult result,
            AtomicReference<String> pausedNodeId,
            AtomicReference<ExecutionResult> resultRef,
            CountDownLatch completionLatch) {
        pausedNodeId.set(nodeId);
        resultRef.set(ExecutionResult.builder()
                .nodeId(nodeId)
                .pauseReason(result.getReason())
                .metadata(result.getMetadata())
                .build());
        completionLatch.countDown();
    }

    /**
     * 处理拦截错误
     */
    private void handleInterceptError(String nodeId, InterceptResult result,
            ConcurrentHashMap<String, Exception> failures,
            AtomicReference<ExecutionResult> resultRef,
            CountDownLatch completionLatch) {
        failures.put(nodeId, new RuntimeException(result.getReason()));
        resultRef.set(ExecutionResult.error(nodeId, result.getReason()));
        completionLatch.countDown();
    }

    /**
     * 注册默认 Reducer
     * <p>
     * 为常见的 State Key 注册合并策略
     * <p>
     * 未来可以根据 WorkflowGraph.getStateSchema() 动态注册
     */
    private void registerDefaultReducers(WorkflowState state) {
        // 执行历史：追加列表
        state.registerReducer(WorkflowRunningConstants.ReAct.EXECUTION_HISTORY_KEY,
                BuiltInReducers.appendList());

        // 思考历史：追加列表
        state.registerReducer(WorkflowRunningConstants.Reflection.THOUGHT_HISTORY_KEY,
                BuiltInReducers.appendList());

        // 行动历史：追加列表
        state.registerReducer(WorkflowRunningConstants.Reflection.ACTION_HISTORY_KEY,
                BuiltInReducers.appendList());

        // 循环计数：累加
        state.registerReducer(WorkflowRunningConstants.Reflection.LOOP_COUNT_KEY,
                BuiltInReducers.increment());

        state.registerReducer(WorkflowRunningConstants.ReAct.REACT_LOOP_COUNT_KEY,
                BuiltInReducers.increment());

        log.debug("已注册 {} 个默认 Reducer", 5);
    }
}
