package com.zj.aiagent.domain.workflow.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.zj.aiagent.domain.workflow.valobj.TriggerPhase;

/**
 * 人工审核配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HumanReviewConfig {
    /**
     * 是否开启人工审核
     */
    private boolean enabled;

    /**
     * 审核提示信息
     */
    private String prompt;

    /**
     * 可编辑字段
     */
    private String[] editableFields;

    /**
     * 触发阶段
     */
    private TriggerPhase triggerPhase;
}
