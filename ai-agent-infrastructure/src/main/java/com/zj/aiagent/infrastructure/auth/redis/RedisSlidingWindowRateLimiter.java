package com.zj.aiagent.infrastructure.auth.redis;

import com.zj.aiagent.domain.auth.service.ratelimit.RateLimiter;
import com.zj.aiagent.shared.constants.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;

/**
 * 基于 Redis ZSet 的滑动窗口限流实现
 */
@Slf4j
@Component("redisSlidingWindowRateLimiter")
@RequiredArgsConstructor
public class RedisSlidingWindowRateLimiter implements RateLimiter {

    private final StringRedisTemplate redisTemplate;

    // LUA Script for Sliding Window
    // KEYS[1]: key
    // ARGV[1]: limit
    // ARGV[2]: window (seconds)
    // ARGV[3]: now (millis)
    // ARGV[4]: member (uuid)
    private static final String LUA_SCRIPT = "local key = KEYS[1]\n" +
            "local limit = tonumber(ARGV[1])\n" +
            "local window = tonumber(ARGV[2])\n" +
            "local now = tonumber(ARGV[3])\n" +
            "local member = ARGV[4]\n" +
            "\n" +
            "-- remove old entries (older than now - window*1000)\n" +
            "redis.call('ZREMRANGEBYSCORE', key, 0, now - window * 1000)\n" +
            "\n" +
            "-- count existing\n" +
            "local count = redis.call('ZCARD', key)\n" +
            "\n" +
            "if count < limit then\n" +
            "    redis.call('ZADD', key, now, member)\n" +
            "    redis.call('EXPIRE', key, window + 1) -- Extend TTL\n" +
            "    return 1\n" +
            "else\n" +
            "    return 0\n" +
            "end";

    @Override
    public boolean tryAcquire(String key, int limit, int periodSeconds) {
        if (limit <= 0)
            return false;

        long now = System.currentTimeMillis();
        String member = UUID.randomUUID().toString();

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);

        try {
            Long result = redisTemplate.execute(script,
                    Collections.singletonList(RedisKeyConstants.RateLimit.IP_PREFIX + key),
                    String.valueOf(limit),
                    String.valueOf(periodSeconds),
                    String.valueOf(now),
                    member);

            return result != null && result == 1L;
        } catch (Exception e) {
            log.error("Rate limit check failed for key: {}", key, e);
            // In case of Redis error, we usually default to ALLOW (fail open) or BLOCK
            // (fail closed).
            // Here assuming fail-open to avoid blocking users on infra error, or user
            // choice.
            // Requirement says "Availability ... fallbacks or fail-fast".
            // Let's return true (allow) on redis failure to preserve availability, but log
            // error.
            return true;
        }
    }
}
