package com.zj.aiagent.infrastructure.user.email;

import com.zj.aiagent.domain.user.service.EmailVerificationDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
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
public class EmailServiceImpl {

    private final StringRedisTemplate stringRedisTemplate;
    private final EmailVerificationDomainService emailVerificationDomainService;

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

        // 2. 存储验证码到Redis（5分钟过期）
        String key = VERIFICATION_CODE_PREFIX + email;
        stringRedisTemplate.opsForValue().set(
                key,
                code,
                emailVerificationDomainService.getCodeExpiryMinutes(),
                TimeUnit.MINUTES);

        // 3. 发送邮件（这里暂时只是记录日志，实际需要对接邮件服务）
        String subject = emailVerificationDomainService.buildVerificationEmailSubject();
        String content = emailVerificationDomainService.buildVerificationEmailContent(code, email);

        log.info("模拟发送邮件: email={}, subject={}, code={}", email, subject, code);

        // TODO: 实际对接邮件服务
//         mailSender.send(email, subject, content);
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
        String storedCode = stringRedisTemplate.opsForValue().get(key);

        if (storedCode == null) {
            log.warn("验证码不存在或已过期, email: {}", email);
            return false;
        }

        if (!storedCode.equals(code)) {
            log.warn("验证码错误, email: {}", email);
            return false;
        }

        // 验证成功后删除验证码
        stringRedisTemplate.delete(key);
        log.info("验证码验证成功, email: {}", email);
        return true;
    }
}
