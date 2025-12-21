package com.zj.aiagent.infrastructure.user.email;

import com.zj.aiagent.domain.user.service.EmailService;
import com.zj.aiagent.domain.user.service.EmailVerificationDomainService;
import com.zj.aiagent.infrastructure.persistence.entity.EmailSendLogPO;
import com.zj.aiagent.infrastructure.persistence.mapper.EmailSendLogMapper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.zj.aiagent.infrastructure.redis.IRedisService;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 邮箱服务实现 - 基础设施层
 * 
 * <p>
 * 负责发送邮箱验证码、验证验证码等功能
 * 
 * @author zj
 * @since 2025-12-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final IRedisService redisService;
    private final EmailVerificationDomainService emailVerificationDomainService;
    private final JavaMailSender mailSender;
    private final EmailSendLogMapper emailSendLogMapper;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username}")
    private String mailUsername;

    private static final String VERIFICATION_CODE_PREFIX = "email:verification:code:";

    /**
     * 发送验证码
     * 
     * @param email    邮箱地址
     * @param ip       请求IP
     * @param deviceId 设备指纹
     */
    public void sendVerificationCode(String email, String ip, String deviceId) {
        log.info("发送验证码, email: {}, ip: {}", email, ip);

        // 1. 生成验证码
        String code = emailVerificationDomainService.generateVerificationCode();

        // 2. 存储验证码到Redis(5分钟过期)
        String key = VERIFICATION_CODE_PREFIX + email;
        long expirySeconds = emailVerificationDomainService.getCodeExpiryMinutes() * 60;
        redisService.setValue(key, code, expirySeconds);

        // 3. 异步发送邮件
        sendEmailAsync(email, code);
    }

    /**
     * 验证验证码
     * 
     * @param email 邮箱地址
     * @param code  验证码
     * @return true-验证成功，false-验证失败
     */
    public boolean verifyCode(String email, String code) {
        if (!emailVerificationDomainService.isValidCodeFormat(code)) {
            log.warn("验证码格式错误, email: {}, code: {}", email, code);
            return false;
        }

        // 从Redis中获取验证码
        String key = VERIFICATION_CODE_PREFIX + email;
        String storedCode = redisService.getValue(key);

        if (storedCode == null) {
            log.warn("验证码不存在或已过期, email: {}", email);
            return false;
        }

        if (!storedCode.equals(code)) {
            log.warn("验证码错误, email: {}", email);
            return false;
        }

        // 验证成功后删除验证码
        redisService.remove(key);
        log.info("验证码验证成功, email: {}", email);
        return true;
    }

    /**
     * 异步发送邮件
     * 
     * @param email 邮箱地址
     * @param code  验证码
     */
    @Async
    protected void sendEmailAsync(String email, String code) {
        try {
            // 创建邮件
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            // 设置发件人（必须与授权用户一致）
            helper.setFrom(mailUsername);

            helper.setSubject(emailVerificationDomainService.buildVerificationEmailSubject());

            // 使用HTML模板
            String content = buildHtmlEmailContent(code);
            helper.setText(content, true);

            // 发送邮件
            mailSender.send(message);

            log.info("邮件发送成功, email: {}", email);
            emailSendLogMapper.insert(EmailSendLogPO.builder()
                    .email(email)
                    .verificationCode(code)
                    .sendStatus(1)
                    .build());
        } catch (MessagingException | MailException e) {
            log.error("邮件发送失败, email: {}, error: {}", email, e.getMessage(), e);
            // 注意：这里可以考虑添加重试机制或者记录到数据库
            emailSendLogMapper.insert(EmailSendLogPO.builder()
                    .email(email)
                    .verificationCode(code)
                    .sendStatus(0)
                    .errorMsg(e.getMessage())
                    .build());
        }
    }

    /**
     * 构建HTML邮件内容
     * 
     * @param code 验证码
     * @return HTML内容
     */
    private String buildHtmlEmailContent(String code) {
        int expiryMinutes = emailVerificationDomainService.getCodeExpiryMinutes();

        return "<div style='padding: 20px; background-color: #f5f5f5;'>" +
                "<div style='max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px;'>"
                +
                "<h2 style='color: #333; text-align: center;'>AI Agent 邮箱验证</h2>" +
                "<div style='margin: 30px 0; padding: 20px; background-color: #f8f9fa; border-left: 4px solid #007bff;'>"
                +
                "<p style='margin: 0; font-size: 14px; color: #666;'>您的验证码是：</p>" +
                "<p style='margin: 10px 0; font-size: 32px; font-weight: bold; color: #007bff; letter-spacing: 5px;'>"
                + code + "</p>" +
                "<p style='margin: 0; font-size: 14px; color: #999;'>验证码有效期为 " + expiryMinutes + " 分钟</p>" +
                "</div>" +
                "<p style='color: #666; font-size: 14px; line-height: 1.6;'>如果这不是您本人的操作，请忽略此邮件。</p>" +
                "<hr style='border: none; border-top: 1px solid #eee; margin: 20px 0;'>" +
                "<p style='color: #999; font-size: 12px; text-align: center;'>此邮件由系统自动发送，请勿回复。</p>" +
                "</div>" +
                "</div>";
    }
}
