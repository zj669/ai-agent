package com.zj.aiagent.domain.agent.dag.resilience;

import com.zj.aiagent.domain.agent.dag.config.ResilienceConfig;
import com.zj.aiagent.shared.design.dag.DagNodeExecutionException;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * 重试策略 - 提供带超时和重试的执行能力
 */
@Slf4j
public class RetryStrategy {

    /**
     * 带重试的执行（使用 Callable 支持 checked exception）
     */
    public <T> T executeWithRetry(Callable<T> action, ResilienceConfig config, String nodeId)
            throws DagNodeExecutionException {

        int maxRetries = config.getMaxRetries() != null ? config.getMaxRetries() : 3;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                return action.call();
            } catch (DagNodeExecutionException e) {
                // 直接重新抛出节点执行异常
                lastException = e;
                if (attempt > maxRetries || !isRetryable(e)) {
                    log.error("节点 {} 执行失败，不可重试或已达最大重试次数: {}", nodeId, e.getMessage());
                    throw e;
                }
            } catch (Exception e) {
                lastException = e;
                if (attempt > maxRetries || !isRetryable(e)) {
                    log.error("节点 {} 执行失败，不可重试或已达最大重试次数: {}", nodeId, e.getMessage());
                    break;
                }
            }

            long delay = config.calculateRetryDelay(attempt);
            log.warn("节点 {} 执行失败，将在 {}ms 后进行第 {} 次重试: {}",
                    nodeId, delay, attempt, lastException != null ? lastException.getMessage() : "unknown");

            try {
                Thread.sleep(delay);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new DagNodeExecutionException("重试被中断", ie, nodeId, true);
            }
        }

        throw new DagNodeExecutionException(
                "执行失败，已重试 " + maxRetries + " 次: " +
                        (lastException != null ? lastException.getMessage() : "未知错误"),
                lastException, nodeId, true);
    }

    /**
     * 带超时的执行（使用 Callable 支持 checked exception）
     */
    public <T> T executeWithTimeout(Callable<T> action, long timeoutMs, String nodeId)
            throws DagNodeExecutionException {

        if (timeoutMs <= 0) {
            try {
                return action.call();
            } catch (DagNodeExecutionException e) {
                throw e;
            } catch (Exception e) {
                throw new DagNodeExecutionException("节点执行失败: " + e.getMessage(), e, nodeId, true);
            }
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<T> future = executor.submit(action);

        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new DagNodeExecutionException("节点执行超时（" + timeoutMs + "ms）", e, nodeId, true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            throw new DagNodeExecutionException("节点执行被中断", e, nodeId, true);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof DagNodeExecutionException) {
                throw (DagNodeExecutionException) cause;
            }
            throw new DagNodeExecutionException("节点执行失败: " + cause.getMessage(),
                    cause instanceof Exception ? (Exception) cause : e, nodeId, true);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 带超时和重试的执行
     */
    public <T> T executeWithTimeoutAndRetry(Callable<T> action, ResilienceConfig config, String nodeId)
            throws DagNodeExecutionException {

        long timeoutMs = config.getTimeoutMs() != null ? config.getTimeoutMs() : 60000L;

        // 在重试循环中执行带超时的操作
        return executeWithRetry(() -> executeWithTimeout(action, timeoutMs, nodeId), config, nodeId);
    }

    /**
     * 判断异常是否可重试
     */
    private boolean isRetryable(Exception e) {
        if (e == null || e.getMessage() == null) {
            return false;
        }

        String message = e.getMessage().toLowerCase();

        // 可重试的场景
        return message.contains("timeout")
                || message.contains("超时")
                || message.contains("rate limit")
                || message.contains("限流")
                || message.contains("429") // Too Many Requests
                || message.contains("503") // Service Unavailable
                || message.contains("502") // Bad Gateway
                || message.contains("connection reset")
                || message.contains("connection refused")
                || message.contains("temporarily unavailable");
    }
}
