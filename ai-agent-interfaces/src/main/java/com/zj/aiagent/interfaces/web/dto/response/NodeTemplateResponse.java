package com.zj.aiagent.interfaces.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 节点模板响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "节点模板响应")
public class NodeTemplateResponse {

    @Schema(description = "模板ID")
    private String templateId;

    @Schema(description = "节点类型")
    private String nodeType;

    @Schema(description = "模板显示标签")
    private String templateLabel;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "前端展示类型")
    private String displayType;

    @Schema(description = "支持的配置模块")
    private List<String> supportedConfigs;

    @Schema(description = "是否内置")
    private Boolean isBuiltIn;

    @Schema(description = "是否废弃")
    private Boolean isDeprecated;
}
