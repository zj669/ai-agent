package com.zj.aiagemt.service.impl;

import com.zj.aiagemt.constants.EmailRedisKeyConstants;
import com.zj.aiagemt.service.EmailLimitService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 邮件限流服务实现类
 */
@Slf4j
@Service
public class EmailLimitServiceImpl implements EmailLimitService {

    @Resource
    private RedissonClient redissonClient;

    @Override
    public void checkIpLimit(String ip) {
        if (ip == null || ip.isEmpty() || "unknown".equals(ip)) {
            // IP获取失败,跳过IP限流
            return;
        }

        // 检查1分钟限制
        String key1min = String.format(EmailRedisKeyConstants.IP_LIMIT_1MIN, ip);
        RAtomicLong counter1min = redissonClient.getAtomicLong(key1min);
        long count1min = counter1min.get();
        if (count1min >= 1) {
            throw new IllegalStateException("发送过于频繁,请1分钟后再试");
        }
        counter1min.incrementAndGet();
        counter1min.expire(Duration.ofMinutes(1));

        // 检查1小时限制
        String key1hour = String.format(EmailRedisKeyConstants.IP_LIMIT_1HOUR, ip);
        RAtomicLong counter1hour = redissonClient.getAtomicLong(key1hour);
        long count1hour = counter1hour.get();
        if (count1hour >= 5) {
            throw new IllegalStateException("发送次数过多,请1小时后再试");
        }
        counter1hour.incrementAndGet();
        counter1hour.expire(Duration.ofHours(1));

        // 检查1天限制
        String key1day = String.format(EmailRedisKeyConstants.IP_LIMIT_1DAY, ip);
        RAtomicLong counter1day = redissonClient.getAtomicLong(key1day);
        long count1day = counter1day.get();
        if (count1day >= 10) {
            throw new IllegalStateException("今日发送次数已达上限,请明天再试");
        }
        counter1day.incrementAndGet();
        counter1day.expire(Duration.ofDays(1));

        log.debug("IP限流检查通过, ip: {}, 1min: {}, 1hour: {}, 1day: {}",
                ip, count1min + 1, count1hour + 1, count1day + 1);
    }

    @Override
    public void checkEmailLimit(String email) {
        // 检查1分钟限制
        String key1min = String.format(EmailRedisKeyConstants.EMAIL_LIMIT_1MIN, email);
        RAtomicLong counter1min = redissonClient.getAtomicLong(key1min);
        long count1min = counter1min.get();
        if (count1min >= 1) {
            throw new IllegalStateException("发送过于频繁,请1分钟后再试");
        }
        counter1min.incrementAndGet();
        counter1min.expire(Duration.ofMinutes(1));

        // 检查1小时限制
        String key1hour = String.format(EmailRedisKeyConstants.EMAIL_LIMIT_1HOUR, email);
        RAtomicLong counter1hour = redissonClient.getAtomicLong(key1hour);
        long count1hour = counter1hour.get();
        if (count1hour >= 3) {
            throw new IllegalStateException("该邮箱发送次数过多,请1小时后再试");
        }
        counter1hour.incrementAndGet();
        counter1hour.expire(Duration.ofHours(1));

        // 检查1天限制
        String key1day = String.format(EmailRedisKeyConstants.EMAIL_LIMIT_1DAY, email);
        RAtomicLong counter1day = redissonClient.getAtomicLong(key1day);
        long count1day = counter1day.get();
        if (count1day >= 5) {
            throw new IllegalStateException("该邮箱今日发送次数已达上限,请明天再试");
        }
        counter1day.incrementAndGet();
        counter1day.expire(Duration.ofDays(1));

        log.debug("邮箱限流检查通过, email: {}, 1min: {}, 1hour: {}, 1day: {}",
                email, count1min + 1, count1hour + 1, count1day + 1);
    }

    @Override
    public void checkDeviceLimit(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            // 设备指纹为空,跳过设备限流
            return;
        }

        // 检查1小时限制
        String key = String.format(EmailRedisKeyConstants.DEVICE_LIMIT_1HOUR, deviceId);
        RAtomicLong counter = redissonClient.getAtomicLong(key);
        long count = counter.get();
        if (count >= 3) {
            throw new IllegalStateException("该设备注册次数过多,请1小时后再试");
        }
        counter.incrementAndGet();
        counter.expire(Duration.ofHours(1));

        log.debug("设备指纹限流检查通过, deviceId: {}, 1hour: {}", deviceId, count + 1);
    }

    @Override
    public void checkAllLimits(String email, String ip, String deviceId) {
        checkEmailLimit(email);
        checkIpLimit(ip);
        checkDeviceLimit(deviceId);
        log.info("所有限流检查通过, email: {}, ip: {}, deviceId: {}", email, ip, deviceId);
    }
}
