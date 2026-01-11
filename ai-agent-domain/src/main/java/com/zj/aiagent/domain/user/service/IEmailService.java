package com.zj.aiagent.domain.user.service;

import com.zj.aiagent.domain.user.valobj.Email;

public interface IEmailService {
    /**
     * 发送验证码邮件
     * 
     * @param to   目标邮箱
     * @param code 验证码
     * @return 成功返回 true
     */
    boolean sendVerificationCode(Email to, String code);
}
