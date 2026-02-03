package com.zj.aiagent.infrastructure.redis.service;

import com.zj.aiagent.infrastructure.redis.IRedisService;
import jakarta.annotation.Resource;
import org.redisson.api.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis 服务 - Redisson
 *
 */
@Service("redissonService")
public class RedissonService implements IRedisService {

    @Resource
    private RedissonClient redissonClient;

    public <T> void setValue(String key, T value) {
        redissonClient.<T>getBucket(key).set(value);
    }

    @Override
    public <T> void setValue(String key, T value, long expired) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(value, Duration.ofSeconds(expired));
    }

    public <T> T getValue(String key) {
        return redissonClient.<T>getBucket(key).get();
    }

    @Override
    public <T> RQueue<T> getQueue(String key) {
        return redissonClient.getQueue(key);
    }

    @Override
    public <T> RBlockingQueue<T> getBlockingQueue(String key) {
        return redissonClient.getBlockingQueue(key);
    }

    @Override
    public <T> RDelayedQueue<T> getDelayedQueue(RBlockingQueue<T> rBlockingQueue) {
        return redissonClient.getDelayedQueue(rBlockingQueue);
    }

    @Override
    public void setAtomicLong(String key, long value) {
        redissonClient.getAtomicLong(key).set(value);
    }

    @Override
    public Long getAtomicLong(String key) {
        return redissonClient.getAtomicLong(key).get();
    }

    @Override
    public long incr(String key) {
        return redissonClient.getAtomicLong(key).incrementAndGet();
    }

    @Override
    public long incrBy(String key, long delta) {
        return redissonClient.getAtomicLong(key).addAndGet(delta);
    }

    @Override
    public long decr(String key) {
        return redissonClient.getAtomicLong(key).decrementAndGet();
    }

    @Override
    public long decrBy(String key, long delta) {
        return redissonClient.getAtomicLong(key).addAndGet(-delta);
    }

    @Override
    public void remove(String key) {
        redissonClient.getBucket(key).delete();
    }

    @Override
    public boolean expire(String key, long seconds) {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
        return atomicLong.expire(Duration.ofSeconds(seconds));
    }

    @Override
    public boolean isExists(String key) {
        return redissonClient.getBucket(key).isExists();
    }

    public void addToSet(String key, String value) {
        RSet<String> set = redissonClient.getSet(key);
        set.add(value);
    }

    public boolean isSetMember(String key, String value) {
        RSet<String> set = redissonClient.getSet(key);
        return set.contains(value);
    }

    public void addToList(String key, String value) {
        RList<String> list = redissonClient.getList(key);
        list.add(value);
    }

    public String getFromList(String key, int index) {
        RList<String> list = redissonClient.getList(key);
        return list.get(index);
    }

    @Override
    public <K, V> RMap<K, V> getMap(String key) {
        return redissonClient.getMap(key);
    }

    public void addToMap(String key, String field, String value) {
        RMap<String, String> map = redissonClient.getMap(key);
        map.put(field, value);
    }

    public String getFromMap(String key, String field) {
        RMap<String, String> map = redissonClient.getMap(key);
        return map.get(field);
    }

    @Override
    public <K, V> V getFromMap(String key, K field) {
        return redissonClient.<K, V>getMap(key).get(field);
    }

    public void addToSortedSet(String key, String value) {
        RSortedSet<String> sortedSet = redissonClient.getSortedSet(key);
        sortedSet.add(value);
    }

    @Override
    public RLock getLock(String key) {
        return redissonClient.getLock(key);
    }

    @Override
    public RLock getFairLock(String key) {
        return redissonClient.getFairLock(key);
    }

    @Override
    public RReadWriteLock getReadWriteLock(String key) {
        return redissonClient.getReadWriteLock(key);
    }

    @Override
    public RSemaphore getSemaphore(String key) {
        return redissonClient.getSemaphore(key);
    }

    @Override
    public RPermitExpirableSemaphore getPermitExpirableSemaphore(String key) {
        return redissonClient.getPermitExpirableSemaphore(key);
    }

    @Override
    public RCountDownLatch getCountDownLatch(String key) {
        return redissonClient.getCountDownLatch(key);
    }

    @Override
    public <T> RBloomFilter<T> getBloomFilter(String key) {
        return redissonClient.getBloomFilter(key);
    }

    @Override
    public Boolean setNx(String key) {
        return redissonClient.getBucket(key).trySet("lock");
    }

    @Override
    public Boolean setNx(String key, long expired, TimeUnit timeUnit) {
        return redissonClient.getBucket(key).trySet("lock", expired, timeUnit);
    }

    @Override
    public RBitSet getBitSet(String key) {
        return redissonClient.getBitSet(key);
    }

    @Override
    public void setString(String key, String value, long timeout, TimeUnit timeUnit) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.set(value, timeout, timeUnit);
    }

    @Override
    public String getString(String key) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    @Override
    public java.util.List<String> multiGetString(java.util.Collection<String> keys) {
        RBuckets buckets = redissonClient.getBuckets();
        java.util.Map<String, String> result = buckets.get(keys.toArray(new String[0]));
        return new java.util.ArrayList<>(result.values());
    }

    @Override
    public java.util.Set<String> keys(String pattern) {
        Iterable<String> keysIterable = redissonClient.getKeys().getKeysByPattern(pattern);
        java.util.Set<String> result = new java.util.HashSet<>();
        keysIterable.forEach(result::add);
        return result;
    }

    @Override
    public Long delete(java.util.Collection<String> keys) {
        return redissonClient.getKeys().delete(keys.toArray(new String[0]));
    }

    @Override
    public Boolean delete(String key) {
        return redissonClient.getKeys().delete(key) > 0;
    }

    @Override
    public Long addToSet(String key, String... values) {
        RSet<String> set = redissonClient.getSet(key);
        boolean added = set.addAll(java.util.Arrays.asList(values));
        return added ? (long) values.length : 0L;
    }

    @Override
    public java.util.Set<String> getSetMembers(String key) {
        RSet<String> set = redissonClient.getSet(key);
        return new java.util.HashSet<>(set.readAll());
    }

    @Override
    public Long removeFromSet(String key, String... values) {
        RSet<String> set = redissonClient.getSet(key);
        boolean removed = set.removeAll(java.util.Arrays.asList(values));
        return removed ? (long) values.length : 0L;
    }

    @Override
    public void publish(String channel, String message) {
        RTopic topic = redissonClient.getTopic(channel);
        topic.publish(message);
    }

    @Override
    public <T> RSet<T> getSet(String key) {
        return redissonClient.getSet(key);
    }

}
