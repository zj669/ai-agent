package com.zj.aiagent.application.chat;

import com.zj.aiagent.domain.chat.entity.Conversation;
import com.zj.aiagent.domain.chat.entity.Message;
import com.zj.aiagent.domain.chat.port.ConversationRepository;
import com.zj.aiagent.domain.chat.valobj.MessageRole;
import com.zj.aiagent.domain.chat.valobj.MessageStatus;
import com.zj.aiagent.domain.chat.valobj.ThoughtStep;
import com.zj.aiagent.shared.response.PageResult;
import com.zj.aiagent.shared.util.XssFilterUtil;
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

    /**
     * 消息内容最大长度限制（10000 字符）
     */
    private static final int MAX_MESSAGE_LENGTH = 10000;

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
        // 1. 验证消息长度
        if (content != null && content.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Message content exceeds maximum length of %d characters", MAX_MESSAGE_LENGTH));
        }

        // 2. XSS 过滤
        String filteredContent = XssFilterUtil.filter(content);
        if (filteredContent == null || filteredContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty after XSS filtering");
        }

        // 3. 更新会话时间
        conversationRepository.findById(conversationId).ifPresent(c -> {
            c.markUpdated();
            conversationRepository.save(c);
        });

        // 4. 保存用户消息
        Message message = Message.builder()
                .id(UUID.randomUUID().toString())
                .conversationId(conversationId)
                .role(MessageRole.USER)
                .content(filteredContent)
                .status(MessageStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .metadata(metadata)
                .build();

        Message saved = conversationRepository.saveMessage(message);

        // 5. 发布事件 (可触发自动标题等)
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
            // XSS 过滤 Assistant 消息内容
            String filteredContent = content != null ? XssFilterUtil.filter(content) : null;

            message.setContent(filteredContent);
            message.setThoughtProcess(thoughtProcess);
            message.setStatus(status);
            // 可以更新 tokenCount 等元数据

            conversationRepository.saveMessage(message);

            // 更新会话的 updatedAt 时间
            conversationRepository.findById(message.getConversationId()).ifPresent(c -> {
                c.markUpdated();
                conversationRepository.save(c);
            });
        });
    }

    public Optional<Message> findMessageByRunId(String runId) {
        return conversationRepository.findMessageByRunId(runId);
    }

    public Optional<Conversation> getConversationById(String conversationId) {
        return conversationRepository.findById(conversationId);
    }

    public List<Message> getMessages(String conversationId, Pageable pageable) {
        return conversationRepository.findMessagesByConversationId(conversationId, pageable);
    }

    /**
     * 验证用户是否有权访问会话
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @return 是否有权限
     */
    public boolean hasConversationAccess(String conversationId, String userId) {
        return conversationRepository.findById(conversationId)
                .map(conversation -> conversation.getUserId().equals(userId))
                .orElse(false);
    }

    /**
     * 获取会话消息（带权限校验）
     * @param conversationId 会话ID
     * @param userId 当前用户ID
     * @param pageable 分页参数
     * @return 消息列表
     * @throws IllegalAccessException 无权访问时抛出异常
     */
    public List<Message> getMessagesWithAuth(String conversationId, String userId, Pageable pageable) {
        if (!hasConversationAccess(conversationId, userId)) {
            throw new IllegalArgumentException("No permission to access conversation: " + conversationId);
        }
        return getMessages(conversationId, pageable);
    }

    @Transactional
    public void deleteConversation(String conversationId) {
        conversationRepository.deleteById(conversationId);
        conversationRepository.deleteMessagesByConversationId(conversationId);
    }

    /**
     * 删除会话（带权限校验）
     * @param conversationId 会话ID
     * @param userId 当前用户ID
     * @throws IllegalArgumentException 无权访问时抛出异常
     */
    @Transactional
    public void deleteConversationWithAuth(String conversationId, String userId) {
        if (!hasConversationAccess(conversationId, userId)) {
            throw new IllegalArgumentException("No permission to delete conversation: " + conversationId);
        }
        deleteConversation(conversationId);
    }
}
