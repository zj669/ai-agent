package com.zj.aiagent.interfaces.web.dto.request.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天请求
 *
 * @author zj
 * @since 2025-12-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "聊天请求")
public class ChatRequest {

    @NotBlank(message = "Agent ID不能为空")
    @Schema(description = "Agent ID", required = true, example = "agent_123")
    private String agentId;

    @NotBlank(message = "用户消息不能为空")
    @Schema(description = "用户消息", required = true, example = "你好，请帮我分析一下这个问题")
    private String userMessage;

    @Schema(description = "会话ID", required = true, example = "conversation_456")
    private String conversationId;
}
