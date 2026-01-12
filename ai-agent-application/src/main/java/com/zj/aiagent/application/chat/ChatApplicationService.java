package com.zj.aiagent.application.chat;

import com.zj.aiagent.domain.chat.entity.Conversation;
import com.zj.aiagent.domain.chat.entity.Message;
import com.zj.aiagent.domain.chat.port.ConversationRepository;
import com.zj.aiagent.domain.chat.valobj.MessageRole;
import com.zj.aiagent.domain.chat.valobj.MessageStatus;
import com.zj.aiagent.domain.chat.valobj.ThoughtStep;
import com.zj.aiagent.shared.response.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 聊天应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatApplicationService {

    private final ConversationRepository conversationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public String createConversation(String userId, String agentId) {
        Conversation conversation = Conversation.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .agentId(agentId)
                // 初始标题生成可以异步处理，这里先给默认值
                .title("New Chat "
                        + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm")))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        conversationRepository.save(conversation);
        return conversation.getId();
    }

    public PageResult<Conversation> getConversationHistory(String userId, String agentId, Pageable pageable) {
        return conversationRepository.findByUserIdAndAgentId(userId, agentId, pageable);
    }

    @Transactional
    public Message appendUserMessage(String conversationId, String content) {
        return appendUserMessage(conversationId, content, Collections.emptyMap());
    }

    @Transactional
    public Message appendUserMessage(String conversationId, String content, Map<String, Object> metadata) {
        // 1. 更新会话时间
        conversationRepository.findById(conversationId).ifPresent(c -> {
            c.markUpdated();
            conversationRepository.save(c);
        });

        // 2. 保存用户消息
        Message message = Message.builder()
                .id(UUID.randomUUID().toString())
                .conversationId(conversationId)
                .role(MessageRole.USER)
                .content(content)
                .status(MessageStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .metadata(metadata)
                .build();

        Message saved = conversationRepository.saveMessage(message);

        // 3. 发布事件 (可触发自动标题等)
        // eventPublisher.publishEvent(new MessageAppendedEvent(saved));

        return saved;
    }

    /**
     * 初始化 Assistant 消息 (Pending状态)
     * 返回消息ID，供流式更新使用
     */
    @Transactional
    public String initAssistantMessage(String conversationId, String runId) {
        Message message = Message.initAssistant(conversationId, runId);
        conversationRepository.saveMessage(message);
        return message.getId();
    }

    /**
     * 更新(完成) Assistant 消息
     */
    @Transactional
    public void finalizeMessage(String messageId, String content, List<ThoughtStep> thoughtProcess,
            MessageStatus status) {
        conversationRepository.findMessageById(messageId).ifPresent(message -> {
            message.setContent(content);
            message.setThoughtProcess(thoughtProcess);
            message.setStatus(status);
            // 可以更新 tokenCount 等元数据

            conversationRepository.saveMessage(message);
        });
    }

    public Optional<Message> findMessageByRunId(String runId) {
        return conversationRepository.findMessageByRunId(runId);
    }

    public List<Message> getMessages(String conversationId, Pageable pageable) {
        return conversationRepository.findMessagesByConversationId(conversationId, pageable);
    }

    @Transactional
    public void deleteConversation(String conversationId) {
        conversationRepository.deleteById(conversationId);
        conversationRepository.deleteMessagesByConversationId(conversationId);
    }
}
