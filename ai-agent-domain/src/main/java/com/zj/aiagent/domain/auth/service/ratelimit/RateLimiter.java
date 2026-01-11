package com.zj.aiagent.domain.auth.service.ratelimit;

/**
 * 限流策略接口
 */
public interface RateLimiter {
    /**
     * 尝试获取许可
     *
     * @param key           限流键
     * @param limit         限制次数
     * @param periodSeconds 时间窗口（秒）
     * @return true-通过，false-被限流
     */
    boolean tryAcquire(String key, int limit, int periodSeconds);
}
