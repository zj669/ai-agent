package com.zj.aiagent.infrastructure.workflow.executor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.workflow.config.NodeConfig;
import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.port.NodeExecutorStrategy;
import com.zj.aiagent.domain.workflow.port.StreamPublisher;
import com.zj.aiagent.domain.workflow.valobj.ExecutionContext;
import com.zj.aiagent.domain.workflow.valobj.NodeExecutionResult;
import com.zj.aiagent.domain.workflow.valobj.NodeType;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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

    private final ChatClient.Builder chatClientBuilder;
    private final Executor executor;
    private final ObjectMapper objectMapper;

    public LlmNodeExecutorStrategy(
            ChatClient.Builder chatClientBuilder,
            @Qualifier("nodeExecutorThreadPool") Executor executor,
            ObjectMapper objectMapper) {
        this.chatClientBuilder = chatClientBuilder;
        this.executor = executor;
        this.objectMapper = objectMapper;
    }

    @Override
    public CompletableFuture<NodeExecutionResult> executeAsync(
            Node node,
            Map<String, Object> resolvedInputs,
            StreamPublisher streamPublisher) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                NodeConfig config = node.getConfig();

                // 获取执行上下文（用于 LTM/STM/Awareness）
                ExecutionContext context = (ExecutionContext) resolvedInputs.get("__context__");

                // Step 1: 构建 System Prompt（包含 LTM + Awareness）
                String systemPrompt = buildSystemPrompt(config, context, resolvedInputs);

                // Step 2: 构建 Message Chain（包含 STM）
                List<Message> messageChain = buildMessageChain(config, context, resolvedInputs, systemPrompt);

                log.info("[LLM Node {}] Executing with {} messages, system prompt length: {}",
                        node.getNodeId(), messageChain.size(), systemPrompt.length());

                // Step 3: 调用 LLM（流式输出）
                ChatClient chatClient = chatClientBuilder.build();
                StringBuilder fullResponse = new StringBuilder();

                Prompt prompt = new Prompt(messageChain);

                chatClient.prompt(prompt)
                        .stream()
                        .content()
                        .doOnNext(chunk -> {
                            fullResponse.append(chunk);
                            // 实时推送 delta
                            streamPublisher.publishDelta(chunk);
                        })
                        .doOnError(error -> {
                            log.error("[LLM Node {}] Stream error: {}", node.getNodeId(), error.getMessage());
                            streamPublisher.publishError(error.getMessage());
                        })
                        .blockLast();

                String response = fullResponse.toString();
                log.info("[LLM Node {}] Response received, length: {}", node.getNodeId(),
                        response.length());

                // 构建输出
                Map<String, Object> outputs = new HashMap<>();
                outputs.put("response", response);
                outputs.put("text", response); // 兼容常用 key

                return NodeExecutionResult.success(outputs);

            } catch (Exception e) {
                log.error("[LLM Node {}] Execution failed: {}", node.getNodeId(), e.getMessage(), e);
                streamPublisher.publishError(e.getMessage());
                return NodeExecutionResult.failed(e.getMessage());
            }
        }, executor);
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
     * 包含：基础人设 + LTM + Awareness + Context Ref
     */
    private String buildSystemPrompt(NodeConfig config, ExecutionContext context,
            Map<String, Object> resolvedInputs) {
        StringBuilder sb = new StringBuilder();

        // 1. 基础系统提示词
        String systemPromptConfig = config.getString("systemPrompt");
        if (StringUtils.hasText(systemPromptConfig)) {
            sb.append(systemPromptConfig).append("\n\n");
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
                        sb.append("- 节点[").append(nodeId).append("] 输出: ").append(json).append("\n");
                    } catch (JsonProcessingException e) {
                        sb.append("- 节点[").append(nodeId).append("] 输出: ").append(output).append("\n");
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
    private List<Message> buildMessageChain(NodeConfig config, ExecutionContext context,
            Map<String, Object> resolvedInputs, String systemPrompt) {
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

        // 3. 添加当前用户输入
        String userPrompt = buildUserPrompt(config, resolvedInputs);
        if (StringUtils.hasText(userPrompt)) {
            messages.add(new UserMessage(userPrompt));
        }

        return messages;
    }

    /**
     * 构建用户 Prompt（替换占位符）
     */
    private String buildUserPrompt(NodeConfig config, Map<String, Object> resolvedInputs) {
        String template = config.getString("userPromptTemplate");

        if (template == null || template.isEmpty()) {
            // 使用默认输入
            Object userInput = resolvedInputs.get("input");
            return userInput != null ? userInput.toString() : "";
        }

        // 简单替换（实际已由 ExecutionContext 预处理）
        String prompt = template;
        for (Map.Entry<String, Object> entry : resolvedInputs.entrySet()) {
            String placeholder = "#{" + entry.getKey() + "}";
            if (entry.getValue() != null) {
                prompt = prompt.replace(placeholder, entry.getValue().toString());
            }
        }

        return prompt;
    }
}
