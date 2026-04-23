package com.zj.aiagent.domain.user.service;

import com.zj.aiagent.domain.user.valobj.Email;

public interface IEmailService {
    /**
     * 同步发送验证码邮件
     *
     * @param to   目标邮箱
     * @param code 验证码
     * @return 成功返回 true
     */
    boolean sendVerificationCode(Email to, String code);

    /**
     * 异步发送验证码邮件（fire-and-forget）
     * <p>
     * 邮件发送在后台线程池中执行，调用方无需等待 SMTP I/O 完成。
     * 发送失败时仅记录日志，不影响主流程。
     *
     * @param to   目标邮箱
     * @param code 验证码
     */
    void sendVerificationCodeAsync(Email to, String code);
}
