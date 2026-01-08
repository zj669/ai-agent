package com.zj.aiagent.domain.memory.repository;

import com.zj.aiagent.domain.memory.entity.ChatMessage;
import com.zj.aiagent.domain.memory.entity.NodeExecutionRecord;

import java.util.List;

/**
 * 对话历史存储接口
 * <p>
 * 定义对话消息的持久化技术接口，具体实现由基础设施层提供
 * （如 Redis、数据库、Spring AI ChatMemory 等）
 */
public interface ChatHistoryRepository {

    /**
     * 保存对话消息
     *
     * @param executionId 执行ID
     * @param message     对话消息
     */
    void save(String executionId, ChatMessage message);

    /**
     * 加载对话历史
     *
     * @param executionId 执行ID
     * @param maxMessages 最大消息数
     * @return 对话消息列表（按时间倒序）
     */
    List<ChatMessage> load(String executionId, int maxMessages);

    /**
     * 清除对话历史
     *
     * @param executionId 执行ID
     */
    void clear(String executionId);

    /**
     * 加载带节点执行详情的完整消息
     *
     * @param conversationId 会话ID
     * @param maxMessages    最大消息数
     * @return 包含节点执行详情的消息列表
     */
    List<ChatMessage> loadWithNodeExecutions(String conversationId, int maxMessages);

    /**
     * 保存节点执行日志
     *
     * @param conversationId 会话ID
     * @param instanceId     实例ID
     * @param record         节点执行记录
     */
    void saveNodeExecution(String conversationId, Long instanceId, NodeExecutionRecord record);

    /**
     * 根据实例ID查询节点执行日志
     *
     * @param instanceId 实例ID
     * @return 节点执行记录列表
     */
    List<NodeExecutionRecord> loadNodeExecutions(Long instanceId);

    /**
     * 查询用户的会话ID列表
     *
     * @param userId  用户ID
     * @param agentId Agent ID
     * @return 会话ID列表
     */
    List<String> queryConversationIds(Long userId, String agentId);
}
