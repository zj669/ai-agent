package com.zj.aiagent.infrastructure.swarm.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.infrastructure.config.LlmDefaultConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.Consumer;

/**
 * 蜂群 LLM 调用器
 * MVP 阶段从 yml 读配置（LlmDefaultConfig），P3.5 做完后切换到数据库配置
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SwarmLlmCaller {

    private final LlmDefaultConfig llmDefaultConfig;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    /**
     * 流式调用 LLM，支持 tool_calls
     *
     * @param messages     消息列表（system + user + assistant 交替）
     * @param toolSchemas  工具 JSON Schema 列表（OpenAI function calling 格式）
     * @param onChunk      每个 chunk 的回调
     * @return 完整的 assistant 回复（拼接后的）
     */
    public SwarmLlmResponse callStream(
            List<Message> messages,
            List<OpenAiApi.FunctionTool> toolSchemas,
            Consumer<String> onChunk) {

        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(llmDefaultConfig.getBaseUrl())
                .apiKey(llmDefaultConfig.getApiKey())
                .restClientBuilder(restClientBuilder)
                .build();

        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(llmDefaultConfig.getModel());

        if (toolSchemas != null && !toolSchemas.isEmpty()) {
            optionsBuilder.tools(toolSchemas);
            optionsBuilder.internalToolExecutionEnabled(false);  // 禁用自动执行，由 Runner 手动处理
        }

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(optionsBuilder.build())
                .build();

        Prompt prompt = new Prompt(messages);

        // 非流式调用（简化 MVP，tool_calls 需要完整响应）
        ChatResponse response = chatModel.call(prompt);

        if (response.getResult() == null || response.getResult().getOutput() == null) {
            return SwarmLlmResponse.builder().content("").build();
        }

        AssistantMessage output = response.getResult().getOutput();
        String content = output.getText() != null ? output.getText() : "";

        if (onChunk != null && !content.isEmpty()) {
            onChunk.accept(content);
        }

        // 检查 tool_calls
        List<AssistantMessage.ToolCall> toolCalls = output.getToolCalls();

        return SwarmLlmResponse.builder()
                .content(content)
                .toolCalls(toolCalls)
                .build();
    }

    /**
     * 构建 Spring AI Message 列表
     */
    public static Message systemMessage(String content) {
        return new SystemMessage(content);
    }

    public static Message userMessage(String content) {
        return new UserMessage(content);
    }

    public static Message assistantMessage(String content) {
        return new AssistantMessage(content);
    }
}
