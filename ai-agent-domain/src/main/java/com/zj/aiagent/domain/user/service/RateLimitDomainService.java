package com.zj.aiagent.domain.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 * 限流策略领域服务
 * 
 * <p>
 * 定义IP限流、邮箱限流、设备指纹限流等策略规则
 * 
 * @author zj
 * @since 2025-12-21
 */
@Slf4j
@Service
public class RateLimitDomainService {

    /**
     * IP限流配置
     */
    public static class IpRateLimitConfig {
        /** IP限流时间窗口（秒） */
        public static final int TIME_WINDOW_SECONDS = 60;
        /** IP限流最大次数 */
        public static final int MAX_ATTEMPTS = 5;

        public static String getDescription() {
            return String.format("同一IP每%d秒最多发送%d次", TIME_WINDOW_SECONDS, MAX_ATTEMPTS);
        }
    }

    /**
     * 邮箱限流配置
     */
    public static class EmailRateLimitConfig {
        /** 邮箱限流时间窗口（秒） */
        public static final int TIME_WINDOW_SECONDS = 86400; // 24小时
        /** 邮箱限流最大次数 */
        public static final int MAX_ATTEMPTS = 10;

        public static String getDescription() {
            return String.format("同一邮箱每天最多发送%d次", MAX_ATTEMPTS);
        }
    }

    /**
     * 设备指纹限流配置
     */
    public static class DeviceRateLimitConfig {
        /** 设备指纹限流时间窗口（秒） */
        public static final int TIME_WINDOW_SECONDS = 3600; // 1小时
        /** 设备指纹限流最大次数 */
        public static final int MAX_ATTEMPTS = 20;

        public static String getDescription() {
            return String.format("同一设备每小时最多发送%d次", MAX_ATTEMPTS);
        }
    }

    /**
     * 获取IP限流键
     * 
     * @param ip IP地址
     * @return Redis键
     */
    public String getIpRateLimitKey(String ip) {
        return "rate_limit:ip:" + ip;
    }

    /**
     * 获取邮箱限流键
     * 
     * @param email 邮箱
     * @return Redis键
     */
    public String getEmailRateLimitKey(String email) {
        return "rate_limit:email:" + email;
    }

    /**
     * 获取设备指纹限流键
     * 
     * @param deviceId 设备指纹
     * @return Redis键
     */
    public String getDeviceRateLimitKey(String deviceId) {
        return "rate_limit:device:" + deviceId;
    }

    /**
     * 检查是否超过限流阈值
     * 
     * @param currentCount 当前计数
     * @param maxAttempts  最大尝试次数
     * @return true-超过限制，false-未超过限制
     */
    public boolean isRateLimitExceeded(int currentCount, int maxAttempts) {
        return currentCount >= maxAttempts;
    }
}
