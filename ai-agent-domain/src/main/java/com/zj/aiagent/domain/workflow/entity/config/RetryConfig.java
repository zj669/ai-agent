package com.zj.aiagent.domain.workflow.entity.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 重试配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryConfig {
    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 最大重试次数
     */
    private Integer maxRetries;

    /**
     * 重试退避时间（毫秒）
     */
    private Long backoffMs;

    /**
     * 重试条件列表
     * <p>
     * 例如: ["TIMEOUT", "SERVICE_ERROR"]
     * </p>
     */
    private List<String> retryOn;
}
