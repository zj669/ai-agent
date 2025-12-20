package com.zj.aiagemt.service.impl;

import com.zj.aiagemt.constants.EmailConstants;
import com.zj.aiagemt.constants.EmailRedisKeyConstants;
import com.zj.aiagemt.model.entity.EmailSendLog;
import com.zj.aiagemt.repository.base.EmailSendLogMapper;
import com.zj.aiagemt.service.EmailService;
import com.zj.aiagemt.utils.EmailCodeGenerator;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 邮件服务实现类
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Resource
    private JavaMailSender mailSender;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private EmailSendLogMapper emailSendLogMapper;

    @Override
    public void sendVerificationCode(String email, String ip, String deviceId) {
        // 1. 生成验证码
        String code = EmailCodeGenerator.generateCode();

        // 2. 保存验证码到Redis（5分钟过期）
        String key = String.format(EmailRedisKeyConstants.VERIFICATION_CODE, email);
        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.set(code, Duration.ofMinutes(EmailConstants.CODE_EXPIRE_MINUTES));

        log.info("验证码已生成并保存到Redis, email: {}, code: {}", email, code);

        // 3. 异步发送邮件
        sendEmailAsync(email, code, ip, deviceId);
    }

    @Override
    public boolean verifyCode(String email, String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }

        String key = String.format(EmailRedisKeyConstants.VERIFICATION_CODE, email);
        RBucket<String> bucket = redissonClient.getBucket(key);
        String savedCode = bucket.get();

        if (savedCode == null) {
            log.warn("验证码不存在或已过期, email: {}", email);
            return false;
        }

        boolean isValid = savedCode.equals(code);
        if (isValid) {
            // 验证成功后立即删除验证码,防止重复使用
            bucket.delete();
            log.info("验证码验证成功, email: {}", email);
        } else {
            log.warn("验证码验证失败, email: {}, inputCode: {}, savedCode: {}",
                    email, code, savedCode);
        }

        return isValid;
    }

    /**
     * 异步发送邮件
     */
    @Async
    protected void sendEmailAsync(String email, String code, String ip, String deviceId) {
        EmailSendLog sendLog = EmailSendLog.builder()
                .email(email)
                .emailType(EmailConstants.EMAIL_TYPE_VERIFICATION_CODE)
                .ip(ip)
                .deviceId(deviceId)
                .createTime(LocalDateTime.now())
                .build();

        try {
            // 创建邮件
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject(EmailConstants.EMAIL_SUBJECT_VERIFICATION_CODE);

            // 使用HTML模板
            String content = String.format(
                    EmailConstants.EMAIL_TEMPLATE_VERIFICATION_CODE,
                    code,
                    EmailConstants.CODE_EXPIRE_MINUTES);
            helper.setText(content, true);

            // 发送邮件
            mailSender.send(message);

            // 记录成功日志
            sendLog.setSendStatus(EmailConstants.SEND_STATUS_SUCCESS);
            log.info("邮件发送成功, email: {}", email);

        } catch (MessagingException | MailException e) {
            // 记录失败日志
            sendLog.setSendStatus(EmailConstants.SEND_STATUS_FAIL);
            sendLog.setErrorMsg(e.getMessage());
            log.error("邮件发送失败, email: {}, error: {}", email, e.getMessage(), e);
        } finally {
            // 保存发送日志
            try {
                emailSendLogMapper.insert(sendLog);
            } catch (Exception e) {
                log.error("保存邮件发送日志失败, email: {}", email, e);
            }
        }
    }
}
