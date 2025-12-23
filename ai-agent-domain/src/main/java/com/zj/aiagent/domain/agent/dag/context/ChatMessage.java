package com.zj.aiagent.domain.agent.dag.context;

import lombok.Data;

/**
 * 聊天消息实体
 * 用于消息历史累积（Reducer 模式）
 */
@Data
public class ChatMessage {
    /**
     * 消息角色：user, assistant, system
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 时间戳
     */
    private long timestamp;

    /**
     * 来源节点ID（可选）
     */
    private String sourceNodeId;

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    public ChatMessage(String role, String content, String sourceNodeId) {
        this.role = role;
        this.content = content;
        this.sourceNodeId = sourceNodeId;
        this.timestamp = System.currentTimeMillis();
    }
}
