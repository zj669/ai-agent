package com.zj.aiagent.domain.agent.dag.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 人工介入配置
 * 允许在任意节点上设置人工介入点
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HumanInterventionConfig {

    /**
     * 是否启用人工介入
     */
    private boolean enabled;

    /**
     * 介入时机
     */
    private InterventionTiming timing;

    /**
     * 审核提示消息
     */
    private String checkMessage;

    /**
     * 是否允许修改节点输出
     */
    private Boolean allowModifyOutput;

    /**
     * 超时时间（毫秒），超时后自动通过
     * null 表示永久等待
     */
    private Long timeout;

    /**
     * 介入时机枚举
     */
    public enum InterventionTiming {
        /**
         * 节点执行前暂停
         */
        BEFORE,

        /**
         * 节点执行后暂停（默认）
         */
        AFTER
    }

    /**
     * 获取介入时机，默认为 AFTER
     */
    public InterventionTiming getTiming() {
        return timing != null ? timing : InterventionTiming.AFTER;
    }
}
