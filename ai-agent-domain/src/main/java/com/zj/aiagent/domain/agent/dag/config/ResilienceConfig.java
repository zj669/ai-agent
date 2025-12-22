package com.zj.aiagent.domain.agent.dag.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 弹性配置 - 超时、重试、限流、降级等参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResilienceConfig {

    // ==================== 超时配置 ====================
    /** 执行超时时间（毫秒），默认60秒 */
    @Builder.Default
    private Long timeoutMs = 60000L;

    // ==================== 重试配置 ====================
    /** 最大重试次数，默认3次 */
    @Builder.Default
    private Integer maxRetries = 3;

    /** 初始重试延迟（毫秒），默认1秒 */
    @Builder.Default
    private Long retryDelayMs = 1000L;

    /** 指数退避倍数，默认2.0 */
    @Builder.Default
    private Double retryMultiplier = 2.0;

    /** 最大重试延迟（毫秒），默认30秒 */
    @Builder.Default
    private Long maxRetryDelayMs = 30000L;

    // ==================== 限流配置 ====================
    /** 最大并发数，默认10 */
    @Builder.Default
    private Integer maxConcurrent = 10;

    /** 每分钟最大请求数，默认60 */
    @Builder.Default
    private Integer maxRequestsPerMinute = 60;

    // ==================== 降级配置 ====================
    /** 是否启用模型降级，默认启用 */
    @Builder.Default
    private Boolean enableFallback = true;

    /**
     * 计算第n次重试的延迟时间（指数退避）
     */
    public long calculateRetryDelay(int attempt) {
        if (attempt <= 0) {
            return retryDelayMs;
        }
        long delay = (long) (retryDelayMs * Math.pow(retryMultiplier, attempt - 1));
        return Math.min(delay, maxRetryDelayMs);
    }

    /**
     * 创建默认配置
     */
    public static ResilienceConfig defaultConfig() {
        return ResilienceConfig.builder().build();
    }

    /**
     * 创建快速失败配置（不重试）
     */
    public static ResilienceConfig failFast() {
        return ResilienceConfig.builder()
                .maxRetries(0)
                .enableFallback(false)
                .build();
    }
}
