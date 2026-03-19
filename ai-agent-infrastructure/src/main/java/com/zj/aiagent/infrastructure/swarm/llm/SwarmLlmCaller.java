package com.zj.aiagent.infrastructure.swarm.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.llm.entity.LlmProviderConfig;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
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
            throw new IllegalStateException(
                "Workspace 未配置 LLM 模型，请在创建 Workspace 时选择模型配置"
            );
        }
        return OpenAiApi.builder()
            .baseUrl(config.getBaseUrl())
            .apiKey(config.getApiKey())
            .restClientBuilder(restClientBuilder)
            .build();
    }

    private String resolveModel(LlmProviderConfig config) {
        if (config == null) {
            throw new IllegalStateException(
                "Workspace 未配置 LLM 模型，请在创建 Workspace 时选择模型配置"
            );
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
        LlmProviderConfig llmConfig
    ) {
        OpenAiApi api = buildApi(llmConfig);

        OpenAiChatOptions.Builder optionsBuilder =
            OpenAiChatOptions.builder().model(resolveModel(llmConfig));

        if (toolSchemas != null && !toolSchemas.isEmpty()) {
            optionsBuilder.tools(toolSchemas);
            optionsBuilder.internalToolExecutionEnabled(false); // 禁用自动执行，由 Runner 手动处理
        }

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
            .openAiApi(api)
            .defaultOptions(optionsBuilder.build())
            .build();

        Prompt prompt = new Prompt(messages);

        // 非流式调用（简化 MVP，tool_calls 需要完整响应）
        ChatResponse response = chatModel.call(prompt);

        if (
            response.getResult() == null ||
            response.getResult().getOutput() == null
        ) {
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
        LlmProviderConfig llmConfig
    ) {
        OpenAiApi api = buildApi(llmConfig);

        OpenAiChatOptions.Builder optionsBuilder =
            OpenAiChatOptions.builder().model(resolveModel(llmConfig));

        if (toolSchemas != null && !toolSchemas.isEmpty()) {
            optionsBuilder.tools(toolSchemas);
            optionsBuilder.internalToolExecutionEnabled(false);
        }

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
            .openAiApi(api)
            .defaultOptions(optionsBuilder.build())
            .build();

        Prompt prompt = new Prompt(messages);

        return collectStreamingResponse(chatModel.stream(prompt), onChunk);
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
        LlmProviderConfig llmConfig
    ) {
        OpenAiApi api = buildApi(llmConfig);

        OpenAiChatOptions.Builder optionsBuilder =
            OpenAiChatOptions.builder().model(resolveModel(llmConfig));

        if (toolCallbacks != null && toolCallbacks.length > 0) {
            optionsBuilder.toolCallbacks(toolCallbacks);
            optionsBuilder.internalToolExecutionEnabled(false);
        }

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
            .openAiApi(api)
            .defaultOptions(optionsBuilder.build())
            .build();

        Prompt prompt = new Prompt(messages);

        return collectStreamingResponse(chatModel.stream(prompt), onChunk);
    }

    private SwarmLlmResponse collectStreamingResponse(
        Flux<ChatResponse> flux,
        Consumer<String> onChunk
    ) {
        StringBuilder contentBuilder = new StringBuilder();
        List<AssistantMessage.ToolCall> latestToolCallSnapshot =
            new ArrayList<>();

        flux
            .doOnNext(chatResponse -> {
                if (
                    chatResponse.getResult() == null ||
                    chatResponse.getResult().getOutput() == null
                ) {
                    return;
                }

                AssistantMessage output = chatResponse.getResult().getOutput();
                String text = output.getText();
                if (text != null && !text.isEmpty()) {
                    contentBuilder.append(text);
                    if (onChunk != null) {
                        onChunk.accept(text);
                    }
                }

                if (output.getToolCalls() == null) {
                    return;
                }

                if (
                    output.getToolCalls().size() >=
                    latestToolCallSnapshot.size()
                ) {
                    latestToolCallSnapshot.clear();
                    latestToolCallSnapshot.addAll(output.getToolCalls());
                }
            })
            .blockLast(Duration.ofMinutes(5));

        List<AssistantMessage.ToolCall> mergedToolCalls =
            mergeToolCallFragments(latestToolCallSnapshot);
        List<AssistantMessage.ToolCall> executableToolCalls =
            filterExecutableToolCalls(mergedToolCalls);
        if (
            !latestToolCallSnapshot.isEmpty() &&
            mergedToolCalls.size() != latestToolCallSnapshot.size()
        ) {
            log.info(
                "[SwarmLlmCaller] Collapsed streamed tool call fragments: rawCount={}, mergedCount={}",
                latestToolCallSnapshot.size(),
                mergedToolCalls.size()
            );
        }
        if (mergedToolCalls.size() != executableToolCalls.size()) {
            log.warn(
                "[SwarmLlmCaller] Dropped invalid streamed tool calls after merge: mergedCount={}, executableCount={}",
                mergedToolCalls.size(),
                executableToolCalls.size()
            );
        }

        return SwarmLlmResponse.builder()
            .content(contentBuilder.toString())
            .toolCalls(
                executableToolCalls.isEmpty() ? null : executableToolCalls
            )
            .build();
    }

    private List<AssistantMessage.ToolCall> mergeToolCallFragments(
        List<AssistantMessage.ToolCall> fragments
    ) {
        if (fragments == null || fragments.isEmpty()) {
            return List.of();
        }
        List<AssistantMessage.ToolCall> merged = new ArrayList<>();
        MutableToolCall current = null;
        for (AssistantMessage.ToolCall fragment : fragments) {
            if (fragment == null) {
                continue;
            }
            if (current == null) {
                current = MutableToolCall.from(fragment);
                continue;
            }
            if (shouldStartNewToolCall(current, fragment)) {
                if (current.isMeaningful()) {
                    merged.add(current.toToolCall());
                }
                current = MutableToolCall.from(fragment);
                continue;
            }
            current.absorb(fragment);
        }
        if (current != null && current.isMeaningful()) {
            merged.add(current.toToolCall());
        }
        return merged;
    }

    private boolean shouldStartNewToolCall(
        MutableToolCall current,
        AssistantMessage.ToolCall fragment
    ) {
        String fragmentId = normalize(fragment.id());
        String fragmentName = normalize(fragment.name());
        String fragmentArgs =
            fragment.arguments() != null ? fragment.arguments() : "";

        if (
            fragmentId != null &&
            current.id != null &&
            !fragmentId.equals(current.id)
        ) {
            return true;
        }
        if (
            fragmentName != null &&
            current.name != null &&
            !fragmentName.equals(current.name)
        ) {
            return true;
        }

        if (!current.isJsonComplete(objectMapper)) {
            return false;
        }

        if (fragmentId != null || fragmentName != null) {
            return true;
        }

        String trimmedArgs = fragmentArgs.trim();
        return trimmedArgs.startsWith("{") || trimmedArgs.startsWith("[");
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private List<AssistantMessage.ToolCall> filterExecutableToolCalls(
        List<AssistantMessage.ToolCall> toolCalls
    ) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return List.of();
        }
        List<AssistantMessage.ToolCall> executable = new ArrayList<>();
        for (AssistantMessage.ToolCall toolCall : toolCalls) {
            if (!isExecutableToolCall(toolCall)) {
                continue;
            }
            executable.add(toolCall);
        }
        return executable;
    }

    private boolean isExecutableToolCall(AssistantMessage.ToolCall toolCall) {
        if (toolCall == null) {
            return false;
        }
        if (normalize(toolCall.name()) == null) {
            log.warn(
                "[SwarmLlmCaller] Ignoring streamed tool call without name: id={}, argsPreview={}",
                toolCall.id(),
                preview(toolCall.arguments())
            );
            return false;
        }

        String arguments = toolCall.arguments();
        if (arguments == null || arguments.isBlank()) {
            return true;
        }

        try {
            return objectMapper.readTree(arguments).isObject();
        } catch (Exception e) {
            log.warn(
                "[SwarmLlmCaller] Ignoring streamed tool call with incomplete arguments: tool={}, argsPreview={}",
                toolCall.name(),
                preview(arguments)
            );
            return false;
        }
    }

    private String preview(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 120
            ? normalized
            : normalized.substring(0, 120) + "...";
    }

    private static final class MutableToolCall {

        private String id;
        private String name;
        private final StringBuilder arguments = new StringBuilder();

        private static MutableToolCall from(
            AssistantMessage.ToolCall fragment
        ) {
            MutableToolCall call = new MutableToolCall();
            call.absorb(fragment);
            return call;
        }

        private void absorb(AssistantMessage.ToolCall fragment) {
            if (fragment.id() != null && !fragment.id().isBlank()) {
                this.id = fragment.id();
            }
            if (fragment.name() != null && !fragment.name().isBlank()) {
                this.name = fragment.name();
            }
            if (
                fragment.arguments() != null && !fragment.arguments().isEmpty()
            ) {
                this.arguments.append(fragment.arguments());
            }
        }

        private boolean isMeaningful() {
            return (
                (name != null && !name.isBlank()) || arguments.length() > 0
            );
        }

        private boolean isJsonComplete(ObjectMapper objectMapper) {
            String raw = arguments.toString().trim();
            if (raw.isEmpty()) {
                return false;
            }
            try {
                objectMapper.readTree(raw);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }

        private AssistantMessage.ToolCall toToolCall() {
            return new AssistantMessage.ToolCall(
                id != null ? id : "",
                "function",
                name != null ? name : "",
                arguments.toString()
            );
        }
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
