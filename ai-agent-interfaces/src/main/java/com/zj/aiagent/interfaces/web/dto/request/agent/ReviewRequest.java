package com.zj.aiagent.interfaces.web.dto.request.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 人工介入审核请求
 *
 * @author zj
 * @since 2025-12-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "人工介入审核请求")
public class ReviewRequest {

    @NotBlank(message = "会话ID不能为空")
    @Schema(description = "会话ID", required = true)
    private String conversationId;

    @NotBlank(message = "节点ID不能为空")
    @Schema(description = "节点ID", required = true)
    private String nodeId;

    @NotNull(message = "审核结果不能为空")
    @Schema(description = "是否批准", required = true)
    private Boolean approved;
}
