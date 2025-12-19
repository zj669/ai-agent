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
@TableName("ai_agent_version")
public class AiAgentVersion {

    /**
     * 主键ID
     */
    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的工作流ID
     */
    @Schema(description = "关联的工作流ID")
    private Long agentId;

    /**
     * 版本号 (如: v1.0.1)
     */
    @Schema(description = "版本号")
    private String version;

    /**
     * 核心编排规则 (节点、连线、DSL配置)
     */
    @Schema(description = "核心编排规则")
    private String graphJson;

    /**
     * 状态 (0:草稿, 1:已发布, 2:历史版本)
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
