package com.zj.aiagent.infrastructure.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天消息持久化对象
 * <p>
 * 对应表: ai_chat_message
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessagePO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * Agent ID
     */
    private Long agentId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 关联的执行实例ID（仅 assistant 消息）
     */
    private Long instanceId;

    /**
     * 消息角色: user, assistant
     */
    private String role;

    /**
     * 消息内容（用户消息原文）
     */
    private String content;

    /**
     * AI最终回复内容
     */
    private String finalResponse;

    /**
     * 是否有错误
     */
    private Boolean isError;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 消息时间戳（毫秒）
     */
    private Long timestamp;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
