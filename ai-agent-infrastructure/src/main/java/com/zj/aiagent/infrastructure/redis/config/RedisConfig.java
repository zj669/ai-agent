package com.zj.aiagent.infrastructure.redis.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis配置类 - 基础设施层
 * 
 * <p>
 * 使用Redisson客户端连接Redis服务器
 * <p>
 * Redisson文档: <a href="https://github.com/redisson/redisson">Redisson
 * GitHub</a>
 * 
 * @author zj
 * @since 2025-12-21
 */
@Configuration
@EnableConfigurationProperties(RedisProperties.class)
public class RedisConfig {

    /**
     * 创建Redisson客户端Bean
     * 
     * @param properties Redis配置属性
     * @return RedissonClient实例
     */
    @Bean("redissonClient")
    public RedissonClient redissonClient(RedisProperties properties) {
        Config config = new Config();

        // 设置JSON序列化编解码器
        // 文档:
        // https://github.com/redisson/redisson/wiki/4.-%E6%95%B0%E6%8D%AE%E5%BA%8F%E5%88%97%E5%8C%96
        config.setCodec(JsonJacksonCodec.INSTANCE);

        // 配置单机模式
        config.useSingleServer()
                .setAddress("redis://" + properties.getHost() + ":" + properties.getPort())
                .setPassword(properties.getPassword())
                .setConnectionPoolSize(properties.getPoolSize())
                .setConnectionMinimumIdleSize(properties.getMinIdleSize())
                .setIdleConnectionTimeout(properties.getIdleTimeout())
                .setConnectTimeout(properties.getConnectTimeout())
                .setRetryAttempts(properties.getRetryAttempts())
                .setRetryInterval(properties.getRetryInterval())
                .setPingConnectionInterval(properties.getPingInterval())
                .setKeepAlive(properties.isKeepAlive());

        return Redisson.create(config);
    }
}
