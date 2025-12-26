package com.zj.aiagent.infrastructure.parse.adpater;

import com.zj.aiagent.infrastructure.context.AgentContextProvider;
import com.zj.aiagent.shared.design.workflow.NodeExecutor;
import com.zj.aiagent.shared.design.workflow.StateUpdate;
import com.zj.aiagent.shared.design.workflow.WorkflowState;
import com.zj.aiagent.shared.design.workflow.WorkflowStateListener;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import reactor.core.publisher.Flux;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AI 聊天节点的抽象基类
 * <p>
 * 提供通用的 ChatClient 构建和流式调用能力，
 * 子类只需实现提示词构建和响应处理逻辑
 */
@Slf4j
@AllArgsConstructor
@Data
public abstract class BaseChatNodeExecutorAdapter implements NodeExecutor {

    protected final String nodeId;
    protected final String nodeName;
    protected final String description;
    protected final String nodeType;
    protected final OpenAiChatModel chatModel;
    protected final String systemPrompt;
    protected final AgentContextProvider contextProvider; // 新增:统一上下文管理

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public String getNodeName() {
        return nodeName;
    }

    @Override
    public StateUpdate execute(WorkflowState state) {
        try {
            // 1. 构建提示词（由子类实现）
            String prompt = buildPrompt(state);

            // 2. 调用 AI
            ChatClient chatClient = buildChatClient();
            String aiResponse = callAIStream(chatClient, prompt, state);

            // 3. 处理响应（由子类实现）
            return processResponse(aiResponse, state);

        } catch (Exception e) {
            log.error("节点 {} 执行失败", nodeId, e);
            return StateUpdate.error("节点执行失败: " + e.getMessage());
        }
    }

    /**
     * 构建提示词
     * <p>
     * 子类根据节点类型实现不同的提示词构建逻辑
     * 
     * @param state 当前状态
     * @return 提示词字符串
     */
    protected abstract String buildPrompt(WorkflowState state);

    /**
     * 处理 AI 响应
     * <p>
     * 子类根据节点类型实现不同的响应处理逻辑
     * 
     * @param aiResponse AI 响应内容
     * @param state      当前状态
     * @return 状态更新
     */
    protected abstract StateUpdate processResponse(String aiResponse, WorkflowState state);

    /**
     * 构建 ChatClient（通用逻辑）
     */
    protected ChatClient buildChatClient() {
        ChatClient.Builder builder = ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt);


        return builder.build();
    }

    /**
     * 流式调用 AI（通用逻辑）
     */
    protected String callAIStream(ChatClient chatClient, String prompt, WorkflowState state) {
        StringBuilder fullResponse = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        WorkflowStateListener listener = state.getWorkflowStateListener();

        if (listener == null) {
            throw new RuntimeException("WorkflowStateListener is null");
        }

        try {
            ChatClient.ChatClientRequestSpec spec = chatClient.prompt(prompt);

            Flux<String> stream = spec.stream().content();

            stream.subscribe(
                    chunk -> {
                        // 推送流式内容
                        listener.onNodeStreaming(nodeId, chunk);
                        fullResponse.append(chunk);
                    },
                    error -> {
                        log.error("流式调用失败", error);
                        errorRef.set((Exception) error);
                        latch.countDown();
                    },
                    latch::countDown);

            // 等待流完成，最多等待 5 分钟
            boolean completed = latch.await(5, TimeUnit.MINUTES);
            if (!completed) {
                throw new RuntimeException("流式调用超时（5分钟）");
            }

            // 检查错误
            Exception error = errorRef.get();
            if (error != null) {
                throw new RuntimeException("流式调用失败: " + error.getMessage());
            }

            return fullResponse.toString();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("流式调用被中断", e);
        }
    }
}
