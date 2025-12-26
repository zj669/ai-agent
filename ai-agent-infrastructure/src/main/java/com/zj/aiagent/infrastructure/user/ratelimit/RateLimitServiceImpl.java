package com.zj.aiagent.infrastructure.user.ratelimit;

import com.zj.aiagent.domain.user.service.RateLimitDomainService;
import com.zj.aiagent.domain.user.interfaces.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.zj.aiagent.infrastructure.redis.IRedisService;
import org.springframework.stereotype.Service;

/**
 * 限流服务实现 - 基础设施层
 * 
 * <p>
 * 基于Redis实现IP限流、邮箱限流、设备指纹限流
 * 
 * @author zj
 * @since 2025-12-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private final IRedisService redisService;
    private final RateLimitDomainService rateLimitDomainService;

    /**
     * 检查IP限流
     * 
     * @param ip IP地址
     * @throws IllegalStateException 超过限流阈值时抛出
     */
    @Override
    public void checkIpRateLimit(String ip) {
        String key = rateLimitDomainService.getIpRateLimitKey(ip);
        checkRateLimit(
                key,
                RateLimitDomainService.IpRateLimitConfig.MAX_ATTEMPTS,
                RateLimitDomainService.IpRateLimitConfig.TIME_WINDOW_SECONDS,
                RateLimitDomainService.IpRateLimitConfig.getDescription());
    }

    /**
     * 检查邮箱限流
     * 
     * @param email 邮箱地址
     * @throws IllegalStateException 超过限流阈值时抛出
     */
    @Override
    public void checkEmailRateLimit(String email) {
        String key = rateLimitDomainService.getEmailRateLimitKey(email);
        checkRateLimit(
                key,
                RateLimitDomainService.EmailRateLimitConfig.MAX_ATTEMPTS,
                RateLimitDomainService.EmailRateLimitConfig.TIME_WINDOW_SECONDS,
                RateLimitDomainService.EmailRateLimitConfig.getDescription());
    }

    /**
     * 检查设备指纹限流
     * 
     * @param deviceId 设备指纹
     * @throws IllegalStateException 超过限流阈值时抛出
     */
    @Override
    public void checkDeviceRateLimit(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            return; // 设备指纹可选，如果没有则不进行限流
        }

        String key = rateLimitDomainService.getDeviceRateLimitKey(deviceId);
        checkRateLimit(
                key,
                RateLimitDomainService.DeviceRateLimitConfig.MAX_ATTEMPTS,
                RateLimitDomainService.DeviceRateLimitConfig.TIME_WINDOW_SECONDS,
                RateLimitDomainService.DeviceRateLimitConfig.getDescription());
    }

    /**
     * 执行所有限流检查
     * 
     * @param email    邮箱地址
     * @param ip       IP地址
     * @param deviceId 设备指纹
     * @throws IllegalStateException 超过任一限流阈值时抛出
     */
    public void checkAllLimits(String email, String ip, String deviceId) {
        checkIpRateLimit(ip);
        checkEmailRateLimit(email);
        checkDeviceRateLimit(deviceId);
    }

    /**
     * 通用限流检查
     * 
     * @param key               Redis键
     * @param maxAttempts       最大尝试次数
     * @param timeWindowSeconds 时间窗口（秒）
     * @param description       限流描述
     * @throws IllegalStateException 超过限流阈值时抛出
     */
    private void checkRateLimit(String key, int maxAttempts, int timeWindowSeconds, String description) {
        // 获取当前计数(使用 getAtomicLong 确保类型一致)
        Long currentCount = redisService.getAtomicLong(key);
        long count = currentCount == null ? 0L : currentCount;

        // 检查是否超过限流阈值
        if (rateLimitDomainService.isRateLimitExceeded((int) count, maxAttempts)) {
            log.warn("触发限流: key={}, count={}, maxAttempts={}", key, count, maxAttempts);
            throw new IllegalStateException("操作过于频繁,请稍后再试。限制:" + description);
        }

        // 增加计数(incr 会自动创建 key 并设置为 1,或者增加现有值)
        long newCount = redisService.incr(key);

        // 如果是第一次计数,设置过期时间
        if (newCount == 1) {
            // 使用 expire 方法仅设置过期时间,不改变值的类型
            redisService.expire(key, timeWindowSeconds);
        }

        log.debug("限流检查通过: key={}, count={}/{}", key, newCount, maxAttempts);
    }
}
