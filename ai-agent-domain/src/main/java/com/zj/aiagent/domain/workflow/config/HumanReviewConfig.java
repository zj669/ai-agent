package com.zj.aiagent.domain.workflow.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

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
     * 审核人用户ID列表
     */
    private String[] reviewerIds;

    /**
     * 审核超时时间（毫秒）
     */
    private Long timeoutMs;

    /**
     * 审核提示信息
     */
    private String prompt;

    /**
     * 可编辑字段
     */
    private String[] editableFields;
}
