package com.zj.aiagemt.service;

/**
 * 邮件限流服务接口
 */
public interface EmailLimitService {

    /**
     * 检查IP限流
     * 
     * @param ip IP地址
     * @throws IllegalStateException 如果超出限流限制
     */
    void checkIpLimit(String ip);

    /**
     * 检查邮箱限流
     * 
     * @param email 邮箱地址
     * @throws IllegalStateException 如果超出限流限制
     */
    void checkEmailLimit(String email);

    /**
     * 检查设备指纹限流
     * 
     * @param deviceId 设备指纹
     * @throws IllegalStateException 如果超出限流限制
     */
    void checkDeviceLimit(String deviceId);

    /**
     * 统一检查所有限流
     * 
     * @param email    邮箱地址
     * @param ip       IP地址
     * @param deviceId 设备指纹
     * @throws IllegalStateException 如果超出限流限制
     */
    void checkAllLimits(String email, String ip, String deviceId);
}
