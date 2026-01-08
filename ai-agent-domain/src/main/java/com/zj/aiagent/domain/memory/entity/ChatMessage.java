package com.zj.aiagent.domain.memory.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天消息实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    /**
     * 消息ID（数据库主键）
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
     * 消息角色: user, assistant, system
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
     * 消息时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 是否有错误
     */
    private Boolean isError;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 消息元数据
     */
    private java.util.Map<String, Object> metadata;

    /**
     * 关联的节点执行记录（懒加载）
     * <p>
     * 仅在需要详细执行信息时加载
     */
    private List<NodeExecutionRecord> nodeExecutions;
}
