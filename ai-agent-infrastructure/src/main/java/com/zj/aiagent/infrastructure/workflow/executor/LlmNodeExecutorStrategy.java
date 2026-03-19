package com.zj.aiagent.infrastructure.workflow.executor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.knowledge.service.KnowledgeRetrievalService;
import com.zj.aiagent.domain.llm.entity.LlmProviderConfig;
import com.zj.aiagent.domain.llm.repository.LlmProviderConfigRepository;
import com.zj.aiagent.domain.workflow.config.NodeConfig;
import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.port.NodeExecutorStrategy;
import com.zj.aiagent.domain.workflow.port.StreamPublisher;
import com.zj.aiagent.domain.workflow.valobj.ExecutionContext;
import com.zj.aiagent.domain.workflow.valobj.NodeExecutionResult;
import com.zj.aiagent.domain.workflow.valobj.NodeType;
import com.zj.aiagent.infrastructure.workflow.template.PromptTemplateResolver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * LLM 节点执行策略
 * 使用 Spring AI 调用大模型，支持流式输出
 *
 * 增强能力：
 * - 自动注入长期记忆 (LTM) 到 System Prompt
 * - 支持会话历史 (STM) 作为 Message Chain
 * - 注入执行日志 (Awareness) 让 LLM 了解当前进度
 */
@Slf4j
@Component
public class LlmNodeExecutorStrategy implements NodeExecutorStrategy {

    private final Executor executor;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final LlmProviderConfigRepository llmProviderConfigRepository;
    private final PromptTemplateResolver promptTemplateResolver;

    public LlmNodeExecutorStrategy(
        @Qualifier("nodeExecutorThreadPool") Executor executor,
        @Qualifier("restClientBuilder1") RestClient.Builder restClientBuilder,
        ObjectMapper objectMapper,
        KnowledgeRetrievalService knowledgeRetrievalService,
        LlmProviderConfigRepository llmProviderConfigRepository,
        PromptTemplateResolver promptTemplateResolver
    ) {
        this.executor = executor;
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
        this.llmProviderConfigRepository = llmProviderConfigRepository;
        this.promptTemplateResolver = promptTemplateResolver;
    }

    @Override
    public CompletableFuture<NodeExecutionResult> executeAsync(
        Node node,
        Map<String, Object> resolvedInputs,
        StreamPublisher streamPublisher
    ) {
        return CompletableFuture.supplyAsync(
            () -> {
                try {
                    NodeConfig config = node.getConfig();

                    // 优先通过 llmConfigId 引用模型配置
                    String model;
                    String apiUrl;
                    String apiKey;
                    Long llmConfigId = config.getLong("llmConfigId");
                    if (llmConfigId != null) {
                        LlmProviderConfig providerConfig =
                            llmProviderConfigRepository
                                .findById(llmConfigId)
                                .orElseThrow(() ->
                                    new IllegalStateException(
                                        "模型配置不存在（ID: " +
                                            llmConfigId +
                                            "），请在模型配置页面检查"
                                    )
                                );
                        model = providerConfig.getModel();
                        apiUrl = providerConfig.getBaseUrl();
                        apiKey = providerConfig.getApiKey();
                    } else {
                        // 兼容旧数据：直接从节点配置读取
                        model =
                            config.getString("llm_model") != null
                                ? config.getString("llm_model")
                                : config.getString("model");
                        apiUrl =
                            config.getString("llm_base_url") != null
                                ? config.getString("llm_base_url")
                                : config.getString("baseUrl");
                        apiKey =
                            config.getString("llm_api_key") != null
                                ? config.getString("llm_api_key")
                                : config.getString("apiKey");
                    }

                    if (
                        !StringUtils.hasText(model) ||
                        !StringUtils.hasText(apiUrl) ||
                        !StringUtils.hasText(apiKey)
                    ) {
                        throw new IllegalStateException(
                            "LLM 节点缺少必要配置（model/baseUrl/apiKey），请在工作流编辑器中配置 LLM 节点参数"
                        );
                    }

                    // Spring AI OpenAiApi 会自动拼 /v1 前缀，去掉用户配置中多余的 /v1
                    String normalizedUrl = apiUrl.replaceAll("/v1/?$", "");

                    ChatClient.Builder chatClientBuilder = ChatClient.builder(
                        OpenAiChatModel.builder()
                            .openAiApi(
                                OpenAiApi.builder()
                                    .apiKey(apiKey)
                                    .baseUrl(normalizedUrl)
                                    .restClientBuilder(restClientBuilder)
                                    .build()
                            )
                            .defaultOptions(
                                OpenAiChatOptions.builder().model(model).build()
                            )
                            .build()
                    );
                    // 获取执行上下文（用于 LTM/STM/Awareness）
                    ExecutionContext context =
                        (ExecutionContext) resolvedInputs.get("__context__");
                    Long agentId = (Long) resolvedInputs.get("__agentId__");

                    // Step 1: 构建 System Prompt（包含 LTM + RAG + Awareness）
                    String userInput = buildUserPrompt(config, resolvedInputs);
                    String systemPrompt = buildSystemPrompt(
                        config,
                        context,
                        resolvedInputs,
                        agentId,
                        userInput
                    );

                    // Step 2: 构建 Message Chain（包含 STM），复用已构建的 userInput
                    List<Message> messageChain = buildMessageChain(
                        config,
                        context,
                        resolvedInputs,
                        systemPrompt,
                        userInput
                    );

                    log.info(
                        "[LLM Node {}] Executing with {} messages, system prompt length: {}",
                        node.getNodeId(),
                        messageChain.size(),
                        systemPrompt.length()
                    );

                    // Step 3: 调用 LLM（流式输出）
                    ChatClient chatClient = chatClientBuilder.build();
                    StringBuilder fullResponse = new StringBuilder();

                    Prompt prompt = new Prompt(messageChain);

                    chatClient
                        .prompt(prompt)
                        .stream()
                        .content()
                        .doOnNext(chunk -> {
                            fullResponse.append(chunk);
                            // 实时推送 delta
                            streamPublisher.publishDelta(chunk);
                        })
                        .doOnError(error -> {
                            log.error(
                                "[LLM Node {}] Stream error: {}",
                                node.getNodeId(),
                                error.getMessage()
                            );
                            streamPublisher.publishError(error.getMessage());
                        })
                        .blockLast();

                    String response = fullResponse.toString();
                    log.info(
                        "[LLM Node {}] Response received, length: {}",
                        node.getNodeId(),
                        response.length()
                    );

                    // 构建输出
                    Map<String, Object> outputs = new HashMap<>();
                    outputs.put("llm_output", response); // 与 node template outputSchema key 一致
                    outputs.put("response", response);
                    outputs.put("text", response); // 兼容常用 key

                    return NodeExecutionResult.success(outputs);
                } catch (Exception e) {
                    log.error(
                        "[LLM Node {}] Execution failed: {}",
                        node.getNodeId(),
                        e.getMessage(),
                        e
                    );
                    streamPublisher.publishError(e.getMessage());
                    return NodeExecutionResult.failed(e.getMessage());
                }
            },
            executor
        );
    }

    @Override
    public NodeType getSupportedType() {
        return NodeType.LLM;
    }

    @Override
    public boolean supportsStreaming() {
        return true; // LLM 支持流式输出
    }

    /**
     * 构建增强的 System Prompt
     * 包含：基础人设 + RAG知识库 + LTM + Awareness + Context Ref
     */
    private String buildSystemPrompt(
        NodeConfig config,
        ExecutionContext context,
        Map<String, Object> resolvedInputs,
        Long agentId,
        String userInput
    ) {
        StringBuilder sb = new StringBuilder();

        // 1. 基础系统提示词
        String systemPromptConfig = resolvePromptTemplate(
            config.getString("systemPrompt"),
            resolvedInputs
        );
        if (StringUtils.hasText(systemPromptConfig)) {
            sb.append(systemPromptConfig).append("\n\n");
        }

        // 2. [RAG] 知识库检索 - 根据用户输入从 Milvus 检索相关文档片段
        if (agentId != null && StringUtils.hasText(userInput)) {
            try {
                int ragTopK = config.getInteger("ragTopK", 5);
                List<String> knowledgeResults =
                    knowledgeRetrievalService.retrieve(
                        agentId,
                        userInput,
                        ragTopK
                    );
                if (knowledgeResults != null && !knowledgeResults.isEmpty()) {
                    sb.append("### 相关知识库内容 (Knowledge Base):\n");
                    for (int i = 0; i < knowledgeResults.size(); i++) {
                        sb
                            .append("[")
                            .append(i + 1)
                            .append("] ")
                            .append(knowledgeResults.get(i))
                            .append("\n\n");
                    }
                    sb.append(
                        "请基于以上知识库内容回答用户问题。如果知识库内容与问题无关，可以忽略。\n\n"
                    );
                    log.info(
                        "[LLM Node] RAG retrieved {} knowledge chunks for agentId: {}",
                        knowledgeResults.size(),
                        agentId
                    );
                }
            } catch (Exception e) {
                log.warn(
                    "[LLM Node] RAG retrieval failed for agentId {}: {}",
                    agentId,
                    e.getMessage()
                );
            }
        }

        if (context == null) {
            return sb.toString();
        }

        // 2. [LTM] 注入长期记忆
        List<String> ltm = context.getLongTermMemories();
        if (ltm != null && !ltm.isEmpty()) {
            sb.append("### 相关背景知识 (Long Term Memory):\n");
            for (String memory : ltm) {
                sb.append("- ").append(memory).append("\n");
            }
            sb.append("\n");
        }

        // 3. [Awareness] 注入执行日志
        if (config.getBoolean("includeExecutionLog", true)) {
            String execLog = context.getExecutionLogContent();
            if (StringUtils.hasText(execLog)) {
                sb.append("### 当前工作流执行进度 (Execution Log):\n");
                sb.append(execLog).append("\n");
            }
        }

        // 4. [Context Ref] 注入特定节点输出
        List<String> refNodes = config.getList("contextRefNodes");
        if (refNodes != null && !refNodes.isEmpty()) {
            sb.append("### 参考资料 (Reference Outputs):\n");
            for (String nodeId : refNodes) {
                Map<String, Object> output = context.getNodeOutput(nodeId);
                if (output != null && !output.isEmpty()) {
                    try {
                        String json = objectMapper.writeValueAsString(output);
                        sb
                            .append("- 节点[")
                            .append(nodeId)
                            .append("] 输出: ")
                            .append(json)
                            .append("\n");
                    } catch (JsonProcessingException e) {
                        sb
                            .append("- 节点[")
                            .append(nodeId)
                            .append("] 输出: ")
                            .append(output)
                            .append("\n");
                    }
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 构建 Message Chain
     * 包含：System + STM (历史对话) + 当前用户输入
     */
    private List<Message> buildMessageChain(
        NodeConfig config,
        ExecutionContext context,
        Map<String, Object> resolvedInputs,
        String systemPrompt,
        String userPrompt
    ) {
        List<Message> messages = new ArrayList<>();

        // 1. 添加 System Message
        if (StringUtils.hasText(systemPrompt)) {
            messages.add(new SystemMessage(systemPrompt));
        }

        // 2. [STM] 添加历史对话
        if (config.getBoolean("includeChatHistory", true) && context != null) {
            List<Map<String, String>> chatHistory = context.getChatHistory();
            if (chatHistory != null && !chatHistory.isEmpty()) {
                int maxRounds = config.getInteger("maxHistoryRounds", 10);
                int startIdx = Math.max(0, chatHistory.size() - maxRounds * 2);

                for (int i = startIdx; i < chatHistory.size(); i++) {
                    Map<String, String> entry = chatHistory.get(i);
                    String role = entry.get("role");
                    String content = entry.get("content");

                    if ("USER".equalsIgnoreCase(role)) {
                        messages.add(new UserMessage(content));
                    } else if ("ASSISTANT".equalsIgnoreCase(role)) {
                        messages.add(new AssistantMessage(content));
                    }
                }
            }
        }

        // 3. 添加当前用户输入（复用调用方已构建的 userPrompt）
        if (StringUtils.hasText(userPrompt)) {
            messages.add(new UserMessage(userPrompt));
        }

        return messages;
    }

    /**
     * 构建用户 Prompt（替换占位符）
     */
    String buildUserPrompt(
        NodeConfig config,
        Map<String, Object> resolvedInputs
    ) {
        String template = config.getString("userPromptTemplate");

        if (template == null || template.isEmpty()) {
            // 使用默认输入
            Object userInput = resolvedInputs.get("input");
            return userInput != null ? userInput.toString() : "";
        }

        return resolvePromptTemplate(template, resolvedInputs);
    }

    String resolvePromptTemplate(
        String template,
        Map<String, Object> resolvedInputs
    ) {
        ExecutionContext context = (ExecutionContext) resolvedInputs.get(
            "__context__"
        );
        return promptTemplateResolver.resolve(
            template,
            resolvedInputs,
            context
        );
    }
}
