package com.zj.aiagemt.service;

/**
 * 邮件服务接口
 */
public interface EmailService {

    /**
     * 发送验证码邮件
     * 
     * @param email    邮箱地址
     * @param ip       请求IP
     * @param deviceId 设备指纹
     */
    void sendVerificationCode(String email, String ip, String deviceId);

    /**
     * 验证验证码
     * 
     * @param email 邮箱地址
     * @param code  验证码
     * @return 是否验证成功
     */
    boolean verifyCode(String email, String code);
}
