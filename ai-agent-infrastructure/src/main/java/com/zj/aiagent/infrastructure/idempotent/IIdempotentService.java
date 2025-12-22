package com.zj.aiagent.infrastructure.idempotent;

/**
 * 幂等服务接口
 * <p>
 * 提供分布式幂等锁机制，防止短时间内重复请求
 *
 * @author zj
 * @since 2025-12-22
 */
public interface IIdempotentService {

    /**
     * 尝试获取幂等锁
     *
     * @param key           幂等Key
     * @param expireSeconds 过期时间(秒)
     * @return true-获取成功(首次请求), false-获取失败(重复请求)
     */
    boolean tryAcquire(String key, long expireSeconds);

    /**
     * 释放幂等锁
     * <p>
     * 注意: 一般不需要主动释放,依赖过期时间自动释放
     *
     * @param key 幂等Key
     */
    void release(String key);
}
