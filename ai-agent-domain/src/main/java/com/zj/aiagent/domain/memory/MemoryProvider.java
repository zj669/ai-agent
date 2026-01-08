package com.zj.aiagent.domain.memory;

import com.zj.aiagent.domain.memory.entity.ChatMessage;
import com.zj.aiagent.domain.memory.entity.Memory;

import java.util.List;

/**
 * Memory Provider - 管理对话历史和长短期记忆
 */
public interface MemoryProvider {

    /**
     * 加载对话历史
     * 
     * @param executionId 执行ID
     * @param maxMessages 最大消息数
     * @return 对话历史列表
     */
    List<ChatMessage> loadChatHistory(String executionId, int maxMessages);

    /**
     * 加载带节点执行详情的对话历史
     *
     * @param conversationId 会话ID
     * @param maxMessages    最大消息数
     * @return 包含节点执行详情的对话历史列表
     */
    List<ChatMessage> loadChatHistoryWithNodeExecutions(String conversationId, int maxMessages);

    /**
     * 保存消息到对话历史
     * 
     * @param executionId 执行ID
     * @param message     消息
     */
    void saveChatMessage(String executionId, ChatMessage message);

    /**
     * 加载长期记忆
     * 
     * @param executionId 执行ID
     * @param query       查询关键词
     * @return 相关的长期记忆
     */
    List<Memory> loadLongTermMemory(String executionId, String query);

    /**
     * 保存长期记忆
     * 
     * @param executionId 执行ID
     * @param memory      记忆
     */
    void saveLongTermMemory(String executionId, Memory memory);

    /**
     * 清除对话历史
     * 
     * @param executionId 执行ID
     */
    void clearChatHistory(String executionId);

    /**
     * 查询用户的会话ID列表
     *
     * @param userId  用户ID
     * @param agentId Agent ID
     * @return 会话ID列表
     */
    List<String> queryConversationIds(Long userId, String agentId);
}
