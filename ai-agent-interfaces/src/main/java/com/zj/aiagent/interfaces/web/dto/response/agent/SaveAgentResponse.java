package com.zj.aiagent.interfaces.web.dto.response.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 保存Agent配置响应
 *
 * @author zj
 * @since 2025-12-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "保存Agent配置响应")
public class SaveAgentResponse {

    @Schema(description = "Agent ID", example = "agent_123456")
    private String agentId;

    @Schema(description = "状态（0:草稿, 1:已发布, 2:已停用）", example = "0")
    private Integer status;

    @Schema(description = "提示信息", example = "保存成功")
    private String message;
}
