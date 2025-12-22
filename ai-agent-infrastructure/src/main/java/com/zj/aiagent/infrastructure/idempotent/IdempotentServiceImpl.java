package com.zj.aiagent.infrastructure.idempotent;

import com.zj.aiagent.infrastructure.redis.IRedisService;
import com.zj.aiagent.shared.constants.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 幂等服务实现
 * <p>
 * 基于Redisson的setNx实现分布式幂等锁
 *
 * @author zj
 * @since 2025-12-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotentServiceImpl implements IIdempotentService {

    private final IRedisService redisService;

    @Override
    public boolean tryAcquire(String key, long expireSeconds) {
        String fullKey = RedisKeyConstants.Idempotent.PREFIX + key;
        Boolean success = redisService.setNx(fullKey, expireSeconds, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(success)) {
            log.debug("幂等锁获取成功: key={}, expire={}s", fullKey, expireSeconds);
            return true;
        } else {
            log.debug("幂等锁获取失败(重复请求): key={}", fullKey);
            return false;
        }
    }

    @Override
    public void release(String key) {
        String fullKey = RedisKeyConstants.Idempotent.PREFIX + key;
        redisService.remove(fullKey);
        log.debug("幂等锁已释放: key={}", fullKey);
    }
}
