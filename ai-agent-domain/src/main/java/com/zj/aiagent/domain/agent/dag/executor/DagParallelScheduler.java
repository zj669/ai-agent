package com.zj.aiagent.domain.agent.dag.executor;

import com.zj.aiagent.domain.agent.dag.context.DagExecutionContext;
import com.zj.aiagent.domain.agent.dag.context.ProgressData;
import com.zj.aiagent.domain.agent.dag.node.AbstractConfigurableNode;
import com.zj.aiagent.shared.design.dag.DagNodeExecutionException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * DAG并行调度器
 * 负责并行执行可以同时运行的节点
 */
@Slf4j
public class DagParallelScheduler {

    private final ExecutorService executorService;
    private final int maxParallelism;

    public DagParallelScheduler(int maxParallelism) {
        this.maxParallelism = maxParallelism;
        this.executorService = new ThreadPoolExecutor(
                maxParallelism,
                maxParallelism,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadFactory() {
                    private int counter = 0;

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "DAG-Executor-" + (counter++));
                    }
                });
    }

    /**
     * 并行执行一组节点
     * 统一使用 AbstractConfigurableNode 类型
     * 
     * @param nodes          要执行的节点列表
     * @param context        执行上下文
     * @param completedNodes 已完成节点数
     * @param totalNodes     总节点数
     */
    public List<NodeExecutionResult> executeParallel(
            List<AbstractConfigurableNode> nodes,
            DagExecutionContext context,
            int completedNodes,
            int totalNodes) {

        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }

        log.info("并行执行 {} 个节点", nodes.size());

        // 初始化进度数据领域对象
        context.initProgress(totalNodes);
        ProgressData progress = context.getProgressData();
        progress.setCompletedNodes(completedNodes);

        List<CompletableFuture<NodeExecutionResult>> futures = new ArrayList<>();

        for (AbstractConfigurableNode node : nodes) {
            CompletableFuture<NodeExecutionResult> future = CompletableFuture.supplyAsync(() -> {
                return executeNode(node, context);
            }, executorService);

            futures.add(future);
        }

        // 等待所有节点执行完成
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("并行执行被中断", e);
        }

        // 收集结果
        List<NodeExecutionResult> results = new ArrayList<>();
        for (CompletableFuture<NodeExecutionResult> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                log.error("获取节点执行结果失败", e);
            }
        }

        return results;
    }

    /**
     * 执行单个节点
     * 统一使用 AbstractConfigurableNode 类型
     */
    private NodeExecutionResult executeNode(AbstractConfigurableNode node,
            DagExecutionContext context) {
        String nodeId = node.getNodeId();
        String nodeName = node.getNodeName();

        long startTime = System.currentTimeMillis();

        // 从领域对象获取进度信息
        ProgressData progress = context.getProgressData();
        Integer completedNodes = progress != null ? progress.getCompletedNodes() : null;
        Integer totalNodes = progress != null ? progress.getTotalNodes() : null;

        // 推送节点开始事件
        pushNodeLifecycleEvent(context, nodeId, nodeName, "starting", null, null, completedNodes, totalNodes);

        try {
            log.info("开始执行节点: {}", nodeId);

            // 统一调用 beforeExecute
            node.beforeExecute(context);

            // 执行节点
            com.zj.aiagent.shared.design.dag.NodeExecutionResult result = node.execute(context);

            // 调用 afterExecute
            node.afterExecute(context, result, null);

            // 将结果存入context
            context.setNodeResult(nodeId, result);

            long duration = System.currentTimeMillis() - startTime;
            log.info("节点执行成功: {}, 耗时: {}ms", nodeId, duration);

            // 更新进度领域对象
            if (progress != null) {
                progress.incrementCompleted();
            }

            // 推送节点完成事件
            String resultContent = result != null ? result.getContent() : null;
            int currentCompleted = progress != null ? progress.getCompletedNodes() : 0;
            int currentTotal = progress != null ? progress.getTotalNodes() : 0;
            pushNodeLifecycleEvent(context, nodeId, nodeName, "completed", resultContent, duration, currentCompleted,
                    currentTotal);

            return new NodeExecutionResult(nodeId, true, resultContent, null, duration);

        } catch (DagNodeExecutionException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("节点执行失败: {}, 耗时: {}ms", nodeId, duration, e);

            // 调用 afterExecute 记录失败
            node.afterExecute(context, null, e);

            // 更新进度并推送节点失败事件
            if (progress != null) {
                progress.incrementCompleted();
            }
            int currentCompleted = progress != null ? progress.getCompletedNodes() : 0;
            int currentTotal = progress != null ? progress.getTotalNodes() : 0;
            pushNodeLifecycleEvent(context, nodeId, nodeName, "failed", e.getMessage(), duration, currentCompleted,
                    currentTotal);

            return new NodeExecutionResult(nodeId, false, null, e, duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("节点执行异常: {}, 耗时: {}ms", nodeId, duration, e);

            DagNodeExecutionException wrappedException = new DagNodeExecutionException(
                    "Node execution failed: " + e.getMessage(), e, nodeId, true);

            // 调用 afterExecute 记录失败
            node.afterExecute(context, null, wrappedException);

            // 更新进度并推送节点失败事件
            if (progress != null) {
                progress.incrementCompleted();
            }
            int currentCompleted = progress != null ? progress.getCompletedNodes() : 0;
            int currentTotal = progress != null ? progress.getTotalNodes() : 0;
            pushNodeLifecycleEvent(context, nodeId, nodeName, "failed", e.getMessage(), duration, currentCompleted,
                    currentTotal);

            return new NodeExecutionResult(nodeId, false, null, wrappedException, duration);
        }
    }

    /**
     * 推送节点生命周期事件到客户端
     * 
     * @param context        执行上下文
     * @param nodeId         节点ID
     * @param nodeName       节点名称
     * @param status         节点状态: starting, completed, failed
     * @param result         结果或错误信息
     * @param durationMs     执行耗时(毫秒),starting 事件时为 null
     * @param completedNodes 已完成节点数(可选)
     * @param totalNodes     总节点数(可选)
     */
    private void pushNodeLifecycleEvent(DagExecutionContext context, String nodeId, String nodeName,
            String status, Object result, Long durationMs, Integer completedNodes, Integer totalNodes) {
        org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter emitter = context.getEmitter();
        if (emitter == null) {
            log.warn("Emitter is null, cannot push lifecycle event for node: {}", nodeId);
            return;
        }

        try {
            // 构建事件消息
            java.util.Map<String, Object> event = new java.util.HashMap<>();
            event.put("type", "node_lifecycle");
            event.put("nodeId", nodeId);
            event.put("nodeName", nodeName);
            event.put("status", status);
            event.put("timestamp", System.currentTimeMillis());
            event.put("conversationId", context.getConversationId());

            if (result != null) {
                // 限制结果长度避免消息过大
                String resultStr = result.toString();
                if (resultStr.length() > 500) {
                    resultStr = resultStr.substring(0, 500) + "...";
                }
                event.put("result", resultStr);
            }

            if (durationMs != null) {
                event.put("durationMs", durationMs);
            }

            // 添加进度信息
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
        log.info("关闭DAG并行调度器");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 节点执行结果
     */
    @Data
    @AllArgsConstructor
    public static class NodeExecutionResult {
        private String nodeId;
        private boolean success;
        private String result;
        private Exception exception;
        private long durationMs;
    }
}
