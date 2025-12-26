package com.zj.aiagent.application.chat.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatCommand {
    private String agentId;

    private String userMessage;

    private String conversationId;

    private ResponseBodyEmitter emitter;
}
