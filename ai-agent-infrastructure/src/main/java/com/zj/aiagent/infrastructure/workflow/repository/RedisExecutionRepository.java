package com.zj.aiagent.infrastructure.workflow.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.workflow.entity.Execution;
import com.zj.aiagent.domain.workflow.port.ExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private static final long TTL_HOURS = 48; // 执行数据保留 48 小时

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void save(Execution execution) {
        try {
            String key = KEY_PREFIX + execution.getExecutionId();
            String value = objectMapper.writeValueAsString(execution);
            redisTemplate.opsForValue().set(key, value, TTL_HOURS, TimeUnit.HOURS);

            // Maintain Secondary Index
            String indexKey = "workflow:conversation:" + execution.getConversationId() + ":executions";
            redisTemplate.opsForSet().add(indexKey, execution.getExecutionId());
            redisTemplate.expire(indexKey, TTL_HOURS, TimeUnit.HOURS);

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
            String value = redisTemplate.opsForValue().get(key);

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
            java.util.Set<String> executionIds = redisTemplate.opsForSet().members(indexKey);

            if (executionIds == null || executionIds.isEmpty()) {
                return java.util.Collections.emptyList();
            }

            java.util.List<String> keys = executionIds.stream()
                    .map(id -> KEY_PREFIX + id)
                    .collect(java.util.stream.Collectors.toList());

            java.util.List<String> values = redisTemplate.opsForValue().multiGet(keys);

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
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())) // Sort desc
                    .collect(java.util.stream.Collectors.toList());

        } catch (Exception e) {
            log.error("[Execution] Failed to find by conversationId: {}", e.getMessage(), e);
            return java.util.Collections.emptyList();
        }
    }

    @Override
    public void update(Execution execution) {
        try {
            String key = KEY_PREFIX + execution.getExecutionId();
            String existingValue = redisTemplate.opsForValue().get(key);

            if (existingValue != null) {
                Execution existing = objectMapper.readValue(existingValue, Execution.class);
                // 乐观锁检查
                if (!existing.getVersion().equals(execution.getVersion() - 1)) {
                    throw new OptimisticLockingFailureException(
                            "Execution version mismatch. Expected: " + existing.getVersion() +
                                    ", Got: " + (execution.getVersion() - 1));
                }
            }

            String value = objectMapper.writeValueAsString(execution);
            redisTemplate.opsForValue().set(key, value, TTL_HOURS, TimeUnit.HOURS);
            log.debug("[Execution] Updated: {} (v{})", execution.getExecutionId(), execution.getVersion());
        } catch (OptimisticLockingFailureException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Execution] Failed to update: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update execution", e);
        }
    }

    @Override
    public void delete(String executionId) {
        try {
            // Needed to remove from index... need to know conversationId first?
            // If delete without reading, index might remain stale. Expiry handles cleanup
            // though.
            // Or we read it first.
            String key = KEY_PREFIX + executionId;
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                Execution ex = objectMapper.readValue(value, Execution.class);
                String indexKey = "workflow:conversation:" + ex.getConversationId() + ":executions";
                redisTemplate.opsForSet().remove(indexKey, executionId);
            }

            redisTemplate.delete(key);
            log.debug("[Execution] Deleted: {}", executionId);
        } catch (Exception e) {
            log.error("[Execution] Failed to delete: {}", e.getMessage(), e);
        }
    }
}
