package com.zj.aiagent.infrastructure.mcp.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SseMcpTransport 单元测试
 * <p>
 * 测试 SSE 行解析逻辑的各种边界场景
 */
class SseMcpTransportTest {

    private SseMcpTransport transport;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        transport = new SseMcpTransport(objectMapper);
    }

    @Nested
    @DisplayName("标准 SSE 流解析")
    class StandardSseStream {

        @Test
        @DisplayName("带 event: 指令和 data: 行")
        void shouldParseWithEventDirective() {
            String sse = """
                    event: message
                    data: {"jsonrpc":"2.0","id":"1","result":{"tools":[{"name":"tool1","description":"desc1","inputSchema":"{}"}]}}
                    """;
            String result = transport.parseSseContent(sse);
            assertThat(result).contains("\"jsonrpc\":\"2.0\"");
            assertThat(result).contains("\"name\":\"tool1\"");
        }

        @Test
        @DisplayName("紧凑格式 data:{...} (无空格)")
        void shouldParseCompactFormat() {
            String sse = "data:{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"content\":[{\"text\":\"ok\"}]}}";
            String result = transport.parseSseContent(sse);
            assertThat(result).contains("\"jsonrpc\":\"2.0\"");
            assertThat(result).contains("\"text\":\"ok\"");
        }

        @Test
        @DisplayName("标准格式 data: {...} (有空格)")
        void shouldParseStandardFormat() {
            String sse = "data: {\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"content\":[{\"text\":\"ok\"}]}}";
            String result = transport.parseSseContent(sse);
            assertThat(result).contains("\"jsonrpc\":\"2.0\"");
            assertThat(result).contains("\"text\":\"ok\"");
        }
    }

    @Nested
    @DisplayName("多行 data: 分块传输")
    class MultiLineData {

        @Test
        @DisplayName("JSON 跨多个 data: 行传输")
        void shouldParseMultiLineData() {
            String sse = """
                    data: {"jsonrpc":"2.0",
                    data: "id":"1",
                    data: "result":{"tools":[]}}
                    """;
            String result = transport.parseSseContent(sse);
            assertThat(result).contains("\"jsonrpc\":\"2.0\"");
            assertThat(result).contains("\"id\":\"1\"");
            assertThat(result).contains("\"result\"");
        }

        @Test
        @DisplayName("多行 JSON 跨多条消息")
        void shouldParseMultiMessageJson() {
            String sse = """
                    event: message
                    data: {"jsonrpc":"2.0","id":"1","result":
                    data: {"content":[{"text":"hello"}]}}
                    """;
            String result = transport.parseSseContent(sse);
            assertThat(result).contains("\"jsonrpc\":\"2.0\"");
            assertThat(result).contains("\"text\":\"hello\"");
        }
    }

    @Nested
    @DisplayName("空行和注释处理")
    class EmptyAndCommentLines {

        @Test
        @DisplayName("包含空行的 SSE 流")
        void shouldSkipEmptyLines() {
            String sse = """
                    event: message

                    data: {"jsonrpc":"2.0","id":"1","result":{"tools":[]}}

                    """;
            String result = transport.parseSseContent(sse);
            assertThat(result).contains("\"jsonrpc\":\"2.0\"");
        }

        @Test
        @DisplayName("包含注释行的 SSE 流")
        void shouldSkipCommentLines() {
            String sse = """
                    : This is a comment
                    data: {"jsonrpc":"2.0","id":"1","result":{}}
                    """;
            String result = transport.parseSseContent(sse);
            assertThat(result).contains("\"jsonrpc\":\"2.0\"");
        }

        @Test
        @DisplayName("注释行和空行混合")
        void shouldHandleMixedCommentsAndEmptyLines() {
            String sse = """
                    :comment 1

                    event: message

                    data: {"jsonrpc":"2.0","id":"1","result":{}}
                    """;
            String result = transport.parseSseContent(sse);
            assertThat(result).contains("\"jsonrpc\":\"2.0\"");
        }
    }

    @Nested
    @DisplayName("流结束边界")
    class StreamEndBoundary {

        @Test
        @DisplayName("流结束时未解析的 JSON 片段")
        void shouldNotParseIncompleteJson() {
            // 流结束时缓冲区有内容但不完整（不是有效 JSON）
            String sse = "data: {\"partial\": \"con";
            String result = transport.parseSseContent(sse);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("流结束时缓冲区有完整 JSON")
        void shouldParseCompleteJsonAtStreamEnd() {
            String sse = "data:{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"tools\":[]}}";
            String result = transport.parseSseContent(sse);
            assertThat(result).contains("\"jsonrpc\":\"2.0\"");
        }

        @Test
        @DisplayName("空流")
        void shouldReturnEmptyForBlankStream() {
            assertThat(transport.parseSseContent("")).isEmpty();
            assertThat(transport.parseSseContent(null)).isEmpty();
            assertThat(transport.parseSseContent("   ")).isEmpty();
        }
    }

    @Nested
    @DisplayName("多条消息序列")
    class MultipleMessages {

        @Test
        @DisplayName("连续多条消息只返回第一条")
        void shouldReturnFirstCompleteMessage() {
            String sse = """
                    event: message
                    data: {"jsonrpc":"2.0","id":"1","result":{"tools":[{"name":"tool1"}]}}

                    event: message
                    data: {"jsonrpc":"2.0","id":"2","result":{"tools":[{"name":"tool2"}]}}
                    """;
            String result = transport.parseSseContent(sse);
            assertThat(result).contains("\"id\":\"1\"");
            assertThat(result).contains("\"name\":\"tool1\"");
            // 不应包含第二条消息
            assertThat(result).doesNotContain("\"name\":\"tool2\"");
        }

        @Test
        @DisplayName("中间消息不完整时跳到下一条")
        void shouldSkipIncompleteMessageAndParseNext() {
            String sse = """
                    event: message
                    data: {"incomplete

                    event: message
                    data: {"jsonrpc":"2.0","id":"2","result":{"tools":[]}}
                    """;
            String result = transport.parseSseContent(sse);
            assertThat(result).contains("\"id\":\"2\"");
        }
    }

    @Nested
    @DisplayName("数组格式响应")
    class ArrayFormat {

        @Test
        @DisplayName("JSON 数组格式响应")
        void shouldParseJsonArrayFormat() {
            String sse = "data: [{\"name\":\"tool1\",\"description\":\"desc\"},{\"name\":\"tool2\"}]";
            String result = transport.parseSseContent(sse);
            assertThat(result).contains("\"name\":\"tool1\"");
            assertThat(result).contains("\"name\":\"tool2\"");
        }
    }

    @Nested
    @DisplayName("CRLF 兼容性")
    class CrlfCompatibility {

        @Test
        @DisplayName("Windows 格式 CRLF 行尾")
        void shouldHandleCrlfLineEndings() {
            String sse = "data: {\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{}}\r\n";
            String result = transport.parseSseContent(sse);
            assertThat(result).contains("\"jsonrpc\":\"2.0\"");
        }

        @Test
        @DisplayName("CRLF 混合 LF")
        void shouldHandleMixedCrlfAndLf() {
            String sse = "data: {\"jsonrpc\":\"2.0\",\"id\":\"1\"}\r\ndata: {\"result\":{}}\n";
            String result = transport.parseSseContent(sse);
            assertThat(result).contains("\"jsonrpc\":\"2.0\"");
        }
    }
}
