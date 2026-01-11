package com.zj.aiagent.infrastructure.workflow.executor;

import com.zj.aiagent.domain.workflow.config.LlmNodeConfig;
import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.port.NodeExecutorStrategy;

import com.zj.aiagent.domain.workflow.valobj.NodeExecutionResult;
import com.zj.aiagent.domain.workflow.valobj.NodeType;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * LLM 节点执行策略
 * 使用 Spring AI 调用大模型
 */
@Slf4j
@Component
public class LlmNodeExecutorStrategy implements NodeExecutorStrategy {

    private final ChatClient.Builder chatClientBuilder;
    private final Executor executor;

    public LlmNodeExecutorStrategy(
            ChatClient.Builder chatClientBuilder,
            @Qualifier("nodeExecutorThreadPool") Executor executor) {
        this.chatClientBuilder = chatClientBuilder;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<NodeExecutionResult> executeAsync(Node node, Map<String, Object> resolvedInputs) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LlmNodeConfig config = (LlmNodeConfig) node.getConfig();

                // 构建 Prompt
                String prompt = buildPrompt(config, resolvedInputs);

                log.info("[LLM Node {}] Executing with prompt: {}", node.getNodeId(),
                        prompt.length() > 100 ? prompt.substring(0, 100) + "..." : prompt);

                // 调用 LLM
                ChatClient chatClient = chatClientBuilder.build();
                String response = chatClient.prompt()
                        .user(prompt)
                        .call()
                        .content();

                log.info("[LLM Node {}] Response received, length: {}", node.getNodeId(),
                        response != null ? response.length() : 0);

                // 构建输出
                Map<String, Object> outputs = new HashMap<>();
                outputs.put("response", response);
                outputs.put("text", response); // 兼容常用 key

                return NodeExecutionResult.success(outputs);

            } catch (Exception e) {
                log.error("[LLM Node {}] Execution failed: {}", node.getNodeId(), e.getMessage(), e);
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
     * 构建 Prompt（替换 SpEL 占位符）
     */
    private String buildPrompt(LlmNodeConfig config, Map<String, Object> resolvedInputs) {
        String template = config.getPromptTemplate();

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
