package com.zj.aiagent.infrastructure.workflow.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.workflow.entity.Execution;
import com.zj.aiagent.domain.workflow.port.ExecutionRepository;
import com.zj.aiagent.infrastructure.redis.IRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Redis 执行仓储实现
 * 用于持久化执行聚合根（热数据）
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisExecutionRepository implements ExecutionRepository {

    private static final String KEY_PREFIX = "workflow:execution:";
    private static final String VERSION_KEY_SUFFIX = ":_v";
    private static final long TTL_HOURS = 48;

    private final IRedisService redisService;
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;

    @Override
    public void save(Execution execution) {
        try {
            String key = KEY_PREFIX + execution.getExecutionId();
            String value = objectMapper.writeValueAsString(execution);
            redisService.setString(key, value, TTL_HOURS, TimeUnit.HOURS);

            // Initialize version counter
            redissonClient.getAtomicLong(KEY_PREFIX + execution.getExecutionId() + VERSION_KEY_SUFFIX)
                    .set(execution.getVersion());

            String indexKey = "workflow:conversation:" + execution.getConversationId() + ":executions";
            redisService.addToSet(indexKey, execution.getExecutionId());
            redisService.expire(indexKey, TTL_HOURS * 3600);

            log.debug("[Execution] Saved: {}", execution.getExecutionId());
        } catch (Exception e) {
            log.error("[Execution] Failed to save: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save execution", e);
        }
    }

    @Override
    public Optional<Execution> findById(String executionId) {
        try {
            String key = KEY_PREFIX + executionId;
            String value = redisService.getString(key);

            if (value == null) {
                return Optional.empty();
            }

            return Optional.of(objectMapper.readValue(value, Execution.class));
        } catch (Exception e) {
            log.error("[Execution] Failed to find: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public java.util.List<Execution> findByConversationId(String conversationId) {
        try {
            String indexKey = "workflow:conversation:" + conversationId + ":executions";
            java.util.Set<String> executionIds = redisService.getSetMembers(indexKey);

            if (executionIds == null || executionIds.isEmpty()) {
                return java.util.Collections.emptyList();
            }

            java.util.List<String> keys = executionIds.stream()
                    .map(id -> KEY_PREFIX + id)
                    .collect(java.util.stream.Collectors.toList());

            java.util.List<String> values = redisService.multiGetString(keys);

            if (values == null) {
                return java.util.Collections.emptyList();
            }

            return values.stream()
                    .filter(java.util.Objects::nonNull)
                    .map(v -> {
                        try {
                            return objectMapper.readValue(v, Execution.class);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .collect(java.util.stream.Collectors.toList());

        } catch (Exception e) {
            log.error("[Execution] Failed to find by conversationId: {}", e.getMessage(), e);
            return java.util.Collections.emptyList();
        }
    }

    @Override
    public void update(Execution execution) {
        String key = KEY_PREFIX + execution.getExecutionId();
        String versionKey = key + VERSION_KEY_SUFFIX;
        org.redisson.api.RAtomicLong versionCounter = redissonClient.getAtomicLong(versionKey);

        // Single-threaded scheduler: no concurrent writes. Set version directly.
        versionCounter.set(execution.getVersion());

        try {
            String newValue = objectMapper.writeValueAsString(execution);
            redisService.setString(key, newValue, TTL_HOURS, TimeUnit.HOURS);
            log.debug("[Execution] Updated: {} (v{})", execution.getExecutionId(), execution.getVersion());
        } catch (Exception e) {
            versionCounter.set(execution.getVersion() - 1);
            log.error("[Execution] Failed to update: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update execution", e);
        }
    }
}
