package com.zj.aiagent.domain.auth.service.ratelimit;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流器工厂
 * 负责管理和提供不同的限流策略实现
 */
@Component
public class RateLimiterFactory {

    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    // Spring会自动注入所有的RateLimiter实现
    public RateLimiterFactory(Map<String, RateLimiter> limiterMap) {
        this.limiters.putAll(limiterMap);
    }

    /**
     * 获取指定类型的限流器
     *
     * @param type 限流器类型 (e.g., "slidingWindowRateLimiter")
     * @return RateLimiter 实例
     */
    public RateLimiter getRateLimiter(String type) {
        RateLimiter limiter = limiters.get(type);
        if (limiter == null) {
            throw new IllegalArgumentException("Unknown rate limiter type: " + type);
        }
        return limiter;
    }

    /**
     * 获取默认限流器 (滑动窗口)
     */
    public RateLimiter getDefaultLimiter() {
        return getRateLimiter("redisSlidingWindowRateLimiter");
    }
}
