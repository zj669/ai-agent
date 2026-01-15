package com.zj.aiagent.domain.user.service;

import com.zj.aiagent.domain.auth.service.ratelimit.RateLimiter;
import com.zj.aiagent.domain.auth.service.ratelimit.RateLimiterFactory;
import com.zj.aiagent.domain.user.entity.User;
import com.zj.aiagent.domain.user.exception.AuthenticationException;
import com.zj.aiagent.domain.user.exception.AuthenticationException.ErrorCode;
import com.zj.aiagent.domain.user.repository.IUserRepository;
import com.zj.aiagent.domain.user.repository.IVerificationCodeRepository;
import com.zj.aiagent.domain.user.valobj.Credential;
import com.zj.aiagent.domain.user.valobj.Email;
import com.zj.aiagent.domain.user.valobj.UserStatus;
import com.zj.aiagent.shared.constants.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.CompletableFuture;

/**
 * 用户认证领域服务
 * 
 * 封装用户注册、登录的核心业务逻辑
 * 不依赖任何 HTTP 或框架特定的类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthenticationDomainService {

    private final IUserRepository userRepository;
    private final IEmailService emailService;
    private final IVerificationCodeRepository verificationCodeRepository;
    private final RateLimiterFactory rateLimiterFactory;

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int VERIFICATION_CODE_TTL_SECONDS = 300; // 5分钟
    private static final int EMAIL_RATE_LIMIT = 1;
    private static final int EMAIL_RATE_LIMIT_WINDOW_SECONDS = 60;

    /**
     * 发送邮箱验证码
     * 
     * @param emailStr 目标邮箱
     * @throws AuthenticationException 限流或发送失败时抛出
     */
    public void sendVerificationCode(String emailStr) {
        Email email = Email.of(emailStr);
        String rateLimitKey = RedisKeyConstants.RateLimit.EMAIL_PREFIX + emailStr;

        // 限流检查 (1分钟1次)
        RateLimiter limiter = rateLimiterFactory.getDefaultLimiter();
        if (!limiter.tryAcquire(rateLimitKey, EMAIL_RATE_LIMIT, EMAIL_RATE_LIMIT_WINDOW_SECONDS)) {
            log.warn("Email rate limit exceeded for: {}", emailStr);
            throw new AuthenticationException(ErrorCode.RATE_LIMITED);
        }

        String code = generateSecureCode();
        verificationCodeRepository.save(email, code, VERIFICATION_CODE_TTL_SECONDS);
        CompletableFuture.runAsync(() -> {
            userRepository.saveEmailLog(email, code);
        });
        boolean sent = emailService.sendVerificationCode(email, code);
        if (!sent) {
            log.error("Failed to send verification email to: {}", emailStr);
            throw new AuthenticationException(ErrorCode.EMAIL_SEND_FAILED);
        }

        log.info("Verification code sent to: {}", emailStr);
    }

    /**
     * 邮箱注册
     * 
     * @param emailStr 邮箱
     * @param code     验证码
     * @param password 密码
     * @param username 用户名（可选）
     * @return 创建的用户
     * @throws AuthenticationException 验证码无效、邮箱已存在或密码弱时抛出
     */
    public User register(String emailStr, String code, String password, String username) {
        Email email = Email.of(emailStr);

        // 验证码校验
        String storedCode = verificationCodeRepository.get(email);
        if (storedCode == null || !storedCode.equals(code)) {
            log.warn("Invalid verification code for: {}", emailStr);
            throw new AuthenticationException(ErrorCode.INVALID_VERIFICATION_CODE);
        }

        // 邮箱查重
        if (userRepository.existsByEmail(email)) {
            log.warn("Email already registered: {}", emailStr);
            throw new AuthenticationException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        // 密码强度校验
        validatePasswordStrength(password);

        // 创建用户
        Credential credential = Credential.create(password);
        String finalUsername = (username == null || username.isBlank())
                ? extractUsernameFromEmail(emailStr)
                : username;

        User newUser = new User(finalUsername, email, credential);
        User savedUser = userRepository.save(newUser);

        // 销毁验证码（防止重复使用）
        verificationCodeRepository.remove(email);

        log.info("User registered successfully: {}", emailStr);
        return savedUser;
    }

    /**
     * 用户登录
     * 
     * @param emailStr 邮箱
     * @param password 密码
     * @param ip       登录IP
     * @return 登录成功的用户
     * @throws AuthenticationException 凭证无效或用户被禁用时抛出
     */
    public User login(String emailStr, String password, String ip) {
        Email email = Email.of(emailStr);

        User user = userRepository.findByEmail(email);
        if (user == null) {
            log.warn("Login failed - user not found: {}", emailStr);
            throw new AuthenticationException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 验证密码
        if (!user.verifyPassword(password)) {
            log.warn("Login failed - invalid password for: {}", emailStr);
            throw new AuthenticationException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 检查用户状态
        if (user.getStatus() == UserStatus.DISABLED) {
            log.warn("Login failed - user disabled: {}", emailStr);
            throw new AuthenticationException(ErrorCode.USER_DISABLED);
        }

        // 记录登录成功
        user.onLoginSuccess(ip);
        userRepository.save(user);

        log.info("User logged in successfully: {} from IP: {}", emailStr, ip);
        return user;
    }

    /**
     * 生成安全的6位验证码
     */
    private String generateSecureCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * 从邮箱提取用户名
     */
    private String extractUsernameFromEmail(String email) {
        return email.split("@")[0];
    }

    /**
     * 验证密码强度
     */
    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new AuthenticationException(ErrorCode.WEAK_PASSWORD);
        }
    }
}
