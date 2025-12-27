package com.zj.aiagent.interfaces.web.dto.response.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 配置字段定义响应 DTO
 *
 * @author zj
 * @since 2025-12-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "配置字段定义")
public class ConfigFieldResponse {

    @Schema(description = "字段名称")
    private String fieldName;

    @Schema(description = "字段标签")
    private String fieldLabel;

    @Schema(description = "字段类型")
    private String fieldType;

    @Schema(description = "是否必填")
    private Boolean required;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "默认值")
    private String defaultValue;

    @Schema(description = "可选项")
    private String options;

    @Schema(description = "排序顺序")
    private Integer sortOrder;
}
