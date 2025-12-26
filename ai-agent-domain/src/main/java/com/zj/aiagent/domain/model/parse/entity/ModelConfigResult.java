package com.zj.aiagent.domain.model.parse.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.openai.OpenAiChatModel;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ModelConfigResult {
    private OpenAiChatModel chatModel;
}
