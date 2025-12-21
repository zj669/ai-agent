package com.zj.aiagent.infrastructure.redis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis配置属性 - 基础设施层
 * 
 * <p>
 * 从配置文件读取Redis连接相关参数
 * 
 * @author zj
 * @since 2025-12-21
 */
@Data
@ConfigurationProperties(prefix = "redis.sdk.config", ignoreInvalidFields = true)
public class RedisProperties {

    /** Redis服务器地址 */
    private String host;

    /** Redis服务器端口 */
    private int port;

    /** Redis密码 */
    private String password;

    /** 连接池大小，默认64 */
    private int poolSize = 64;

    /** 连接池最小空闲连接数，默认10 */
    private int minIdleSize = 10;

    /** 连接最大空闲时间（毫秒），默认10000 */
    private int idleTimeout = 10000;

    /** 连接超时时间（毫秒），默认10000 */
    private int connectTimeout = 10000;

    /** 连接重试次数，默认3 */
    private int retryAttempts = 3;

    /** 连接重试间隔（毫秒），默认1000 */
    private int retryInterval = 1000;

    /** 定期检查连接间隔（毫秒），默认0表示不检查 */
    private int pingInterval = 0;

    /** 是否保持长连接，默认true */
    private boolean keepAlive = true;
}
