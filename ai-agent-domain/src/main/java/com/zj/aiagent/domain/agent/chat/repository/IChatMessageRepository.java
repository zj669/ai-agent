package com.zj.aiagent.domain.agent.chat.repository;

import com.zj.aiagent.domain.agent.chat.entity.ChatMessageEntity;

import java.util.List;

/**
 * 聊天消息仓储接口
 */
public interface IChatMessageRepository {

    /**
     * 保存聊天消息
     */
    ChatMessageEntity save(ChatMessageEntity message);

    /**
     * 根据会话ID查询消息列表（按时间升序）
     */
    List<ChatMessageEntity> findByConversationId(String conversationId);

    /**
     * 根据 instanceId 查询节点执行日志
     */
    List<ChatMessageEntity.NodeExecutionInfo> findNodeExecutionsByInstanceId(Long instanceId);

    /**
     * 统计会话的消息数量
     */
    int countByConversationId(String conversationId);

    /**
     * 删除会话的所有消息
     */
    void deleteByConversationId(String conversationId);
}
