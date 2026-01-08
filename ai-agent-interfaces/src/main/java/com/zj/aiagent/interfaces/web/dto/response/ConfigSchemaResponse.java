package com.zj.aiagent.interfaces.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 配置模块 Schema 响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "配置模块Schema响应")
public class ConfigSchemaResponse {

    @Schema(description = "模块名称")
    private String module;

    @Schema(description = "显示名称")
    private String displayName;

    @Schema(description = "字段列表")
    private List<FieldDefinition> fields;

    /**
     * 字段定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "字段定义")
    public static class FieldDefinition {

        @Schema(description = "字段名")
        private String name;

        @Schema(description = "字段类型: number, string, boolean, select, array")
        private String type;

        @Schema(description = "显示标签")
        private String label;

        @Schema(description = "默认值")
        private Object defaultValue;

        @Schema(description = "是否必填")
        private Boolean required;

        @Schema(description = "最小值（number类型）")
        private Integer min;

        @Schema(description = "最大值（number类型）")
        private Integer max;

        @Schema(description = "选项列表（select/array类型）")
        private List<String> options;

        @Schema(description = "提示信息")
        private String placeholder;
    }
}
