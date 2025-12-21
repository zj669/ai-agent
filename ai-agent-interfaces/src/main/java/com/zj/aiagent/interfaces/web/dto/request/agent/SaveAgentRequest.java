package com.zj.aiagent.interfaces.web.dto.request.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 保存Agent配置请求
 *
 * @author zj
 * @since 2025-12-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "保存Agent配置请求")
public class SaveAgentRequest {

    @Schema(description = "Agent ID（可选，不传则创建新Agent）", example = "agent_123456")
    private String agentId;

    @NotBlank(message = "Agent名称不能为空")
    @Schema(description = "Agent名称", required = true, example = "我的智能助手")
    private String agentName;

    @Schema(description = "描述", example = "这是一个帮助用户完成任务的智能助手")
    private String description;

    @NotBlank(message = "DAG配置不能为空")
    @Schema(description = "DAG配置JSON", required = true)
    private String graphJson;

    @Schema(description = "状态（0:草稿, 1:已发布, 2:已停用）", example = "0")
    private Integer status;
}
