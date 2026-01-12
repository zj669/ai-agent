package com.zj.aiagent.infrastructure.chat.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zj.aiagent.domain.chat.entity.Conversation;
import com.zj.aiagent.domain.chat.entity.Message;
import com.zj.aiagent.domain.chat.port.ConversationRepository;
import com.zj.aiagent.infrastructure.chat.mapper.ConversationMapper;
import com.zj.aiagent.infrastructure.chat.mapper.MessageMapper;
import com.zj.aiagent.infrastructure.chat.persistence.entity.ConversationDO;
import com.zj.aiagent.infrastructure.chat.persistence.entity.MessageDO;
import com.zj.aiagent.shared.response.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MybatisConversationRepository implements ConversationRepository {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;

    @Override
    public Conversation save(Conversation conversation) {
        ConversationDO conversationDO = new ConversationDO();
        BeanUtils.copyProperties(conversation, conversationDO);
        // 如果 ID 存在则更新，否则插入 (简化逻辑，实际可能需要判断)
        // 这里假设调用方已正确处理 ID
        if (conversationMapper.selectById(conversation.getId()) != null) {
            conversationMapper.updateById(conversationDO);
        } else {
            conversationMapper.insert(conversationDO);
        }
        return conversation;
    }

    @Override
    public Optional<Conversation> findById(String id) {
        ConversationDO conversationDO = conversationMapper.selectById(id);
        if (conversationDO == null || Boolean.TRUE.equals(conversationDO.getIsDeleted())) {
            return Optional.empty();
        }
        Conversation conversation = new Conversation();
        BeanUtils.copyProperties(conversationDO, conversation);
        return Optional.of(conversation);
    }

    @Override
    public void deleteById(String id) {
        ConversationDO conversationDO = new ConversationDO();
        conversationDO.setId(id);
        conversationDO.setIsDeleted(true);
        conversationMapper.updateById(conversationDO);
    }

    @Override
    public PageResult<Conversation> findByUserIdAndAgentId(String userId, String agentId, Pageable pageable) {
        Page<ConversationDO> page = new Page<>(pageable.getPageNumber() + 1, pageable.getPageSize()); // MyBatis Plus
                                                                                                      // Page uses
                                                                                                      // 1-based index
        LambdaQueryWrapper<ConversationDO> wrapper = new LambdaQueryWrapper<ConversationDO>()
                .eq(ConversationDO::getUserId, userId)
                .eq(ConversationDO::getAgentId, agentId)
                .eq(ConversationDO::getIsDeleted, false)
                .orderByDesc(ConversationDO::getUpdatedAt);

        Page<ConversationDO> result = conversationMapper.selectPage(page, wrapper);

        List<Conversation> list = result.getRecords().stream().map(doObj -> {
            Conversation c = new Conversation();
            BeanUtils.copyProperties(doObj, c);
            return c;
        }).collect(Collectors.toList());

        return new PageResult<>(result.getTotal(), result.getPages(), list);
    }

    @Override
    public Message saveMessage(Message message) {
        MessageDO messageDO = new MessageDO();
        BeanUtils.copyProperties(message, messageDO);

        if (messageMapper.selectById(message.getId()) != null) {
            messageMapper.updateById(messageDO);
        } else {
            messageMapper.insert(messageDO);
        }
        return message;
    }

    @Override
    public Optional<Message> findMessageById(String messageId) {
        MessageDO messageDO = messageMapper.selectById(messageId);
        if (messageDO == null) {
            return Optional.empty();
        }
        Message message = new Message();
        BeanUtils.copyProperties(messageDO, message);
        return Optional.of(message);
    }

    @Override
    public List<Message> findMessagesByConversationId(String conversationId, Pageable pageable) {
        // 注意：领域层接口返回 PageResult 或 List，这里简化为 List，
        // 实际可能需要使用 MyBatis Plus 的分页查询
        Page<MessageDO> page = new Page<>(pageable.getPageNumber() + 1, pageable.getPageSize());
        LambdaQueryWrapper<MessageDO> wrapper = new LambdaQueryWrapper<MessageDO>()
                .eq(MessageDO::getConversationId, conversationId)
                .orderByAsc(MessageDO::getCreatedAt); // 历史记录通常按时间正序

        return messageMapper.selectPage(page, wrapper).getRecords().stream().map(doObj -> {
            Message m = new Message();
            BeanUtils.copyProperties(doObj, m);
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public Optional<Message> findMessageByRunId(String runId) {
        // 使用 MyBatis Plus 的 json 字段查询特性 (MySQL 5.7+)
        // `meta_data` ->> '$.runId' = runId
        LambdaQueryWrapper<MessageDO> wrapper = new LambdaQueryWrapper<MessageDO>()
                .apply("meta_data ->> '$.runId' = {0}", runId)
                .last("LIMIT 1"); // 只要一条

        MessageDO messageDO = messageMapper.selectOne(wrapper);
        if (messageDO == null) {
            return Optional.empty();
        }
        Message message = new Message();
        BeanUtils.copyProperties(messageDO, message);
        return Optional.of(message);
    }

    @Override
    public void deleteMessagesByConversationId(String conversationId) {
        // 物理删除或软删除，取决于业务需求，这里简单做物理删除
        messageMapper.delete(new LambdaQueryWrapper<MessageDO>()
                .eq(MessageDO::getConversationId, conversationId));
    }
}
