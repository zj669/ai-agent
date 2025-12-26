package com.zj.aiagent.domain.memory.repository;

import com.zj.aiagent.domain.memory.entity.ChatMessage;
import com.zj.aiagent.domain.memory.entity.Memory;

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
}
