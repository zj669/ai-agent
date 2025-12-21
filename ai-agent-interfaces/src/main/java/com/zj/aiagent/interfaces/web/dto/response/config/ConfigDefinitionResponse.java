package com.zj.aiagent.interfaces.web.dto.response.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 配置项定义响应
 *
 * @author zj
 * @since 2025-12-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "配置项定义响应")
public class ConfigDefinitionResponse {

    @Schema(description = "配置项类型", example = "MODEL")
    private String configType;

    @Schema(description = "配置项名称", example = "模型配置")
    private String configName;

    @Schema(description = "可选值列表")
    private List<ConfigOption> options;

    /**
     * 配置选项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigOption {
        @Schema(description = "选项ID", example = "model_123")
        private String id;

        @Schema(description = "选项名称", example = "GPT-4")
        private String name;

        @Schema(description = "选项类型", example = "openai")
        private String type;

        @Schema(description = "扩展属性")
        private Map<String, Object> extra;
    }
}
