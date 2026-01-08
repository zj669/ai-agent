package com.zj.aiagent.interfaces.web.dto.response.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 节点模板响应 DTO
 *
 * @author zj
 * @since 2025-12-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "节点模板信息")
public class NodeTemplateResponse {

    @Schema(description = "模板ID")
    private String templateId;

    @Schema(description = "节点类型")
    private String nodeType;

    @Schema(description = "节点名称")
    private String nodeName;

    @Schema(description = "模板显示标签")
    private String templateLabel;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "基础类型")
    private String baseType;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "系统提示词模板")
    private String systemPromptTemplate;

    @Schema(description = "输出Schema")
    private String outputSchema;

    @Schema(description = "可编辑字段列表")
    private String editableFields;

    @Schema(description = "是否系统内置")
    private Boolean isBuiltIn;
}
