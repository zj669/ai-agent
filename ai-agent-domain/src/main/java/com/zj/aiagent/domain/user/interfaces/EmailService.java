package com.zj.aiagent.domain.user.interfaces;

/**
 * 邮箱服务接口 - 领域层
 * 
 * <p>
 * 定义邮箱验证码发送和验证功能的契约，具体实现由基础设施层提供
 * 
 * @author zj
 * @since 2025-12-21
 */
public interface EmailService {

    /**
     * 发送验证码
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
     * @return true-验证成功，false-验证失败
     */
    boolean verifyCode(String email, String code);

}
