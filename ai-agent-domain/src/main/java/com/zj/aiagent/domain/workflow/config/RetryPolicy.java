package com.zj.aiagent.domain.workflow.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 重试策略配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryPolicy {

    /**
     * 最大重试次数
     */
    @Builder.Default
    private int maxRetries = 3;

    /**
     * 重试间隔（毫秒）
     */
    @Builder.Default
    private long retryDelayMs = 1000;

    /**
     * 是否指数退避
     */
    @Builder.Default
    private boolean exponentialBackoff = true;

    /**
     * 退避乘数
     */
    @Builder.Default
    private double backoffMultiplier = 2.0;

    /**
     * 最大重试间隔（毫秒）
     */
    @Builder.Default
    private long maxRetryDelayMs = 30000;
}
