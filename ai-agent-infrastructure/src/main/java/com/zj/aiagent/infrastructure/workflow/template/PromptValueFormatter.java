package com.zj.aiagent.infrastructure.workflow.template;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 统一处理 Prompt 模板值到字符串的序列化逻辑。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptValueFormatter {

    private final ObjectMapper objectMapper;

    public String format(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }

        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("[PromptValueFormatter] Failed to serialize value, fallback to toString: {}", e.getMessage());
            return value.toString();
        }
    }
}
