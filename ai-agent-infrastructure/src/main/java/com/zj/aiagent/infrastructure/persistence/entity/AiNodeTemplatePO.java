package com.zj.aiagent.infrastructure.persistence.entity;

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
public class AiNodeTemplatePO {

    /**
     * 主键ID
     */
    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模板唯一ID
     */
    @Schema(description = "模板唯一ID")
    private String templateId;

    /**
     * 节点类型 (PLAN_NODE, ACT_NODE, SUMMARY_NODE, ROUTER_NODE)
     */
    @Schema(description = "节点类型")
    private String nodeType;

    /**
     * 节点展示名称（如 PlanNode）
     */
    @Schema(description = "节点展示名称")
    private String nodeName;

    /**
     * 模板显示标签（中文，如"规划节点"）
     */
    @Schema(description = "模板显示标签")
    private String templateLabel;

    /**
     * 节点描述
     */
    @Schema(description = "节点描述")
    private String description;

    /**
     * 基础节点类型 (LLM_NODE, TOOL_NODE, ROUTER_NODE)
     */
    @Schema(description = "基础节点类型")
    private String baseType;

    /**
     * 前端图标标识
     */
    @Schema(description = "前端图标标识")
    private String icon;

    /**
     * System Prompt 模板（支持 {state.fieldName} 变量占位符）
     */
    @Schema(description = "System Prompt 模板")
    private String systemPromptTemplate;

    /**
     * 输出 Schema 定义 (JSON 格式)
     */
    @Schema(description = "输出 Schema 定义")
    private String outputSchema;

    /**
     * 用户可编辑字段列表 (JSON 数组格式)
     */
    @Schema(description = "用户可编辑字段列表")
    private String editableFields;

    /**
     * 是否系统内置模板
     */
    @Schema(description = "是否系统内置模板")
    private Boolean isBuiltIn;

    /**
     * 是否已废弃
     */
    @Schema(description = "是否已废弃")
    private Boolean isDeprecated;

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
