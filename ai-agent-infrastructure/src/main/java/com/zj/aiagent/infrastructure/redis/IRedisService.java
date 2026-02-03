package com.zj.aiagent.infrastructure.redis;

import org.redisson.api.*;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * Redis 服务
 *
 */
public interface IRedisService {

    /**
     * 设置指定 key 的值
     *
     * @param key   键
     * @param value 值
     */
    <T> void setValue(String key, T value);

    /**
     * 设置指定 key 的值
     *
     * @param key     键
     * @param value   值
     * @param expired 过期时间
     */
    <T> void setValue(String key, T value, long expired);

    /**
     * 获取指定 key 的值
     *
     * @param key 键
     * @return 值
     */
    <T> T getValue(String key);

    /**
     * 获取队列
     *
     * @param key 键
     * @param <T> 泛型
     * @return 队列
     */
    <T> RQueue<T> getQueue(String key);

    /**
     * 加锁队列
     *
     * @param key 键
     * @param <T> 泛型
     * @return 队列
     */
    <T> RBlockingQueue<T> getBlockingQueue(String key);

    /**
     * 延迟队列
     *
     * @param rBlockingQueue 加锁队列
     * @param <T>            泛型
     * @return 队列
     */
    <T> RDelayedQueue<T> getDelayedQueue(RBlockingQueue<T> rBlockingQueue);

    /**
     * 设置值
     *
     * @param key   key 键
     * @param value 值
     */
    void setAtomicLong(String key, long value);

    /**
     * 获取值
     *
     * @param key key 键
     */
    Long getAtomicLong(String key);

    /**
     * 自增 Key 的值；1、2、3、4
     *
     * @param key 键
     * @return 自增后的值
     */
    long incr(String key);

    /**
     * 指定值，自增 Key 的值；1、2、3、4
     *
     * @param key 键
     * @return 自增后的值
     */
    long incrBy(String key, long delta);

    /**
     * 自减 Key 的值；1、2、3、4
     *
     * @param key 键
     * @return 自增后的值
     */
    long decr(String key);

    /**
     * 指定值，自增 Key 的值；1、2、3、4
     *
     * @param key 键
     * @return 自增后的值
     */
    long decrBy(String key, long delta);

    /**
     * 移除指定 key 的值
     *
     * @param key 键
     */
    void remove(String key);

    /**
     * 设置指定 key 的过期时间
     *
     * @param key     键
     * @param seconds 过期时间(秒)
     * @return true-设置成功, false-设置失败
     */
    boolean expire(String key, long seconds);

    /**
     * 判断指定 key 的值是否存在
     *
     * @param key 键
     * @return true/false
     */
    boolean isExists(String key);

    /**
     * 将指定的值添加到集合中
     *
     * @param key   键
     * @param value 值
     */
    void addToSet(String key, String value);

    /**
     * 判断指定的值是否是集合的成员
     *
     * @param key   键
     * @param value 值
     * @return 如果是集合的成员返回 true，否则返回 false
     */
    boolean isSetMember(String key, String value);

    /**
     * 将指定的值添加到列表中
     *
     * @param key   键
     * @param value 值
     */
    void addToList(String key, String value);

    /**
     * 获取列表中指定索引的值
     *
     * @param key   键
     * @param index 索引
     * @return 值
     */
    String getFromList(String key, int index);

    /**
     * 获取Map
     *
     * @param key 键
     * @return 值
     */
    <K, V> RMap<K, V> getMap(String key);

    /**
     * 将指定的键值对添加到哈希表中
     *
     * @param key   键
     * @param field 字段
     * @param value 值
     */
    void addToMap(String key, String field, String value);

    /**
     * 获取哈希表中指定字段的值
     *
     * @param key   键
     * @param field 字段
     * @return 值
     */
    String getFromMap(String key, String field);

    /**
     * 获取哈希表中指定字段的值
     *
     * @param key   键
     * @param field 字段
     * @return 值
     */
    <K, V> V getFromMap(String key, K field);

    /**
     * 将指定的值添加到有序集合中
     *
     * @param key   键
     * @param value 值
     */
    void addToSortedSet(String key, String value);

    /**
     * 获取 Redis 锁（可重入锁）
     *
     * @param key 键
     * @return Lock
     */
    RLock getLock(String key);

    /**
     * 获取 Redis 锁（公平锁）
     *
     * @param key 键
     * @return Lock
     */
    RLock getFairLock(String key);

    /**
     * 获取 Redis 锁（读写锁）
     *
     * @param key 键
     * @return RReadWriteLock
     */
    RReadWriteLock getReadWriteLock(String key);

    /**
     * 获取 Redis 信号量
     *
     * @param key 键
     * @return RSemaphore
     */
    RSemaphore getSemaphore(String key);

    /**
     * 获取 Redis 过期信号量
     * <p>
     * 基于Redis的Redisson的分布式信号量（Semaphore）Java对象RSemaphore采用了与java.util.concurrent.Semaphore相似的接口和用法。
     * 同时还提供了异步（Async）、反射式（Reactive）和RxJava2标准的接口。
     *
     * @param key 键
     * @return RPermitExpirableSemaphore
     */
    RPermitExpirableSemaphore getPermitExpirableSemaphore(String key);

    /**
     * 闭锁
     *
     * @param key 键
     * @return RCountDownLatch
     */
    RCountDownLatch getCountDownLatch(String key);

    /**
     * 布隆过滤器
     *
     * @param key 键
     * @param <T> 存放对象
     * @return 返回结果
     */
    <T> RBloomFilter<T> getBloomFilter(String key);

    Boolean setNx(String key);

    Boolean setNx(String key, long expired, TimeUnit timeUnit);

    RBitSet getBitSet(String key);

    default int getIndexFromUserId(String userId) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(userId.getBytes(StandardCharsets.UTF_8));
            // 将哈希字节数组转换为正整数
            BigInteger bigInt = new BigInteger(1, hashBytes);
            // 取模以确保索引在合理范围内
            return bigInt.mod(BigInteger.valueOf(Integer.MAX_VALUE)).intValue();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    /**
     * 设置字符串值(带过期时间)
     *
     * @param key      键
     * @param value    值
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     */
    void setString(String key, String value, long timeout, TimeUnit timeUnit);

    /**
     * 获取字符串值
     *
     * @param key 键
     * @return 值
     */
    String getString(String key);

    /**
     * 批量获取字符串值
     *
     * @param keys 键列表
     * @return 值列表
     */
    java.util.List<String> multiGetString(java.util.Collection<String> keys);

    /**
     * 获取匹配模式的所有键
     *
     * @param pattern 模式 (如 "user:*")
     * @return 键集合
     */
    java.util.Set<String> keys(String pattern);

    /**
     * 删除多个键
     *
     * @param keys 键集合
     * @return 删除的数量
     */
    Long delete(java.util.Collection<String> keys);

    /**
     * 删除单个键
     *
     * @param key 键
     * @return 是否删除成功
     */
    Boolean delete(String key);

    /**
     * 添加元素到集合
     *
     * @param key    键
     * @param values 值
     * @return 添加的数量
     */
    Long addToSet(String key, String... values);

    /**
     * 获取集合的所有成员
     *
     * @param key 键
     * @return 成员集合
     */
    java.util.Set<String> getSetMembers(String key);

    /**
     * 从集合中移除元素
     *
     * @param key    键
     * @param values 值
     * @return 移除的数量
     */
    Long removeFromSet(String key, String... values);

    /**
     * 发布消息到频道
     *
     * @param channel 频道
     * @param message 消息
     */
    void publish(String channel, String message);

    /**
     * 获取 RSet
     *
     * @param key 键
     * @param <T> 泛型
     * @return RSet
     */
    <T> RSet<T> getSet(String key);

}
