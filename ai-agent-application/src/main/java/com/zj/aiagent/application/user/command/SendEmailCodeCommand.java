package com.zj.aiagent.application.user.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送邮箱验证码命令
 * 
 * @author zj
 * @since 2025-12-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendEmailCodeCommand {

    /**
     * 邮箱地址
     */
    private String email;

    /**
     * 请求IP
     */
    private String ip;

    /**
     * 设备指纹
     */
    private String deviceId;
}
