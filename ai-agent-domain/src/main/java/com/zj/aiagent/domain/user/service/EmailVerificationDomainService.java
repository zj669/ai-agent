package com.zj.aiagent.domain.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Random;

/**
 * 邮箱验证领域服务
 * 
 * <p>
 * 负责验证码生成、验证等邮箱验证相关的业务逻辑
 * 
 * @author zj
 * @since 2025-12-21
 */
@Slf4j
@Service
public class EmailVerificationDomainService {

    private static final Random RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 6;
    private static final int CODE_EXPIRY_MINUTES = 5;

    /**
     * 生成6位数字验证码
     * 
     * @return 验证码
     */
    public String generateVerificationCode() {
        int code = 100000 + RANDOM.nextInt(900000); // 生成100000-999999之间的随机数
        String codeStr = String.valueOf(code);
        log.debug("生成验证码: {}", codeStr);
        return codeStr;
    }

    /**
     * 获取验证码过期时间（分钟）
     * 
     * @return 过期时间（分钟）
     */
    public int getCodeExpiryMinutes() {
        return CODE_EXPIRY_MINUTES;
    }

    /**
     * 验证验证码格式
     * 
     * @param code 验证码
     * @return true-格式正确，false-格式错误
     */
    public boolean isValidCodeFormat(String code) {
        if (code == null || code.length() != CODE_LENGTH) {
            return false;
        }
        return code.matches("\\d{" + CODE_LENGTH + "}");
    }

    /**
     * 构建验证码邮件内容
     * 
     * @param code  验证码
     * @param email 邮箱地址
     * @return 邮件内容
     */
    public String buildVerificationEmailContent(String code, String email) {
        return String.format(
                "您好！\n\n" +
                        "您正在注册AI Agent平台账号，验证码为：%s\n\n" +
                        "验证码有效期为%d分钟，请尽快完成验证。\n\n" +
                        "如果这不是您的操作，请忽略此邮件。\n\n" +
                        "AI Agent团队",
                code,
                CODE_EXPIRY_MINUTES);
    }

    /**
     * 构建验证码邮件主题
     * 
     * @return 邮件主题
     */
    public String buildVerificationEmailSubject() {
        return "【AI Agent】邮箱验证码";
    }
}
