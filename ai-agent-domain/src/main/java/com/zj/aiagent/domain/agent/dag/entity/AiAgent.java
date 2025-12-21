package com.zj.aiagent.domain.agent.dag.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAgent {
    /**
     * 主键ID
     */
    @Schema(description = "主键")
    private Long id;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 智能体ID
     */
    @Schema(description = "智能体ID")
    private String agentId;

    /**
     * 智能体名称
     */
    @Schema(description = "智能体名称")
    private String agentName;

    /**
     * 描述
     */
    @Schema(description = "描述")
    private String description;

    /**
     * 核心编排规则 (节点、连线、DSL配置)
     */
    @Schema(description = "核心编排规则")
    private String graphJson;

    /**
     * 状态 (0:草稿, 1:已发布, 2:已停用)
     */
    @Schema(description = "状态")
    private Integer status;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
