package com.zj.aiagent.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.domain.agent.chat.entity.ChatMessageEntity;
import com.zj.aiagent.domain.agent.chat.repository.IChatMessageRepository;
import com.zj.aiagent.infrastructure.persistence.entity.AiAgentExecutionLogPO;
import com.zj.aiagent.infrastructure.persistence.entity.AiChatMessagePO;
import com.zj.aiagent.infrastructure.persistence.mapper.AiAgentExecutionLogMapper;
import com.zj.aiagent.infrastructure.persistence.mapper.AiChatMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天消息仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements IChatMessageRepository {

    private final AiChatMessageMapper chatMessageMapper;
    private final AiAgentExecutionLogMapper executionLogMapper;

    @Override
    public ChatMessageEntity save(ChatMessageEntity message) {
        AiChatMessagePO po = convertToPO(message);
        chatMessageMapper.insert(po);
        message.setId(po.getId());
        log.info("保存聊天消息成功: id={}, conversationId={}, role={}",
                po.getId(), po.getConversationId(), po.getRole());
        return message;
    }

    @Override
    public List<ChatMessageEntity> findByConversationId(String conversationId) {
        LambdaQueryWrapper<AiChatMessagePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatMessagePO::getConversationId, conversationId)
                .orderByAsc(AiChatMessagePO::getTimestamp);

        List<AiChatMessagePO> pos = chatMessageMapper.selectList(wrapper);
        log.info("查询会话消息: conversationId={}, count={}", conversationId, pos.size());

        return pos.stream().map(po -> {
            ChatMessageEntity entity = convertToEntity(po);
            // 对于 assistant 消息，查询节点执行详情
            if ("assistant".equals(po.getRole()) && po.getInstanceId() != null) {
                entity.setNodeExecutions(findNodeExecutionsByInstanceId(po.getInstanceId()));
            }
            return entity;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ChatMessageEntity.NodeExecutionInfo> findNodeExecutionsByInstanceId(Long instanceId) {
        LambdaQueryWrapper<AiAgentExecutionLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiAgentExecutionLogPO::getInstanceId, instanceId)
                .orderByAsc(AiAgentExecutionLogPO::getStartTime);

        List<AiAgentExecutionLogPO> logs = executionLogMapper.selectList(wrapper);
        log.debug("查询节点执行日志: instanceId={}, count={}", instanceId, logs.size());

        return logs.stream().map(log -> ChatMessageEntity.NodeExecutionInfo.builder()
                .nodeId(log.getNodeId())
                .nodeName(log.getNodeName())
                .nodeType(log.getNodeType())
                .executeStatus(log.getExecuteStatus())
                .outputData(log.getOutputData())
                .durationMs(log.getDurationMs())
                .startTime(log.getStartTime())
                .endTime(log.getEndTime())
                .build()).collect(Collectors.toList());
    }

    @Override
    public int countByConversationId(String conversationId) {
        LambdaQueryWrapper<AiChatMessagePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatMessagePO::getConversationId, conversationId);
        return chatMessageMapper.selectCount(wrapper).intValue();
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        LambdaQueryWrapper<AiChatMessagePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatMessagePO::getConversationId, conversationId);
        chatMessageMapper.delete(wrapper);
        log.info("删除会话消息: conversationId={}", conversationId);
    }

    private AiChatMessagePO convertToPO(ChatMessageEntity entity) {
        return AiChatMessagePO.builder()
                .id(entity.getId())
                .conversationId(entity.getConversationId())
                .agentId(entity.getAgentId())
                .userId(entity.getUserId())
                .instanceId(entity.getInstanceId())
                .role(entity.getRole().getValue())
                .content(entity.getContent())
                .finalResponse(entity.getFinalResponse())
                .isError(entity.getIsError())
                .errorMessage(entity.getErrorMessage())
                .timestamp(entity.getTimestamp())
                .build();
    }

    private ChatMessageEntity convertToEntity(AiChatMessagePO po) {
        return ChatMessageEntity.builder()
                .id(po.getId())
                .conversationId(po.getConversationId())
                .agentId(po.getAgentId())
                .userId(po.getUserId())
                .instanceId(po.getInstanceId())
                .role(ChatMessageEntity.MessageRole.fromValue(po.getRole()))
                .content(po.getContent())
                .finalResponse(po.getFinalResponse())
                .isError(po.getIsError())
                .errorMessage(po.getErrorMessage())
                .timestamp(po.getTimestamp())
                .createTime(po.getCreateTime())
                .updateTime(po.getUpdateTime())
                .build();
    }
}
