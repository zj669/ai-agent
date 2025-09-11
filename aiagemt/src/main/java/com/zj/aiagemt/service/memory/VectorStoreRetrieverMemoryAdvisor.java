package com.zj.aiagemt.service.memory;

import com.zj.aiagemt.service.memory.chatmemory.VectorStoreRetrieverMemory;
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

/**
 * 向量存储检索记忆顾问
 * 
 * <p>专注于处理ChatClient的请求和响应流程：</p>
 * <ul>
 *   <li><strong>请求增强</strong>：在用户请求前，从向量存储中检索相关历史对话，组合成完整上下文</li>
 *   <li><strong>响应保存</strong>：在AI响应后，将新的对话保存到向量存储中</li>
 * </ul>
 * 
 * <p>该类遵循单一职责原则，<strong>不负责</strong>向量检索等记忆管理逻辑，
 * 这些功能由 {@link VectorStoreRetrieverMemory} 专门处理。</p>
 * 
 * @author AI Agent
 * @since 1.0.0
 * @see VectorStoreRetrieverMemory
 */
@Slf4j
public class VectorStoreRetrieverMemoryAdvisor implements BaseAdvisor {

    /** 向量存储检索记忆管理器 - 负责实际的记忆管理工作 */
    private final VectorStoreRetrieverMemory chatMemory;
    
    /** Advisor的执行顺序 */
    private final int order;

    /**
     * 构造函数
     * 
     * @param chatMemory 向量存储检索记忆管理器，不能为null
     * @param order Advisor执行顺序
     * @throws IllegalArgumentException 如果chatMemory为null
     */
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

    /**
     * 请求处理前执行 - 增强用户请求
     * 
     * <p><strong>核心功能：基于语义相似度检索相关历史对话</strong></p>
     * <ol>
     *   <li>从向量存储中检索与当前用户输入相关的历史对话</li>
     *   <li>将相关历史消息添加到当前请求的上下文中</li>
     *   <li>确保AI模型能够看到最相关的历史上下文</li>
     * </ol>
     * 
     * @param chatClientRequest 原始用户请求
     * @param advisorChain Advisor链
     * @return 增强后的请求（包含相关历史上下文）
     */
    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        Map<String, Object> context = chatClientRequest.context();
        String conversationId = getConversationId(context);
        
        // 将用户消息保存到上下文中，供 after 方法使用
        context.put("userMessage", chatClientRequest.prompt().getUserMessage().getText());
        
        try {
            // 从向量存储检索相关历史消息
            List<Message> relevantHistory = chatMemory.get(conversationId);
            
            if (relevantHistory != null && !relevantHistory.isEmpty()) {
                // 将相关历史消息添加到请求中
                ChatClientRequest enhancedRequest = addHistoryToRequest(chatClientRequest, relevantHistory);
                
                log.debug("📚 已加载相关历史消息 - conversationId: {}, 相关消息数: {}", 
                        conversationId, relevantHistory.size());
                
                return enhancedRequest;
            } else {
                log.debug("🆕 未找到相关历史消息 - conversationId: {}", conversationId);
                return chatClientRequest;
            }
            
        } catch (Exception e) {
            log.warn("⚠️ 检索相关历史消息失败，使用原始请求 - conversationId: {}, 错误: {}", 
                    conversationId, e.getMessage());
            return chatClientRequest;
        }
    }

    /**
     * 响应处理后执行 - 保存新对话
     * 
     * <p><strong>核心功能：将新的对话轮次保存到向量存储中</strong></p>
     * <ol>
     *   <li>提取用户输入消息</li>
     *   <li>提取AI响应消息</li>
     *   <li>将新消息向量化并存储到向量数据库</li>
     * </ol>
     * 
     * @param chatClientResponse AI响应结果
     * @param advisorChain Advisor链
     * @return 原始响应（不修改）
     */
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

    // ======================== 私有工具方法 ========================
    
    /**
     * 从请求上下文中获取对话ID
     * 
     * <p>支持多种常见的键名格式：conversationId, sessionId, userId</p>
     * 
     * @param context 请求上下文
     * @return 对话ID，默认为"default"
     */
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
    
    /**
     * 将历史消息添加到请求中
     * 
     * <p>将检索到的相关历史消息添加到当前请求的消息列表中，
     * 保持时间顺序：历史消息 + 当前用户输入。</p>
     * 
     * @param chatClientRequest 原始请求
     * @param historyMessages 相关历史消息列表
     * @return 增强后的请求
     */
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
    
    /**
     * 从响应中提取用户消息
     * 
     * <p>尝试从响应上下文或其他地方提取原始的用户输入</p>
     * 
     * @param chatClientResponse AI响应
     * @return 用户消息内容，如果无法提取则返回null
     */
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
    
    // ======================== 扩展功能方法 ========================
    
    /**
     * 获取内部的记忆管理器（用于测试和监控）
     * 
     * @return 向量存储检索记忆管理器
     */
    public VectorStoreRetrieverMemory getChatMemory() {
        return chatMemory;
    }
    
    /**
     * 获取对话统计信息（用于监控和调试）
     * 
     * @param conversationId 对话ID
     * @return 包含统计信息的Map
     */
    public Map<String, Object> getConversationStats(String conversationId) {
        return chatMemory.getConversationStats(conversationId);
    }
}
