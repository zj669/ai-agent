package com.zj.aiagemt.service.memory;

import com.zj.aiagemt.service.memory.chatmemory.VectorStoreRetrieverMemory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Slf4j
public class VectorStoreRetrieverMemoryAdvisor implements BaseAdvisor {

    @Getter
    private final VectorStoreRetrieverMemory chatMemory;

    /** Advisor的执行顺序 */
    private final int order;


    public VectorStoreRetrieverMemoryAdvisor(VectorStoreRetrieverMemory chatMemory, int order) {
        if (chatMemory == null) {
            throw new IllegalArgumentException("ChatMemory不能为null");
        }
        this.chatMemory = chatMemory;
        this.order = order;

        log.info("🤖 VectorStoreRetrieverMemoryAdvisor初始化完成 - order: {}, topK: {}, 相似度阈值: {}",
                order, chatMemory.getTopK(), chatMemory.getSimilarityThreshold());
    }

    /**
     * 使用默认顺序的构造函数
     * 
     * @param chatMemory 向量存储检索记忆管理器，不能为null
     */
    public VectorStoreRetrieverMemoryAdvisor(VectorStoreRetrieverMemory chatMemory) {
        this(chatMemory, 200); // 设置为200，在ConversationSummaryMemoryAdvisor(100)之后执行
    }

    // ======================== BaseAdvisor接口实现 ========================

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return this.order;
    }


    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        Map<String, Object> context = chatClientRequest.context();
        String conversationId = getConversationId(context);

        // 提取用户消息
        String userMessage = chatClientRequest.prompt().getUserMessage().getText();

        // 将用户消息保存到上下文中，供 after 方法使用
        context.put("userMessage", userMessage);

        try {
            // 🔍 关键修复：使用用户的实际消息作为查询，进行语义相似度检索
            List<Message> relevantHistory = chatMemory.get(conversationId, userMessage);

            if (relevantHistory != null && !relevantHistory.isEmpty()) {
                // 将相关历史消息添加到请求中
                ChatClientRequest enhancedRequest = addHistoryToRequest(chatClientRequest, relevantHistory);

                log.debug("📚 已加载相关历史消息 - conversationId: {}, 相关消息数: {}, 查询: {}",
                        conversationId, relevantHistory.size(),
                        userMessage.substring(0, Math.min(50, userMessage.length())) + "...");

                return enhancedRequest;
            } else {
                log.debug("🆕 未找到相关历史消息 - conversationId: {}, 查询: {}",
                        conversationId, userMessage.substring(0, Math.min(50, userMessage.length())) + "...");
                return chatClientRequest;
            }

        } catch (Exception e) {
            log.warn("⚠️ 检索相关历史消息失败，使用原始请求 - conversationId: {}, 错误: {}",
                    conversationId, e.getMessage());
            return chatClientRequest;
        }
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        String conversationId = getConversationId(chatClientResponse.context());

        try {
            // 构建要保存的新消息列表
            List<Message> newMessages = new ArrayList<>();

            // 提取用户消息
            String userMessage = extractUserMessage(chatClientResponse);
            if (userMessage != null && !userMessage.trim().isEmpty()) {
                newMessages.add(new UserMessage(userMessage));
            }

            // 提取AI响应消息
            String assistantResponse = chatClientResponse.chatResponse().getResult().getOutput().getText();
            if (assistantResponse != null && !assistantResponse.trim().isEmpty()) {
                newMessages.add(new AssistantMessage(assistantResponse));
            }

            // 保存到向量存储
            if (!newMessages.isEmpty()) {
                chatMemory.add(conversationId, newMessages);
                log.debug("💾 成功保存新消息到向量存储 - conversationId: {}, 消息数: {}",
                        conversationId, newMessages.size());
            }

        } catch (Exception e) {
            log.warn("⚠️ 保存消息到向量存储失败 - conversationId: {}, 错误: {}",
                    conversationId, e.getMessage());
        }

        return chatClientResponse;
    }



    private String getConversationId(Map<String, Object> context) {
        if (context == null) {
            return "default";
        }

        // 优先级顺序尝试不同的键名
        Object conversationId = context.get("conversationId");
        if (conversationId != null) {
            return conversationId.toString();
        }

        Object sessionId = context.get("sessionId");
        if (sessionId != null) {
            return sessionId.toString();
        }

        Object userId = context.get("userId");
        if (userId != null) {
            return "user_" + userId.toString();
        }

        return "default";
    }


    private ChatClientRequest addHistoryToRequest(ChatClientRequest chatClientRequest, List<Message> historyMessages) {
        try {
            // 获取当前请求的所有消息
            List<Message> currentMessages = new ArrayList<>(chatClientRequest.prompt().getInstructions());

            // 创建新的消息列表：历史消息 + 当前消息
            List<Message> allMessages = new ArrayList<>();
            allMessages.addAll(historyMessages); // 先添加相关历史
            allMessages.addAll(currentMessages); // 再添加当前消息

            // 构建新的请求
            ChatClientRequest enhancedRequest = chatClientRequest.mutate()
                    .prompt(chatClientRequest.prompt().mutate().messages(allMessages).build())
                    .build();

            log.debug("📝 历史消息添加完成 - 历史消息: {}条, 当前消息: {}条, 总消息: {}条",
                    historyMessages.size(), currentMessages.size(), allMessages.size());

            return enhancedRequest;

        } catch (Exception e) {
            log.warn("⚠️ 添加历史消息到请求失败，使用原始请求", e);
            return chatClientRequest;
        }
    }

    private String extractUserMessage(ChatClientResponse chatClientResponse) {
        try {
            Map<String, Object> context = chatClientResponse.context();
            if (context != null) {
                Object userInput = context.get("userMessage");
                if (userInput != null) {
                    return userInput.toString();
                }

                Object prompt = context.get("prompt");
                if (prompt != null) {
                    return prompt.toString();
                }
            }

            return null;

        } catch (Exception e) {
            log.debug("无法提取用户消息: {}", e.getMessage());
            return null;
        }
    }

    public Map<String, Object> getConversationStats(String conversationId) {
        return chatMemory.getConversationStats(conversationId);
    }
}
