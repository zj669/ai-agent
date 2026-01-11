package com.zj.aiagent.infrastructure.workflow.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.workflow.port.CheckpointRepository;
import com.zj.aiagent.domain.workflow.valobj.Checkpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis 检查点仓储实现
 * 用于持久化执行状态快照
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisCheckpointRepository implements CheckpointRepository {

    private static final String KEY_PREFIX = "workflow:checkpoint:";
    private static final String PAUSE_KEY_PREFIX = "workflow:pause:";
    private static final long TTL_HOURS = 24; // 检查点保留 24 小时

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void save(Checkpoint checkpoint) {
        try {
            String key = KEY_PREFIX + checkpoint.getExecutionId() + ":" + checkpoint.getCurrentNodeId();
            String value = objectMapper.writeValueAsString(checkpoint);
            redisTemplate.opsForValue().set(key, value, TTL_HOURS, TimeUnit.HOURS);

            // 如果是暂停点，额外保存一份便于快速查询
            if (checkpoint.isPausePoint()) {
                String pauseKey = PAUSE_KEY_PREFIX + checkpoint.getExecutionId();
                redisTemplate.opsForValue().set(pauseKey, value, TTL_HOURS, TimeUnit.HOURS);
            }

            log.debug("[Checkpoint] Saved: {}", checkpoint.getCheckpointId());
        } catch (Exception e) {
            log.error("[Checkpoint] Failed to save: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save checkpoint", e);
        }
    }

    @Override
    public Optional<Checkpoint> findLatest(String executionId) {
        try {
            // 简化实现：使用 SCAN 查找最新的检查点
            // 生产环境建议使用 Sorted Set 按时间排序
            String pattern = KEY_PREFIX + executionId + ":*";
            var keys = redisTemplate.keys(pattern);

            if (keys == null || keys.isEmpty()) {
                return Optional.empty();
            }

            // 取最后一个（按创建时间）
            String latestKey = keys.stream()
                    .max(String::compareTo)
                    .orElse(null);

            if (latestKey == null) {
                return Optional.empty();
            }

            String value = redisTemplate.opsForValue().get(latestKey);
            if (value == null) {
                return Optional.empty();
            }

            return Optional.of(objectMapper.readValue(value, Checkpoint.class));
        } catch (Exception e) {
            log.error("[Checkpoint] Failed to find latest: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Checkpoint> findPausePoint(String executionId) {
        try {
            String key = PAUSE_KEY_PREFIX + executionId;
            String value = redisTemplate.opsForValue().get(key);

            if (value == null) {
                return Optional.empty();
            }

            return Optional.of(objectMapper.readValue(value, Checkpoint.class));
        } catch (Exception e) {
            log.error("[Checkpoint] Failed to find pause point: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public void deleteByExecutionId(String executionId) {
        try {
            // 删除所有检查点
            String pattern = KEY_PREFIX + executionId + ":*";
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }

            // 删除暂停点
            redisTemplate.delete(PAUSE_KEY_PREFIX + executionId);

            log.debug("[Checkpoint] Deleted all for execution: {}", executionId);
        } catch (Exception e) {
            log.error("[Checkpoint] Failed to delete: {}", e.getMessage(), e);
        }
    }
}
