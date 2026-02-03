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
 * 用于持久化执行状态快照
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisCheckpointRepository implements CheckpointRepository {

    private static final String KEY_PREFIX = "workflow:checkpoint:";
    private static final String PAUSE_KEY_PREFIX = "workflow:pause:";
    private static final long TTL_HOURS = 24; // 检查点保留 24 小时

    private final IRedisService redisService;
    private final ObjectMapper objectMapper;

    @Override
    public void save(Checkpoint checkpoint) {
        try {
            // 业务逻辑: 构建 key
            String key = KEY_PREFIX + checkpoint.getExecutionId() + ":" + checkpoint.getCurrentNodeId();
            
            // 业务逻辑: 序列化对象
            String value = objectMapper.writeValueAsString(checkpoint);
            
            // ✅ 使用 IRedisService 的基础操作
            redisService.setString(key, value, TTL_HOURS, TimeUnit.HOURS);

            // 业务逻辑: 如果是暂停点，额外保存一份便于快速查询
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
            // 业务逻辑: 构建查询模式
            String pattern = KEY_PREFIX + executionId + ":*";
            
            // ✅ 使用 IRedisService 的基础操作
            var keys = redisService.keys(pattern);

            if (keys == null || keys.isEmpty()) {
                return Optional.empty();
            }

            // 业务逻辑: 选择最新的 key (按创建时间)
            String latestKey = keys.stream()
                    .max(String::compareTo)
                    .orElse(null);

            if (latestKey == null) {
                return Optional.empty();
            }

            // ✅ 使用 IRedisService 的基础操作
            String value = redisService.getString(latestKey);
            
            if (value == null) {
                return Optional.empty();
            }

            // 业务逻辑: 反序列化对象
            return Optional.of(objectMapper.readValue(value, Checkpoint.class));
        } catch (Exception e) {
            log.error("[Checkpoint] Failed to find latest: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<Checkpoint> findPausePoint(String executionId) {
        try {
            // 业务逻辑: 构建暂停点 key
            String key = PAUSE_KEY_PREFIX + executionId;
            
            // ✅ 使用 IRedisService 的基础操作
            String value = redisService.getString(key);

            if (value == null) {
                return Optional.empty();
            }

            // 业务逻辑: 反序列化对象
            return Optional.of(objectMapper.readValue(value, Checkpoint.class));
        } catch (Exception e) {
            log.error("[Checkpoint] Failed to find pause point: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public void deleteByExecutionId(String executionId) {
        try {
            // 业务逻辑: 构建删除模式
            String pattern = KEY_PREFIX + executionId + ":*";
            
            // ✅ 使用 IRedisService 的基础操作
            var keys = redisService.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisService.delete(keys);
            }

            // 业务逻辑: 删除暂停点
            redisService.delete(PAUSE_KEY_PREFIX + executionId);

            log.debug("[Checkpoint] Deleted all for execution: {}", executionId);
        } catch (Exception e) {
            log.error("[Checkpoint] Failed to delete: {}", e.getMessage(), e);
        }
    }
}
