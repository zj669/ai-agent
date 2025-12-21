package com.zj.aiagent.interfaces.web.dto.response.agent.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型响应 DTO
 *
 * @author zj
 * @since 2025-12-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "模型信息")
public class ModelResponse {

    @Schema(description = "数据库主键")
    private Long id;

    @Schema(description = "模型ID")
    private String modelId;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "模型类型")
    private String modelType;

    @Schema(description = "关联的API ID")
    private String apiId;

    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
}
