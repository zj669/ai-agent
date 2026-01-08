package com.zj.aiagent.domain.workflow.entity.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 限流配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitConfig {
    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 每分钟最大请求数
     */
    private Integer maxRequestsPerMinute;
}
