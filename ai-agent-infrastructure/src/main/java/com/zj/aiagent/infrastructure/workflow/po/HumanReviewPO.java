package com.zj.aiagent.infrastructure.workflow.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zj.aiagent.domain.workflow.valobj.TriggerPhase;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 人工审核记录持久化对象
 */
@Data
@TableName("workflow_human_review_record")
public class HumanReviewPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String executionId;
    private String nodeId;
    private Long reviewerId;
    private String decision;
    /**
     * 触发阶段
     */
    private TriggerPhase triggerPhase;
    private String originalData;
    private String modifiedData;
    private String comment;
    private LocalDateTime reviewedAt;
}
