package com.zj.aiagent.infrastructure.email;

import com.zj.aiagent.domain.user.service.IEmailService;
import com.zj.aiagent.domain.user.valobj.Email;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements IEmailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public boolean sendVerificationCode(Email to, String code) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to.getValue());
            helper.setSubject("【AI-Agent】邮箱验证码");
            helper.setText(buildHtmlContent(code), true);

            javaMailSender.send(mimeMessage);
            log.info("Sent verification code to {}", to.getValue());
            return true;
        } catch (MessagingException e) {
            log.error("Failed to send email to {}", to.getValue(), e);
            return false;
        }
    }

    private String buildHtmlContent(String code) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f7fa; margin: 0; padding: 20px; }
                        .container { max-width: 500px; margin: 0 auto; background: #ffffff; border-radius: 16px; box-shadow: 0 4px 24px rgba(0,0,0,0.08); overflow: hidden; }
                        .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 32px 24px; text-align: center; }
                        .header h1 { color: #ffffff; margin: 0; font-size: 24px; font-weight: 600; }
                        .header p { color: rgba(255,255,255,0.9); margin: 8px 0 0; font-size: 14px; }
                        .content { padding: 40px 32px; text-align: center; }
                        .code-box { background: linear-gradient(135deg, #f5f7fa 0%%, #e4e8ec 100%%); border-radius: 12px; padding: 24px; margin: 24px 0; }
                        .code { font-size: 36px; font-weight: 700; letter-spacing: 8px; color: #667eea; font-family: 'Courier New', monospace; }
                        .tip { color: #666; font-size: 14px; line-height: 1.6; margin-top: 24px; }
                        .warning { background: #fff8e6; border-left: 4px solid #ffc107; padding: 12px 16px; margin: 20px 0; text-align: left; border-radius: 0 8px 8px 0; }
                        .warning p { margin: 0; color: #856404; font-size: 13px; }
                        .footer { background: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #eee; }
                        .footer p { color: #999; font-size: 12px; margin: 0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🤖 AI-Agent</h1>
                            <p>智能代理平台</p>
                        </div>
                        <div class="content">
                            <p style="color: #333; font-size: 16px; margin: 0;">您好！您正在进行邮箱验证</p>
                            <div class="code-box">
                                <div class="code">%s</div>
                            </div>
                            <p class="tip">请在 <strong style="color: #667eea;">5 分钟</strong> 内完成验证</p>
                            <div class="warning">
                                <p>⚠️ 此验证码仅用于 AI-Agent 平台验证，请勿泄露给他人。如非本人操作，请忽略此邮件。</p>
                            </div>
                        </div>
                        <div class="footer">
                            <p>© 2026 AI-Agent Platform. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(code);
    }
}
