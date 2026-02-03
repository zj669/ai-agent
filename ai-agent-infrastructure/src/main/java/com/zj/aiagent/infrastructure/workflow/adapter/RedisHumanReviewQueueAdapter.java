package com.zj.aiagent.infrastructure.workflow.adapter;

import com.zj.aiagent.domain.workflow.port.HumanReviewQueuePort;
import com.zj.aiagent.infrastructure.redis.IRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * 基于 Redis 的人工审核队列适配器
 * 
 * 实现 Domain 层定义的 HumanReviewQueuePort 接口
 * 使用 IRedisService 进行技术实现
 * 
 * 技术细节：
 * - 使用 Redis Set 类型存储待审核的执行ID
 * - Key: human_review:pending
 * - 无过期时间（持久化存储，直到审核完成）
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisHumanReviewQueueAdapter implements HumanReviewQueuePort {
    
    private final IRedisService redisService;
    
    private static final String PENDING_QUEUE_KEY = "human_review:pending";
    
    @Override
    public void addToPendingQueue(String executionId) {
        try {
            redisService.addToSet(PENDING_QUEUE_KEY, executionId);
            log.info("[HumanReviewQueue] Added to pending queue: {}", executionId);
        } catch (Exception e) {
            log.error("[HumanReviewQueue] Failed to add to pending queue: {}", executionId, e);
            throw new RuntimeException("Failed to add to human review queue", e);
        }
    }
    
    @Override
    public void removeFromPendingQueue(String executionId) {
        try {
            redisService.removeFromSet(PENDING_QUEUE_KEY, executionId);
            log.info("[HumanReviewQueue] Removed from pending queue: {}", executionId);
        } catch (Exception e) {
            log.error("[HumanReviewQueue] Failed to remove from pending queue: {}", executionId, e);
            throw new RuntimeException("Failed to remove from human review queue", e);
        }
    }
    
    @Override
    public boolean isInPendingQueue(String executionId) {
        try {
            return redisService.isSetMember(PENDING_QUEUE_KEY, executionId);
        } catch (Exception e) {
            log.error("[HumanReviewQueue] Failed to check pending queue status: {}", executionId, e);
            // 发生异常时返回 false
            return false;
        }
    }
}
