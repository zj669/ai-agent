package com.zj.aiagent.infrastructure.workflow.adapter;

import com.zj.aiagent.domain.workflow.port.WorkflowCancellationPort;
import com.zj.aiagent.infrastructure.redis.IRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的工作流取消适配器
 * 
 * 实现 Domain 层定义的 WorkflowCancellationPort 接口
 * 使用 IRedisService 进行技术实现
 * 
 * 技术细节：
 * - 使用 Redis String 类型存储取消标记
 * - Key 格式: workflow:cancel:{executionId}
 * - 过期时间: 1 小时（避免 Redis 内存泄漏）
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisWorkflowCancellationAdapter implements WorkflowCancellationPort {
    
    private final IRedisService redisService;
    
    private static final String CANCEL_KEY_PREFIX = "workflow:cancel:";
    private static final long CANCEL_EXPIRY_HOURS = 1;
    
    @Override
    public void markAsCancelled(String executionId) {
        try {
            String key = CANCEL_KEY_PREFIX + executionId;
            redisService.setString(key, "true", CANCEL_EXPIRY_HOURS, TimeUnit.HOURS);
            log.info("[WorkflowCancellation] Marked as cancelled: {}", executionId);
        } catch (Exception e) {
            log.error("[WorkflowCancellation] Failed to mark as cancelled: {}", executionId, e);
            throw new RuntimeException("Failed to mark workflow as cancelled", e);
        }
    }
    
    @Override
    public boolean isCancelled(String executionId) {
        try {
            String key = CANCEL_KEY_PREFIX + executionId;
            return redisService.isExists(key);
        } catch (Exception e) {
            log.error("[WorkflowCancellation] Failed to check cancellation status: {}", executionId, e);
            // 发生异常时返回 false，避免阻塞工作流执行
            return false;
        }
    }
}
