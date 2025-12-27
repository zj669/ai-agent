package com.zj.aiagent.infrastructure.converter;

import com.zj.aiagent.domain.memory.entity.ChatMessage;
import com.zj.aiagent.domain.memory.entity.NodeExecutionRecord;
import com.zj.aiagent.infrastructure.persistence.entity.AgentExecutionLogPO;
import com.zj.aiagent.infrastructure.persistence.entity.ChatMessagePO;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 聊天消息转换器
 * <p>
 * 负责 PO ↔ Entity 转换
 */
@Component
public class ChatMessageConverter {

    /**
     * ChatMessage Entity → PO
     */
    public ChatMessagePO toPO(ChatMessage entity) {
        if (entity == null) {
            return null;
        }

        return ChatMessagePO.builder()
                .id(entity.getId())
                .conversationId(entity.getConversationId())
                .agentId(entity.getAgentId())
                .userId(entity.getUserId())
                .instanceId(entity.getInstanceId())
                .role(entity.getRole())
                .content(entity.getContent())
                .finalResponse(entity.getFinalResponse())
                .isError(entity.getIsError())
                .errorMessage(entity.getErrorMessage())
                .timestamp(entity.getTimestamp() != null
                        ? entity.getTimestamp().toInstant(ZoneOffset.UTC).toEpochMilli()
                        : System.currentTimeMillis())
                .build();
    }

    /**
     * ChatMessagePO → Entity
     */
    public ChatMessage toEntity(ChatMessagePO po) {
        if (po == null) {
            return null;
        }

        return ChatMessage.builder()
                .id(po.getId())
                .conversationId(po.getConversationId())
                .agentId(po.getAgentId())
                .userId(po.getUserId())
                .instanceId(po.getInstanceId())
                .role(po.getRole())
                .content(po.getContent())
                .finalResponse(po.getFinalResponse())
                .isError(po.getIsError())
                .errorMessage(po.getErrorMessage())
                .timestamp(po.getTimestamp() != null
                        ? LocalDateTime.ofInstant(Instant.ofEpochMilli(po.getTimestamp()), ZoneOffset.UTC)
                        : null)
                .build();
    }

    /**
     * NodeExecutionRecord → AgentExecutionLogPO
     */
    public AgentExecutionLogPO toExecutionLogPO(NodeExecutionRecord record) {
        if (record == null) {
            return null;
        }

        return AgentExecutionLogPO.builder()
                .id(record.getId())
                .instanceId(record.getInstanceId())
                .nodeId(record.getNodeId())
                .nodeType(record.getNodeType())
                .nodeName(record.getNodeName())
                .executeStatus(record.getExecuteStatus())
                .inputData(record.getInputData())
                .outputData(record.getOutputData())
                .errorMessage(record.getErrorMessage())
                .startTime(record.getStartTime())
                .endTime(record.getEndTime())
                .durationMs(record.getDurationMs())
                .modelInfo(record.getModelInfo())
                .tokenUsage(record.getTokenUsage())
                .build();
    }

    /**
     * AgentExecutionLogPO → NodeExecutionRecord
     */
    public NodeExecutionRecord toNodeExecutionRecord(AgentExecutionLogPO po) {
        if (po == null) {
            return null;
        }

        return NodeExecutionRecord.builder()
                .id(po.getId())
                .instanceId(po.getInstanceId())
                .nodeId(po.getNodeId())
                .nodeType(po.getNodeType())
                .nodeName(po.getNodeName())
                .executeStatus(po.getExecuteStatus())
                .inputData(po.getInputData())
                .outputData(po.getOutputData())
                .errorMessage(po.getErrorMessage())
                .startTime(po.getStartTime())
                .endTime(po.getEndTime())
                .durationMs(po.getDurationMs())
                .modelInfo(po.getModelInfo())
                .tokenUsage(po.getTokenUsage())
                .build();
    }
}
