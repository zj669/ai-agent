package com.zj.aiagemt.service.dag.executor;

import com.zj.aiagemt.common.design.dag.DagNode;
import com.zj.aiagemt.common.design.dag.DagNodeExecutionException;
import com.zj.aiagemt.service.dag.context.DagExecutionContext;
import com.zj.aiagemt.service.dag.logging.DagLoggingService;
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
    private final DagLoggingService loggingService;
    private final int maxParallelism;

    public DagParallelScheduler(int maxParallelism, DagLoggingService loggingService) {
        this.maxParallelism = maxParallelism;
        this.loggingService = loggingService;
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
     */
    public List<NodeExecutionResult> executeParallel(
            List<DagNode<DagExecutionContext, String>> nodes,
            DagExecutionContext context) {

        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }

        log.info("并行执行 {} 个节点", nodes.size());

        List<CompletableFuture<NodeExecutionResult>> futures = new ArrayList<>();

        for (DagNode<DagExecutionContext, String> node : nodes) {
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
     */
    private NodeExecutionResult executeNode(DagNode<DagExecutionContext, String> node,
            DagExecutionContext context) {
        String nodeId = node.getNodeId();
        long startTime = System.currentTimeMillis();

        try {
            log.info("开始执行节点: {}", nodeId);

            // 执行前回调
            node.beforeExecute(context);

            // 执行节点
            String result = node.execute(context);

            // 存储结果
            context.setNodeResult(nodeId, result);

            // 执行后回调
            node.afterExecute(context, result, null);

            long duration = System.currentTimeMillis() - startTime;
            log.info("节点执行成功: {}, 耗时: {}ms", nodeId, duration);

            return new NodeExecutionResult(nodeId, true, result, null, duration);

        } catch (DagNodeExecutionException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("节点执行失败: {}, 耗时: {}ms", nodeId, duration, e);

            // 执行后回调（传递异常）
            node.afterExecute(context, null, e);

            return new NodeExecutionResult(nodeId, false, null, e, duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("节点执行异常: {}, 耗时: {}ms", nodeId, duration, e);

            DagNodeExecutionException wrappedException = new DagNodeExecutionException(
                    "Node execution failed: " + e.getMessage(), e, nodeId, true);

            node.afterExecute(context, null, wrappedException);

            return new NodeExecutionResult(nodeId, false, null, wrappedException, duration);
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
