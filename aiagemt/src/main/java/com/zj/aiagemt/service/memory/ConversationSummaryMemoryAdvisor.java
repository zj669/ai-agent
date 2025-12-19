package com.zj.aiagemt.service.memory;

import com.zj.aiagemt.service.memory.chatmemory.ConversationSummaryMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Slf4j
public class ConversationSummaryMemoryAdvisor implements BaseAdvisor {

    /** 对话摘要记忆管理器 - 负责实际的记忆管理工作 */
    private final ConversationSummaryMemory chatMemory;
    
    /** Advisor的执行顺序 */
    private final int order;

    /**
     * 构造函数
     * 
     * @param chatMemory 对话摘要记忆管理器，不能为null
     * @param order Advisor执行顺序
     * @throws IllegalArgumentException 如果chatMemory为null
     */
    public ConversationSummaryMemoryAdvisor(ConversationSummaryMemory chatMemory, int order) {
        if (chatMemory == null) {
            throw new IllegalArgumentException("ChatMemory不能为null");
        }
        this.chatMemory = chatMemory;
        this.order = order;
        
        log.info("🤖 ConversationSummaryMemoryAdvisor初始化完成 - order: {}", order);
    }
    
    /**
     * 使用默认顺序的构造函数
     * 
     * @param chatMemory 对话摘要记忆管理器，不能为null
     */
    public ConversationSummaryMemoryAdvisor(ConversationSummaryMemory chatMemory) {
        this(chatMemory, 100);
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
     * <p><strong>核心功能：将历史对话与用户请求组合</strong></p>
     * <ol>
     *   <li>从记忆管理器中获取对话历史</li>
     *   <li>将历史消息添加到当前请求的上下文中</li>
     *   <li>确保AI模型能够看到完整的对话历史（包括可能的摘要）</li>
     * </ol>
     * 
     * @param chatClientRequest 原始用户请求
     * @param advisorChain Advisor链
     * @return 增强后的请求（包含历史上下文）
     */
    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        Map<String, Object> context = chatClientRequest.context();
        String conversationId = getConversationId(context);
        context.put("userMessage", chatClientRequest.prompt().getUserMessage().getText());
        
        try {
            // 从记忆管理器获取历史消息（可能触发自动摘要）
            List<Message> historyMessages = chatMemory.get(conversationId);
            
            if (historyMessages != null && !historyMessages.isEmpty()) {
                // 将历史消息添加到请求中
                ChatClientRequest enhancedRequest = addHistoryToRequest(chatClientRequest, historyMessages);
                
                log.debug("📚 已加载历史消息 - conversationId: {}, 历史消息数: {}", 
                        conversationId, historyMessages.size());
                
                return enhancedRequest;
            } else {
                log.debug("🆕 无历史消息，这是新对话 - conversationId: {}", conversationId);
                return chatClientRequest;
            }
            
        } catch (Exception e) {
            log.warn("⚠️ 加载历史消息失败，使用原始请求 - conversationId: {}, 错误: {}", 
                    conversationId, e.getMessage());
            return chatClientRequest;
        }
    }

    /**
     * 响应处理后执行 - 保存新对话
     * 
     * <p><strong>核心功能：将新的对话轮次保存到记忆中</strong></p>
     * <ol>
     *   <li>提取用户输入消息</li>
     *   <li>提取AI响应消息</li>
     *   <li>将新消息保存到记忆管理器（可能触发自动摘要）</li>
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
            
            // 保存到记忆管理器（可能自动触发摘要生成）
            if (!newMessages.isEmpty()) {
                chatMemory.add(conversationId, newMessages);
                log.debug("💾 成功保存新消息 - conversationId: {}, 消息数: {}", 
                        conversationId, newMessages.size());
            }
            
        } catch (Exception e) {
            log.warn("⚠️ 保存消息到记忆失败 - conversationId: {}, 错误: {}", 
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
     * <p>注意：这里的实现可能需要根据Spring AI的具体API进行调整</p>
     * 
     * @param chatClientRequest 原始请求
     * @param historyMessages 历史消息列表
     * @return 增强后的请求
     */
    private ChatClientRequest addHistoryToRequest(ChatClientRequest chatClientRequest, List<Message> historyMessages) {
        // TODO: 根据Spring AI框架的实际API实现请求增强
        // 这里可能需要使用ChatClientRequest的构建器或其他方法来添加历史消息
        
        try {
            // 临时实现：直接返回原始请求
            // 在实际使用时，需要根据Spring AI的API文档来正确实现
            log.debug("📝 历史消息组合功能待完善 - 消息数: {}", historyMessages.size());
            historyMessages.addAll(chatClientRequest.prompt().getInstructions());
            ChatClientRequest processedChatClientRequest = chatClientRequest.mutate().prompt(chatClientRequest.prompt().mutate().messages(historyMessages).build()).build();
            return processedChatClientRequest;
            
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
            // TODO: 根据实际的ChatClientResponse结构来提取用户消息
            // 这里可能需要从response的context中获取原始的用户输入
            
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
}
