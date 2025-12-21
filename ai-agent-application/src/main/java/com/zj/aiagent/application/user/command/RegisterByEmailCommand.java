package com.zj.aiagent.application.user.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邮箱注册命令
 * 
 * @author zj
 * @since 2025-12-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterByEmailCommand {

    /**
     * 邮箱
     */
    private String email;

    /**
     * 验证码
     */
    private String code;

    /**
     * 密码
     */
    private String password;

    /**
     * 用户名（可选，为空则自动生成）
     */
    private String username;

    /**
     * 设备指纹
     */
    private String deviceId;

    /**
     * 请求IP
     */
    private String ip;
}
