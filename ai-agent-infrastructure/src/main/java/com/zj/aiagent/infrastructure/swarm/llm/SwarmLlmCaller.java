package com.zj.aiagent.infrastructure.swarm.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.llm.entity.LlmProviderConfig;
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
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

/**
 * 蜂群 LLM 调用器
 * 通过 workspace 关联的 LlmProviderConfig 获取模型配置
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SwarmLlmCaller {

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    /**
     * 根据 LlmProviderConfig 构建 OpenAiApi
     */
    private OpenAiApi buildApi(LlmProviderConfig config) {
        if (config == null) {
            throw new IllegalStateException("Workspace 未配置 LLM 模型，请在创建 Workspace 时选择模型配置");
        }
        return OpenAiApi.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .restClientBuilder(restClientBuilder)
                .build();
    }

    private String resolveModel(LlmProviderConfig config) {
        if (config == null) {
            throw new IllegalStateException("Workspace 未配置 LLM 模型，请在创建 Workspace 时选择模型配置");
        }
        return config.getModel();
    }

    /**
     * 流式调用 LLM，支持 tool_calls
     *
     * @param messages     消息列表（system + user + assistant 交替）
     * @param toolSchemas  工具 JSON Schema 列表（OpenAI function calling 格式）
     * @param onChunk      每个 chunk 的回调
     * @param llmConfig    LLM 配置
     * @return 完整的 assistant 回复（拼接后的）
     */
    public SwarmLlmResponse callStream(
            List<Message> messages,
            List<OpenAiApi.FunctionTool> toolSchemas,
            Consumer<String> onChunk,
            LlmProviderConfig llmConfig) {

        OpenAiApi api = buildApi(llmConfig);

        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(resolveModel(llmConfig));

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
     * 真正的流式调用 LLM，每个 chunk 实时回调
     *
     * @param messages     消息列表
     * @param toolSchemas  工具 JSON Schema 列表
     * @param onChunk      每个文本 chunk 的回调
     * @param llmConfig    LLM 配置（null 则使用默认）
     * @return 完整的 SwarmLlmResponse（拼接后的 content + 合并后的 tool_calls）
     */
    public SwarmLlmResponse callStreamReal(
            List<Message> messages,
            List<OpenAiApi.FunctionTool> toolSchemas,
            Consumer<String> onChunk,
            LlmProviderConfig llmConfig) {

        OpenAiApi api = buildApi(llmConfig);

        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(resolveModel(llmConfig));

        if (toolSchemas != null && !toolSchemas.isEmpty()) {
            optionsBuilder.tools(toolSchemas);
            optionsBuilder.internalToolExecutionEnabled(false);
        }

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(optionsBuilder.build())
                .build();

        Prompt prompt = new Prompt(messages);

        // 流式调用
        Flux<ChatResponse> flux = chatModel.stream(prompt);

        StringBuilder contentBuilder = new StringBuilder();
        // tool_calls 合并：index -> (id, name, argumentsBuilder)
        java.util.Map<Integer, String[]> toolCallIdMap = new java.util.concurrent.ConcurrentHashMap<>();
        java.util.Map<Integer, String[]> toolCallNameMap = new java.util.concurrent.ConcurrentHashMap<>();
        java.util.Map<Integer, StringBuilder> toolCallArgsMap = new java.util.concurrent.ConcurrentHashMap<>();

        flux.doOnNext(chatResponse -> {
            if (chatResponse.getResult() == null || chatResponse.getResult().getOutput() == null) return;

            AssistantMessage output = chatResponse.getResult().getOutput();

            // 收集文本 chunk
            String text = output.getText();
            if (text != null && !text.isEmpty()) {
                contentBuilder.append(text);
                if (onChunk != null) {
                    onChunk.accept(text);
                }
            }

            // 收集 tool_calls（流式时分散在多个 chunk 里）
            if (output.getToolCalls() != null) {
                for (int i = 0; i < output.getToolCalls().size(); i++) {
                    AssistantMessage.ToolCall tc = output.getToolCalls().get(i);
                    if (tc.id() != null && !tc.id().isEmpty()) {
                        toolCallIdMap.put(i, new String[]{tc.id()});
                    }
                    if (tc.name() != null && !tc.name().isEmpty()) {
                        toolCallNameMap.put(i, new String[]{tc.name()});
                    }
                    if (tc.arguments() != null && !tc.arguments().isEmpty()) {
                        toolCallArgsMap.computeIfAbsent(i, k -> new StringBuilder()).append(tc.arguments());
                    }
                }
            }
        }).blockLast(Duration.ofMinutes(5));

        // 合并 tool_calls
        List<AssistantMessage.ToolCall> mergedToolCalls = new java.util.ArrayList<>();
        for (Integer idx : toolCallIdMap.keySet().stream().sorted().toList()) {
            String id = toolCallIdMap.getOrDefault(idx, new String[]{""})[0];
            String name = toolCallNameMap.containsKey(idx) ? toolCallNameMap.get(idx)[0] : "";
            String args = toolCallArgsMap.containsKey(idx) ? toolCallArgsMap.get(idx).toString() : "";
            mergedToolCalls.add(new AssistantMessage.ToolCall(id, "function", name, args));
        }

        return SwarmLlmResponse.builder()
                .content(contentBuilder.toString())
                .toolCalls(mergedToolCalls.isEmpty() ? null : mergedToolCalls)
                .build();
    }

    /**
     * 使用 @Tool 注解的 ToolCallback 进行流式调用
     *
     * @param messages      消息列表
     * @param toolCallbacks @Tool 注解生成的 ToolCallback 数组
     * @param onChunk       每个文本 chunk 的回调
     * @param llmConfig     LLM 配置
     * @return 完整的 SwarmLlmResponse
     */
    public SwarmLlmResponse callStreamWithTools(
            List<Message> messages,
            ToolCallback[] toolCallbacks,
            Consumer<String> onChunk,
            LlmProviderConfig llmConfig) {

        OpenAiApi api = buildApi(llmConfig);

        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(resolveModel(llmConfig));

        if (toolCallbacks != null && toolCallbacks.length > 0) {
            optionsBuilder.toolCallbacks(toolCallbacks);
            optionsBuilder.internalToolExecutionEnabled(false);
        }

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(optionsBuilder.build())
                .build();

        Prompt prompt = new Prompt(messages);

        // 流式调用
        Flux<ChatResponse> flux = chatModel.stream(prompt);

        StringBuilder contentBuilder = new StringBuilder();
        java.util.Map<Integer, String[]> toolCallIdMap = new java.util.concurrent.ConcurrentHashMap<>();
        java.util.Map<Integer, String[]> toolCallNameMap = new java.util.concurrent.ConcurrentHashMap<>();
        java.util.Map<Integer, StringBuilder> toolCallArgsMap = new java.util.concurrent.ConcurrentHashMap<>();

        flux.doOnNext(chatResponse -> {
            if (chatResponse.getResult() == null || chatResponse.getResult().getOutput() == null) return;

            AssistantMessage output = chatResponse.getResult().getOutput();

            String text = output.getText();
            if (text != null && !text.isEmpty()) {
                contentBuilder.append(text);
                if (onChunk != null) {
                    onChunk.accept(text);
                }
            }

            if (output.getToolCalls() != null) {
                for (int i = 0; i < output.getToolCalls().size(); i++) {
                    AssistantMessage.ToolCall tc = output.getToolCalls().get(i);
                    if (tc.id() != null && !tc.id().isEmpty()) {
                        toolCallIdMap.put(i, new String[]{tc.id()});
                    }
                    if (tc.name() != null && !tc.name().isEmpty()) {
                        toolCallNameMap.put(i, new String[]{tc.name()});
                    }
                    if (tc.arguments() != null && !tc.arguments().isEmpty()) {
                        toolCallArgsMap.computeIfAbsent(i, k -> new StringBuilder()).append(tc.arguments());
                    }
                }
            }
        }).blockLast(Duration.ofMinutes(5));

        List<AssistantMessage.ToolCall> mergedToolCalls = new java.util.ArrayList<>();
        for (Integer idx : toolCallIdMap.keySet().stream().sorted().toList()) {
            String id = toolCallIdMap.getOrDefault(idx, new String[]{""})[0];
            String name = toolCallNameMap.containsKey(idx) ? toolCallNameMap.get(idx)[0] : "";
            String args = toolCallArgsMap.containsKey(idx) ? toolCallArgsMap.get(idx).toString() : "";
            mergedToolCalls.add(new AssistantMessage.ToolCall(id, "function", name, args));
        }

        return SwarmLlmResponse.builder()
                .content(contentBuilder.toString())
                .toolCalls(mergedToolCalls.isEmpty() ? null : mergedToolCalls)
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
