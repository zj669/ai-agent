package com.zj.aiagent.infrastructure.user.repository;

import com.zj.aiagent.domain.user.repository.IVerificationCodeRepository;
import com.zj.aiagent.domain.user.valobj.Email;
import com.zj.aiagent.infrastructure.user.mapper.EmailLogMapper;
import com.zj.aiagent.shared.constants.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RedisVerificationCodeRepository implements IVerificationCodeRepository {

    private final StringRedisTemplate redisTemplate;

    private final EmailLogMapper emailLogMapper;

    @Override
    public void save(Email email, String code, long expirySeconds) {
        redisTemplate.opsForValue().set(RedisKeyConstants.Email.VERIFICATION_CODE_PREFIX + email.getValue(), code,
                expirySeconds, TimeUnit.SECONDS);

    }

    @Override
    public String get(Email email) {
        return redisTemplate.opsForValue().get(RedisKeyConstants.Email.VERIFICATION_CODE_PREFIX + email.getValue());
    }

    @Override
    public void remove(Email email) {
        redisTemplate.delete(RedisKeyConstants.Email.VERIFICATION_CODE_PREFIX + email.getValue());
    }
}
