package com.zj.aiagent.domain.agent.dag.executor;

import com.zj.aiagent.domain.agent.dag.context.DagExecutionContext;
import com.zj.aiagent.domain.agent.dag.context.ProgressData;
import com.zj.aiagent.domain.agent.dag.entity.DagGraph;
import com.zj.aiagent.domain.agent.dag.node.AbstractConfigurableNode;
import com.zj.aiagent.shared.design.dag.DagNodeExecutionException;
import com.zj.aiagent.shared.design.dag.NodeExecutionResult;
import com.zj.aiagent.shared.design.dag.NodeRouteDecision;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DAG事件驱动调度器
 * 基于依赖计数触发的细粒度并行调度
 */
@Slf4j
public class DagEventDrivenScheduler {

    private final ExecutorService executor;
    private final int maxParallelism;

    public DagEventDrivenScheduler(int maxParallelism) {
        this.maxParallelism = maxParallelism;
        this.executor = new ThreadPoolExecutor(
                maxParallelism,
                maxParallelism,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger(0);

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "DAG-EventDriven-" + counter.getAndIncrement());
                    }
                });
    }

    /**
     * 执行DAG
     * 
     * @param dagGraph DAG图
     * @param context  执行上下文
     * @return 执行结果
     */
    public SchedulerExecutionResult execute(DagGraph dagGraph, DagExecutionContext context) {
        log.info("事件驱动调度器开始执行DAG: {}", dagGraph.getDagId());

        long startTime = System.currentTimeMillis();

        // 初始化依赖跟踪器
        DagDependencyTracker tracker = new DagDependencyTracker(dagGraph);

        // 初始化进度跟踪
        int totalNodes = dagGraph.getNodes().size();
        context.initProgress(totalNodes);
        ProgressData progress = context.getProgressData();

        // 记录执行状态
        ConcurrentHashMap<String, NodeResult> nodeResults = new ConcurrentHashMap<>();
        AtomicInteger activeTaskCount = new AtomicInteger(0);
        AtomicInteger completedCount = new AtomicInteger(0);

        // 用于检测失败和暂停
        ConcurrentHashMap<String, Exception> failures = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, String> pausedNodes = new ConcurrentHashMap<>();

        // 同步对象：用于等待所有任务完成
        CountDownLatch allDoneLatch = new CountDownLatch(1);

        // 获取初始就绪节点
        Set<String> readyNodes = tracker.getReadyNodes();
        log.info("初始就绪节点: {}", readyNodes);

        // 提交初始节点
        for (String nodeId : readyNodes) {
            submitNode(nodeId, dagGraph, context, tracker, nodeResults, failures, pausedNodes,
                    activeTaskCount, completedCount, progress, allDoneLatch);
        }

        // 等待所有任务完成
        try {
            // 使用轮询检查是否所有任务都已完成
            while (true) {
                // 检查是否有失败
                if (!failures.isEmpty()) {
                    log.error("检测到节点执行失败，停止调度");
                    break;
                }

                // 检查是否有暂停
                if (!pausedNodes.isEmpty()) {
                    log.info("检测到节点暂停，停止调度");
                    break;
                }

                // 检查是否所有节点都完成（活跃任务为0且所有启用节点都已完成）
                if (activeTaskCount.get() == 0 && tracker.isAllCompleted()) {
                    log.info("所有节点执行完成");
                    allDoneLatch.countDown();
                    break;
                }

                // 短暂等待避免忙等待
                Thread.sleep(50);
            }

            // 等待倒计时或超时
            allDoneLatch.await(1, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("DAG执行被中断", e);
            return SchedulerExecutionResult.failed("DAG execution interrupted", e);
        }

        long totalDuration = System.currentTimeMillis() - startTime;

        // 检查执行结果
        if (!failures.isEmpty()) {
            var firstFailure = failures.entrySet().iterator().next();
            return SchedulerExecutionResult.failed(
                    "Node execution failed: " + firstFailure.getKey(),
                    firstFailure.getValue());
        }

        if (!pausedNodes.isEmpty()) {
            String pausedNodeId = pausedNodes.keySet().iterator().next();
            return SchedulerExecutionResult.paused(pausedNodeId);
        }

        log.info("DAG执行完成，总耗时: {}ms, 完成节点数: {}/{}",
                totalDuration, completedCount.get(), totalNodes);

        return SchedulerExecutionResult.success(totalDuration);
    }

    /**
     * 提交节点执行
     */
    private void submitNode(
            String nodeId,
            DagGraph dagGraph,
            DagExecutionContext context,
            DagDependencyTracker tracker,
            ConcurrentHashMap<String, NodeResult> nodeResults,
            ConcurrentHashMap<String, Exception> failures,
            ConcurrentHashMap<String, String> pausedNodes,
            AtomicInteger activeTaskCount,
            AtomicInteger completedCount,
            ProgressData progress,
            CountDownLatch allDoneLatch) {

        AbstractConfigurableNode node = dagGraph.getNode(nodeId);
        if (node == null) {
            log.error("节点不存在: {}", nodeId);
            return;
        }

        activeTaskCount.incrementAndGet();

        CompletableFuture.runAsync(() -> {
            try {
                // 执行节点
                NodeResult result = executeNode(node, context, progress, completedCount);
                nodeResults.put(nodeId, result);

                // 检查是否需要暂停
                if (result.isPaused()) {
                    pausedNodes.put(nodeId, result.getResultContent());
                    return;
                }

                // 处理路由节点
                if (node.isRouterNode()) {
                    handleRouterNode(node, context, dagGraph, tracker);
                }

                // 标记完成并获取新就绪节点
                Set<String> newReadyNodes = tracker.markCompleted(nodeId);

                // 递归提交新就绪节点
                for (String readyNodeId : newReadyNodes) {
                    submitNode(readyNodeId, dagGraph, context, tracker, nodeResults, failures, pausedNodes,
                            activeTaskCount, completedCount, progress, allDoneLatch);
                }

            } catch (Exception e) {
                log.error("节点执行异常: {}", nodeId, e);
                failures.put(nodeId, e);
            } finally {
                // 减少活跃任务计数
                int remaining = activeTaskCount.decrementAndGet();
                log.debug("节点 {} 完成，剩余活跃任务: {}", nodeId, remaining);
            }
        }, executor);
    }

    /**
     * 执行单个节点
     */
    private NodeResult executeNode(
            AbstractConfigurableNode node,
            DagExecutionContext context,
            ProgressData progress,
            AtomicInteger completedCount) {

        String nodeId = node.getNodeId();
        String nodeName = node.getNodeName();
        long startTime = System.currentTimeMillis();

        // 推送节点开始事件
        pushNodeLifecycleEvent(context, nodeId, nodeName, "starting", null, null,
                completedCount.get(), progress.getTotalNodes());

        try {
            log.info("开始执行节点: {}", nodeId);

            // 调用生命周期钩子
            node.beforeExecute(context);

            // 执行节点
            NodeExecutionResult result = node.execute(context);

            // 调用生命周期钩子
            node.afterExecute(context, result, null);

            // 将结果存入context
            context.setNodeResult(nodeId, result);

            long duration = System.currentTimeMillis() - startTime;
            log.info("节点执行成功: {}, 耗时: {}ms", nodeId, duration);

            // 更新进度
            progress.incrementCompleted();
            completedCount.incrementAndGet();

            // 检查是否暂停
            String resultContent = result != null ? result.getContent() : null;
            boolean isPaused = resultContent != null && resultContent.startsWith("WAITING_FOR_HUMAN");

            // 推送节点完成事件
            pushNodeLifecycleEvent(context, nodeId, nodeName, isPaused ? "paused" : "completed",
                    resultContent, duration, completedCount.get(), progress.getTotalNodes());

            return new NodeResult(nodeId, true, resultContent, null, duration, isPaused);

        } catch (DagNodeExecutionException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("节点执行失败: {}, 耗时: {}ms", nodeId, duration, e);

            node.afterExecute(context, null, e);

            // 更新进度
            progress.incrementCompleted();
            completedCount.incrementAndGet();

            // 推送节点失败事件
            pushNodeLifecycleEvent(context, nodeId, nodeName, "failed", e.getMessage(), duration,
                    completedCount.get(), progress.getTotalNodes());

            throw new RuntimeException(e);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("节点执行异常: {}, 耗时: {}ms", nodeId, duration, e);

            DagNodeExecutionException wrappedException = new DagNodeExecutionException(
                    "Node execution failed: " + e.getMessage(), e, nodeId, true);

            node.afterExecute(context, null, wrappedException);

            // 更新进度
            progress.incrementCompleted();
            completedCount.incrementAndGet();

            // 推送节点失败事件
            pushNodeLifecycleEvent(context, nodeId, nodeName, "failed", e.getMessage(), duration,
                    completedCount.get(), progress.getTotalNodes());

            throw new RuntimeException(wrappedException);
        }
    }

    /**
     * 处理路由节点
     */
    private void handleRouterNode(AbstractConfigurableNode routerNode,
            DagExecutionContext context,
            DagGraph dagGraph,
            DagDependencyTracker tracker) {

        String nodeId = routerNode.getNodeId();
        log.info("处理路由节点: {}", nodeId);

        // 从 context 获取路由决策结果
        Object routeDecisionObj = context.getNodeResult(nodeId);
        if (routeDecisionObj == null) {
            log.warn("路由节点 {} 没有决策结果", nodeId);
            return;
        }

        // 解析路由决策
        String selectedNodeId = null;
        if (routeDecisionObj instanceof String) {
            selectedNodeId = (String) routeDecisionObj;
        } else if (routeDecisionObj instanceof NodeExecutionResult execResult) {
            if (execResult.isRoutingDecision()) {
                NodeRouteDecision decision = execResult.getRouteDecision();
                if (decision.isStopExecution()) {
                    log.info("路由决策: 停止执行");
                    return;
                }
                Set<String> nextNodeIds = decision.getNextNodeIds();
                if (nextNodeIds != null && !nextNodeIds.isEmpty()) {
                    selectedNodeId = nextNodeIds.iterator().next();
                }
            }
        } else if (routeDecisionObj instanceof NodeRouteDecision decision) {
            if (decision.isStopExecution()) {
                log.info("路由决策: 停止执行");
                return;
            }
            Set<String> nextNodeIds = decision.getNextNodeIds();
            if (nextNodeIds != null && !nextNodeIds.isEmpty()) {
                selectedNodeId = nextNodeIds.iterator().next();
            }
        }

        if (selectedNodeId == null || selectedNodeId.isEmpty()) {
            log.warn("无法从路由决策中获取选中的节点ID");
            return;
        }

        log.info("路由决策: 选择节点 {}", selectedNodeId);

        // 获取所有候选节点
        Set<String> candidateNodes = routerNode.getCandidateNextNodes();
        if (candidateNodes == null || candidateNodes.isEmpty()) {
            log.warn("路由节点 {} 没有候选节点", nodeId);
            return;
        }

        // 禁用未被选中的候选节点及其下游
        for (String candidateNodeId : candidateNodes) {
            if (!candidateNodeId.equals(selectedNodeId)) {
                tracker.disableNode(candidateNodeId);
                log.info("禁用未选中的节点: {}", candidateNodeId);
            }
        }
    }

    /**
     * 推送节点生命周期事件
     */
    private void pushNodeLifecycleEvent(DagExecutionContext context, String nodeId, String nodeName,
            String status, Object result, Long durationMs, Integer completedNodes, Integer totalNodes) {

        var emitter = context.getEmitter();
        if (emitter == null) {
            return;
        }

        try {
            java.util.Map<String, Object> event = new java.util.HashMap<>();
            event.put("type", "node_lifecycle");
            event.put("nodeId", nodeId);
            event.put("nodeName", nodeName);
            event.put("status", status);
            event.put("timestamp", System.currentTimeMillis());
            event.put("conversationId", context.getConversationId());

            if (result != null) {
                String resultStr = result.toString();
                if (resultStr.length() > 500) {
                    resultStr = resultStr.substring(0, 500) + "...";
                }
                event.put("result", resultStr);
            }

            if (durationMs != null) {
                event.put("durationMs", durationMs);
            }

            if (completedNodes != null && totalNodes != null && totalNodes > 0) {
                java.util.Map<String, Object> progress = new java.util.HashMap<>();
                progress.put("current", completedNodes);
                progress.put("total", totalNodes);
                progress.put("percentage", (int) ((completedNodes * 100.0) / totalNodes));
                event.put("progress", progress);
            }

            String message = "data: " + com.alibaba.fastjson.JSON.toJSONString(event) + "\n\n";
            emitter.send(message);

            log.debug("推送节点生命周期事件: nodeId={}, status={}", nodeId, status);
        } catch (Exception e) {
            log.warn("推送节点生命周期事件失败: nodeId={}, status={}", nodeId, status, e);
        }
    }

    /**
     * 关闭调度器
     */
    public void shutdown() {
        log.info("关闭事件驱动调度器");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 节点执行结果（内部使用）
     */
    @Data
    @AllArgsConstructor
    private static class NodeResult {
        private String nodeId;
        private boolean success;
        private String resultContent;
        private Exception exception;
        private long durationMs;
        private boolean paused;
    }

    /**
     * 调度器执行结果
     */
    @Data
    @AllArgsConstructor
    public static class SchedulerExecutionResult {
        private String status; // SUCCESS, FAILED, PAUSED
        private String message;
        private String pausedNodeId;
        private Exception exception;
        private long durationMs;

        public static SchedulerExecutionResult success(long durationMs) {
            return new SchedulerExecutionResult("SUCCESS", "All nodes completed", null, null, durationMs);
        }

        public static SchedulerExecutionResult failed(String message, Exception exception) {
            return new SchedulerExecutionResult("FAILED", message, null, exception, 0);
        }

        public static SchedulerExecutionResult paused(String pausedNodeId) {
            return new SchedulerExecutionResult("PAUSED", "Waiting for human intervention", pausedNodeId, null, 0);
        }
    }
}
