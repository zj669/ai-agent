package com.zj.aiagent.domain.workflow.entity.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 人工介入配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HumanInterventionConfig {
    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 介入时机
     * <ul>
     * <li>BEFORE: 节点执行前</li>
     * <li>AFTER: 节点执行后</li>
     * </ul>
     */
    private String timing;
}
