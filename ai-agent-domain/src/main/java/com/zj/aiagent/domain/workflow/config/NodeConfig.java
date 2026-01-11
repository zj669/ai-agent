package com.zj.aiagent.domain.workflow.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 节点配置基类
 * 所有节点类型的共有配置
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class NodeConfig {

    /**
     * 人工审核配置
     */
    private HumanReviewConfig humanReviewConfig;

    /**
     * 重试策略
     */
    private RetryPolicy retryPolicy;

    /**
     * 超时时间（毫秒）
     */
    private Long timeoutMs;

    /**
     * 是否需要人工审核
     */
    public boolean requiresHumanReview() {
        return humanReviewConfig != null && humanReviewConfig.isEnabled();
    }
}
