package com.zj.aiagemt.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("ai_workflow_instance")
public class AiWorkflowInstance {

    /**
     * 主键ID
     */
    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的agentID
     */
    @Schema(description = "关联的agentID")
    private Long agentId;

    /**
     * 关联的工作流版本ID
     */
    @Schema(description = "关联的工作流版本ID")
    private Long versionId;

    /**
     * 会话/任务ID
     */
    @Schema(description = "会话/任务ID")
    private String conversationId;

    /**
     * 当前停留的节点ID
     */
    @Schema(description = "当前停留的节点ID")
    private String currentNodeId;

    /**
     * 运行状态 (RUNNING, PAUSED, COMPLETED, FAILED)
     */
    @Schema(description = "运行状态")
    private String status;

    /**
     * 运行时上下文快照 (变量、Memory、历史)
     */
    @Schema(description = "运行时上下文快照")
    private String runtimeContextJson;

    /**
     * 开始时间
     */
    @Schema(description = "开始时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
