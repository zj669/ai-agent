package com.zj.aiagent.infrastructure.user.repository;

import com.zj.aiagent.domain.user.repository.IVerificationCodeRepository;
import com.zj.aiagent.domain.user.valobj.Email;
import com.zj.aiagent.infrastructure.redis.IRedisService;
import com.zj.aiagent.infrastructure.user.mapper.EmailLogMapper;
import com.zj.aiagent.shared.constants.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

/**
 * Redis 验证码仓储实现
 * 
 * 职责：
 * - 管理验证码的存储和检索
 * - 处理验证码的过期时间
 * - 负责 Redis key 的命名规则
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisVerificationCodeRepository implements IVerificationCodeRepository {

    private final IRedisService redisService;

    private final EmailLogMapper emailLogMapper;

    /**
     * 保存验证码
     * 
     * @param email 邮箱
     * @param code 验证码
     * @param expirySeconds 过期时间（秒）
     */
    @Override
    public void save(Email email, String code, long expirySeconds) {
        try {
            // 业务逻辑：构建 Redis key
            String key = RedisKeyConstants.Email.VERIFICATION_CODE_PREFIX + email.getValue();
            
            // 使用 IRedisService 的基础操作
            redisService.setString(key, code, expirySeconds, TimeUnit.SECONDS);
            
            log.debug("[VerificationCode] Saved code for email: {}, expiry: {}s", email.getValue(), expirySeconds);
        } catch (Exception e) {
            log.error("[VerificationCode] Failed to save code for email: {}", email.getValue(), e);
            throw new RuntimeException("Failed to save verification code", e);
        }
    }

    /**
     * 获取验证码
     * 
     * @param email 邮箱
     * @return 验证码，如果不存在或已过期则返回 null
     */
    @Override
    public String get(Email email) {
        try {
            // 业务逻辑：构建 Redis key
            String key = RedisKeyConstants.Email.VERIFICATION_CODE_PREFIX + email.getValue();
            
            // 使用 IRedisService 的基础操作
            String code = redisService.getString(key);
            
            log.debug("[VerificationCode] Retrieved code for email: {}, exists: {}", email.getValue(), code != null);
            return code;
        } catch (Exception e) {
            log.error("[VerificationCode] Failed to get code for email: {}", email.getValue(), e);
            return null;
        }
    }

    /**
     * 移除验证码
     * 
     * @param email 邮箱
     */
    @Override
    public void remove(Email email) {
        try {
            // 业务逻辑：构建 Redis key
            String key = RedisKeyConstants.Email.VERIFICATION_CODE_PREFIX + email.getValue();
            
            // 使用 IRedisService 的基础操作
            redisService.delete(key);
            
            log.debug("[VerificationCode] Removed code for email: {}", email.getValue());
        } catch (Exception e) {
            log.error("[VerificationCode] Failed to remove code for email: {}", email.getValue(), e);
        }
    }
}
