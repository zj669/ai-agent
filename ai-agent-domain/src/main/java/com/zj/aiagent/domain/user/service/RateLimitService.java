package com.zj.aiagent.domain.user.service;

/**
 * 限流服务接口 - 领域层
 * 
 * <p>
 * 定义限流功能的契约，具体实现由基础设施层提供
 * 
 * @author zj
 * @since 2025-12-21
 */
public interface RateLimitService {

    /**
     * 检查IP限流
     * 
     * @param ip IP地址
     * @throws IllegalStateException 超过限流阈值时抛出
     */
    void checkIpRateLimit(String ip);

    /**
     * 检查邮箱限流
     * 
     * @param email 邮箱地址
     * @throws IllegalStateException 超过限流阈值时抛出
     */
    void checkEmailRateLimit(String email);

    /**
     * 检查设备指纹限流
     * 
     * @param deviceId 设备指纹
     * @throws IllegalStateException 超过限流阈值时抛出
     */
    void checkDeviceRateLimit(String deviceId);

    /**
     * 执行所有限流检查
     * 
     * @param email    邮箱地址
     * @param ip       IP地址
     * @param deviceId 设备指纹
     * @throws IllegalStateException 超过任一限流阈值时抛出
     */
    void checkAllLimits(String email, String ip, String deviceId);
}
