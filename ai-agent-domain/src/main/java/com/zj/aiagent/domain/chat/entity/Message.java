package com.zj.aiagent.domain.chat.entity;

import com.zj.aiagent.domain.chat.valobj.MessageRole;
import com.zj.aiagent.domain.chat.valobj.MessageStatus;
import com.zj.aiagent.domain.chat.valobj.ThoughtStep;
import com.zj.aiagent.domain.chat.valobj.Citation;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 消息实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    private String id;
    private String conversationId;
    private MessageRole role;
    private String content;

    /**
     * 思维链过程 (递归结构)
     */
    private List<ThoughtStep> thoughtProcess;

    /**
     * 引用源
     */
    private List<Citation> citations;

    /**
     * 元数据 (runId, artifactIds, tokenCount等)
     */
    private Map<String, Object> metadata;

    private MessageStatus status;
    private LocalDateTime createdAt;

    /**
     * 初始化 Assistant 消息 (Pending状态)
     */
    public static Message initAssistant(String conversationId, String runId) {
        return Message.builder()
                .id(UUID.randomUUID().toString())
                .conversationId(conversationId)
                .role(MessageRole.ASSISTANT)
                .status(MessageStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .metadata(Map.of("runId", runId))
                .build();
    }
}
