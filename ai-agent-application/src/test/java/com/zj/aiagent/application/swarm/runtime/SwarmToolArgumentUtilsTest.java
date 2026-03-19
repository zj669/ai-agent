package com.zj.aiagent.application.swarm.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Swarm 工具参数解析测试")
class SwarmToolArgumentUtilsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("支持解析 send 的 camelCase 参数")
    void shouldParseSendArgsWithCamelCase() {
        JsonNode args = SwarmToolArgumentUtils.parseArgumentsObject(
                "send",
                "{\"agentId\":27,\"message\":\"请继续处理\"}",
                objectMapper
        );

        assertThat(SwarmToolArgumentUtils.getRequiredLong(args, "agentId", "agent_id")).isEqualTo(27L);
        assertThat(SwarmToolArgumentUtils.getRequiredText(args, "message")).isEqualTo("请继续处理");
    }

    @Test
    @DisplayName("兼容解析 send 的 snake_case 参数")
    void shouldParseSendArgsWithSnakeCase() {
        JsonNode args = SwarmToolArgumentUtils.parseArgumentsObject(
                "send",
                "{\"agent_id\":28,\"message\":\"回报结果\"}",
                objectMapper
        );

        assertThat(SwarmToolArgumentUtils.getRequiredLong(args, "agentId", "agent_id")).isEqualTo(28L);
    }

    @Test
    @DisplayName("遇到自然语言参数时返回清晰错误")
    void shouldRejectNaturalLanguageArguments() {
        assertThatThrownBy(() -> SwarmToolArgumentUtils.parseArgumentsObject(
                "send",
                "？请帮我转告协调者",
                objectMapper
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("工具 send 的参数必须是 JSON 对象");
    }

    @Test
    @DisplayName("落库展示时保留非法参数原文，避免再生成非法 JSON")
    void shouldNormalizeInvalidArgumentsForStorage() {
        Object normalized = SwarmToolArgumentUtils.normalizeForStorage("？", objectMapper);

        assertThat(normalized).isEqualTo("？");
    }
}
