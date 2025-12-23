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
        // 在获取就绪节点前，先处理已执行的节点（恢复现场）
        int restoredCount = 0;
        for (AbstractConfigurableNode node : dagGraph.getNodes().values()) {
            if (context.isNodeExecuted(node.getNodeId())) {
                String nodeId = node.getNodeId();
                // 标记为完成，这将更新下游节点的依赖计数
                // 检查结果类型，如果为 HUMAN_WAIT（暂停点），则视为未完成，需要重新执行
                Object executionResult = context.getNodeResult(nodeId);
                boolean isHumanWait = false;

                // 处理 JSONObject
                if (executionResult instanceof com.alibaba.fastjson.JSONObject) {
                    try {
                        NodeExecutionResult res = ((com.alibaba.fastjson.JSONObject) executionResult)
                                .toJavaObject(NodeExecutionResult.class);
                        if (res.isHumanWait()) {
                            isHumanWait = true;
                        } else {
                            // 更新为 Java 对象，方便后续使用
                            context.setNodeResult(nodeId, res);
                        }
                    } catch (Exception e) {
                        log.warn("反序列化节点结果失败: {}", nodeId, e);
                    }
                } else if (executionResult instanceof NodeExecutionResult) {
                    if (((NodeExecutionResult) executionResult).isHumanWait()) {
                        isHumanWait = true;
                    }
                }

                if (isHumanWait) {
                    log.info("节点 {} 为暂停状态 (HUMAN_WAIT)，跳过恢复，将重新执行", nodeId);
                    // 从 done 列表中移除，这样它会被 treated as incomplete
                    // 并且不会触发 markCompleted，所以下游依赖不会被满足
                    // 但是需要确保它能进入 readyQueue 吗？
                    // 如果它是起始节点，或者它的依赖已经满足（前序已完成），它需要进入 readyQueue
                    // 如果我们不在这里做任何事，下面 tracker.getReadyNodes() 会包含它吗？
                    // tracker 初始化时计算了所有入度。
                    // 前序节点在这里循环中被 markCompleted，会减少它的入度。
                    // 如果前序都完成了，它就会变成 ready。
                    // 如果没有前序（入度0），它初始就是 ready。
                    // 所以：只要 continue 即可！
                    continue;
                }

                log.info("恢复已执行节点状态: {}", nodeId);

                // 如果是路由节点，需要重新应用路由决策（禁用未选中的路径）
                if (node.isRouterNode()) {
                    handleRouterNode(node, context, dagGraph, tracker);
                }

                // 标记为完成，这将更新下游节点的依赖计数
                tracker.markCompleted(nodeId);

                // 更新进度
                progress.incrementCompleted();
                completedCount.incrementAndGet();
                restoredCount++;
            }
        }

        if (restoredCount > 0) {
            log.info("已恢复 {} 个节点的执行状态", restoredCount);
        }

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

                // 检查并处理循环边
                Set<String> loopTargets = tracker.getLoopBackTargets(nodeId);
                if (!loopTargets.isEmpty()) {
                    for (String targetNodeId : loopTargets) {
                        // 检查是否应该触发循环
                        if (shouldTriggerLoop(context, targetNodeId)) {
                            handleLoopBack(targetNodeId, dagGraph, context, tracker,
                                    nodeResults, failures, pausedNodes, activeTaskCount,
                                    completedCount, progress, allDoneLatch);
                        } else {
                            log.info("节点 {} 循环终止：达到最大循环次数或其他条件", targetNodeId);
                        }
                    }
                }

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

        // 调试日志：输出实际对象类型
        log.info("路由决策对象类型: {}, 内容: {}",
                routeDecisionObj.getClass().getName(),
                routeDecisionObj);

        // 如果是 JSONObject，尝试转换为 NodeExecutionResult
        if (routeDecisionObj instanceof com.alibaba.fastjson.JSONObject) {
            try {
                routeDecisionObj = ((com.alibaba.fastjson.JSONObject) routeDecisionObj)
                        .toJavaObject(NodeExecutionResult.class);
                log.info("JSONObject 已转换为 NodeExecutionResult");
            } catch (Exception e) {
                log.warn("无法将 JSONObject 转换为 NodeExecutionResult", e);
            }
        }

        // 解析路由决策
        String selectedNodeId = null;
        if (routeDecisionObj instanceof String) {
            selectedNodeId = (String) routeDecisionObj;
            log.info("解析为字符串: {}", selectedNodeId);
        } else if (routeDecisionObj instanceof NodeExecutionResult execResult) {
            log.info("解析为 NodeExecutionResult, isRoutingDecision: {}", execResult.isRoutingDecision());
            if (execResult.isRoutingDecision()) {
                NodeRouteDecision decision = execResult.getRouteDecision();
                log.info("获取到 RouteDecision: {}", decision);
                if (decision.isStopExecution()) {
                    log.info("路由决策: 停止执行");
                    return;
                }
                Set<String> nextNodeIds = decision.getNextNodeIds();
                log.info("RouteDecision.getNextNodeIds: {}", nextNodeIds);
                if (nextNodeIds != null && !nextNodeIds.isEmpty()) {
                    selectedNodeId = nextNodeIds.iterator().next();
                }
            }
        } else if (routeDecisionObj instanceof NodeRouteDecision decision) {
            log.info("解析为 NodeRouteDecision");
            if (decision.isStopExecution()) {
                log.info("路由决策: 停止执行");
                return;
            }
            Set<String> nextNodeIds = decision.getNextNodeIds();
            if (nextNodeIds != null && !nextNodeIds.isEmpty()) {
                selectedNodeId = nextNodeIds.iterator().next();
            }
        } else {
            log.warn("未知的路由决策对象类型: {}", routeDecisionObj.getClass().getName());
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

    // ==================== 循环边支持方法 ====================

    /**
     * 处理循环边触发
     * 重置目标节点状态并重新提交执行
     */
    private void handleLoopBack(
            String targetNodeId,
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

        // 增加节点执行计数
        int count = context.incrementNodeExecutionCount(targetNodeId);
        log.info("触发循环执行: {} (第 {} 次)", targetNodeId, count);

        // 重置目标节点状态
        tracker.resetNode(targetNodeId);

        // 清除之前的执行结果（可选，保留历史可用于调试）
        // nodeResults.remove(targetNodeId);

        // 重新提交节点执行
        submitNode(targetNodeId, dagGraph, context, tracker,
                nodeResults, failures, pausedNodes,
                activeTaskCount, completedCount, progress, allDoneLatch);
    }

    /**
     * 判断是否应该触发循环
     * 检查节点执行次数是否未达上限
     */
    private boolean shouldTriggerLoop(DagExecutionContext context, String nodeId) {
        return context.canExecuteNode(nodeId);
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
