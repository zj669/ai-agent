package com.zj.aiagent.domain.chat.port;

import com.zj.aiagent.domain.chat.entity.Conversation;
import com.zj.aiagent.domain.chat.entity.Message;
import com.zj.aiagent.shared.response.PageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository {

    Conversation save(Conversation conversation);

    Optional<Conversation> findById(String id);

    void deleteById(String id);

    /**
     * 查询某用户的会话列表
     */
    PageResult<Conversation> findByUserIdAndAgentId(String userId, String agentId, Pageable pageable);

    /**
     * 保存消息
     */
    Message saveMessage(Message message);

    /**
     * 查询消息详情
     */
    Optional<Message> findMessageById(String messageId);

    /**
     * 查询会话的历史消息 (分页)
     * 实际上通常是 Slice 滚动加载，这里简化为 List
     */
    List<Message> findMessagesByConversationId(String conversationId, Pageable pageable);

    /**
     * 根据 metadata.runId 查询消息
     */
    Optional<Message> findMessageByRunId(String runId);

    /**
     * 软删除会话的所有消息
     */
    void deleteMessagesByConversationId(String conversationId);
}
