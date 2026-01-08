package com.zj.aiagent.domain.workflow.entity.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 兜底配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FallbackConfig {
    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 兜底策略
     * <ul>
     * <li>DEFAULT_RESPONSE: 返回默认响应</li>
     * <li>ALTERNATIVE_NODE: 切换到备用节点</li>
     * </ul>
     */
    private String strategy;

    /**
     * 默认消息（当 strategy = DEFAULT_RESPONSE 时使用）
     */
    private String defaultMessage;

    /**
     * 备用节点 ID（当 strategy = ALTERNATIVE_NODE 时使用）
     */
    private String alternativeNodeId;
}
