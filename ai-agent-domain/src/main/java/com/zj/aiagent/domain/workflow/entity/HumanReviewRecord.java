package com.zj.aiagent.domain.workflow.entity;

import com.zj.aiagent.domain.workflow.valobj.TriggerPhase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 人工审核记录实体
 * 不可变对象，作为审计日志
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HumanReviewRecord {
    /**
     * 记录ID
     */
    private String id;

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 触发审核的节点ID
     */
    private String nodeId;

    /**
     * 审核人ID
     */
    private Long reviewerId;

    /**
     * 审核决策
     * APPROVE, REJECT
     */
    private String decision;

    /**
     * 触发阶段
     */
    private TriggerPhase triggerPhase;

    /**
     * 原始数据快照 (JSON)
     */
    private String originalData;

    /**
     * 修改后的数据 (JSON)
     */
    private String modifiedData;

    /**
     * 审核意见
     */
    private String comment;

    /**
     * 审核时间
     */
    private LocalDateTime reviewedAt;
}
