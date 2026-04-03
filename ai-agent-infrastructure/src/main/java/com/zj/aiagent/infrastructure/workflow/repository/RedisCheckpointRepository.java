package com.zj.aiagent.infrastructure.workflow.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.workflow.port.CheckpointRepository;
import com.zj.aiagent.domain.workflow.valobj.Checkpoint;
import com.zj.aiagent.infrastructure.redis.IRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis 检查点仓储实现
 * 使用 ZSET（有序集合）维护时间戳索引，解决键名字典序排序的不稳定性问题
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisCheckpointRepository implements CheckpointRepository {

    private static final String KEY_PREFIX = "workflow:checkpoint:";
    private static final String INDEX_PREFIX = "workflow:checkpoint:index:";
    private static final String PAUSE_KEY_PREFIX = "workflow:pause:";
    private static final long TTL_HOURS = 24;

    private final IRedisService redisService;
    private final ObjectMapper objectMapper;

    @Override
    public void save(Checkpoint checkpoint) {
        try {
            String key = KEY_PREFIX + checkpoint.getExecutionId() + ":" + checkpoint.getCurrentNodeId();
            String value = objectMapper.writeValueAsString(checkpoint);

            redisService.setString(key, value, TTL_HOURS, TimeUnit.HOURS);

            // ZSET: 索引键 -> 时间戳分数，便于按时间排序获取最新检查点
            String indexKey = INDEX_PREFIX + checkpoint.getExecutionId();
            double timestamp = parseCheckpointTimestamp(checkpoint.getCheckpointId());
            redisService.addToScoredSortedSet(indexKey, key, timestamp);

            if (checkpoint.isPausePoint()) {
                String pauseKey = PAUSE_KEY_PREFIX + checkpoint.getExecutionId();
                redisService.setString(pauseKey, value, TTL_HOURS, TimeUnit.HOURS);
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
            String indexKey = INDEX_PREFIX + executionId;
            String latestKey = redisService.getHighestScored(indexKey);

            if (latestKey == null) {
                return Optional.empty();
            }

            String value = redisService.getString(latestKey);
            if (value == null) {
                return Optional.empty();
            }

            return Optional.of(objectMapper.readValue(value, Checkpoint.class));
        } catch (Exception e) {
            log.error("[Checkpoint] Failed to find latest: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to find checkpoint", e);
        }
    }

    @Override
    public Optional<Checkpoint> findPausePoint(String executionId) {
        try {
            String key = PAUSE_KEY_PREFIX + executionId;
            String value = redisService.getString(key);

            if (value == null) {
                return Optional.empty();
            }

            return Optional.of(objectMapper.readValue(value, Checkpoint.class));
        } catch (Exception e) {
            log.error("[Checkpoint] Failed to find pause point: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to find pause point", e);
        }
    }

    @Override
    public void deleteByExecutionId(String executionId) {
        try {
            String indexKey = INDEX_PREFIX + executionId;
            redisService.delete(indexKey);
            redisService.delete(PAUSE_KEY_PREFIX + executionId);

            log.debug("[Checkpoint] Deleted all for execution: {}", executionId);
        } catch (Exception e) {
            log.error("[Checkpoint] Failed to delete: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete checkpoint", e);
        }
    }

    /**
     * 从 checkpointId 解析时间戳（格式: {executionId}_{nodeId}_{timestamp}）
     */
    private double parseCheckpointTimestamp(String checkpointId) {
        if (checkpointId == null) {
            return System.currentTimeMillis();
        }
        int lastUnderscore = checkpointId.lastIndexOf('_');
        if (lastUnderscore < 0) {
            return System.currentTimeMillis();
        }
        try {
            return Double.parseDouble(checkpointId.substring(lastUnderscore + 1));
        } catch (NumberFormatException e) {
            return System.currentTimeMillis();
        }
    }
}
