package com.zj.aiagent.infrastructure.user.ratelimit;

import com.zj.aiagent.domain.user.service.RateLimitDomainService;
import com.zj.aiagent.domain.user.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.zj.aiagent.infrastructure.redis.IRedisService;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

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
        // 获取当前计数
        String countStr = redisService.getValue(key);
        int count = countStr == null ? 0 : Integer.parseInt(countStr);

        // 检查是否超过限流阈值
        if (rateLimitDomainService.isRateLimitExceeded(count, maxAttempts)) {
            log.warn("触发限流: key={}, count={}, maxAttempts={}", key, count, maxAttempts);
            throw new IllegalStateException("操作过于频繁，请稍后再试。限制：" + description);
        }

        // 增加计数
        long newCount = redisService.incr(key);

        // 如果是第一次计数，设置过期时间
        if (newCount == 1) {
            redisService.setValue(key, String.valueOf(newCount), timeWindowSeconds);
        }

        log.debug("限流检查通过: key={}, count={}/{}", key, newCount, maxAttempts);
    }
}
