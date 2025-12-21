package com.zj.aiagent.application.user.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录命令
 * 
 * @author zj
 * @since 2025-12-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginCommand {

    /**
     * 账号（用户名/邮箱/手机号）
     */
    private String account;

    /**
     * 密码
     */
    private String password;

    /**
     * 登录IP
     */
    private String loginIp;
}
