package com.zj.aiagent.infrastructure.swarm.llm;

import lombok.Builder;
import lombok.Data;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.List;

@Data
@Builder
public class SwarmLlmResponse {
    private String content;
    private List<AssistantMessage.ToolCall> toolCalls;

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}
