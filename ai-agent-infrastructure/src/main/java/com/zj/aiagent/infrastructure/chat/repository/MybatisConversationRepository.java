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
        // MyBatis Plus: 如果主键存在则更新，否则插入
        // 使用 insertOrUpdate 需要配置主键策略，这里简化为判断是否存在
        ConversationDO existing = conversationMapper.selectById(conversation.getId());
        if (existing != null) {
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
        // MyBatis Plus: 如果主键存在则更新，否则插入
        MessageDO existing = messageMapper.selectById(message.getId());
        if (existing != null) {
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
        Page<MessageDO> page = new Page<>(pageable.getPageNumber() + 1, pageable.getPageSize());
        LambdaQueryWrapper<MessageDO> wrapper = new LambdaQueryWrapper<MessageDO>()
                .eq(MessageDO::getConversationId, conversationId);

        // 根据 Pageable 的排序规则决定查询顺序
        // 默认按创建时间正序（历史记录从旧到新）
        // 同一秒内的消息用 id（雪花算法，天然有序）作为二级排序
        if (pageable.getSort().isSorted()) {
            pageable.getSort().forEach(order -> {
                if ("createdAt".equals(order.getProperty())) {
                    if (order.isAscending()) {
                        wrapper.orderByAsc(MessageDO::getCreatedAt);
                        wrapper.orderByAsc(MessageDO::getId);
                    } else {
                        wrapper.orderByDesc(MessageDO::getCreatedAt);
                        wrapper.orderByDesc(MessageDO::getId);
                    }
                }
            });
        } else {
            wrapper.orderByAsc(MessageDO::getCreatedAt);
            wrapper.orderByAsc(MessageDO::getId);
        }

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
