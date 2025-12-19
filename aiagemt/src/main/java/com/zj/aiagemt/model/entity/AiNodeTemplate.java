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
@TableName("ai_node_template")
public class AiNodeTemplate {

    /**
     * 主键ID
     */
    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 节点类型 (PLAN, ACT, REFLECT, HUMAN, ROUTER, END)
     */
    @Schema(description = "节点类型")
    private String nodeType;

    /**
     * 节点展示名称
     */
    @Schema(description = "节点展示名称")
    private String nodeName;

    /**
     * 前端图标标识
     */
    @Schema(description = "前端图标标识")
    private String icon;

    /**
     * 默认系统提示词 (用于初始化)
     */
    @Schema(description = "默认系统提示词")
    private String defaultSystemPrompt;

    /**
     * 前端表单配置Schema (JSON格式)
     */
    @Schema(description = "前端表单配置Schema")
    private String configSchema;

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
