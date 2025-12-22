package com.zj.aiagent.domain.agent.chat.entity;

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
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageEntity {

    private Long id;
    private String conversationId;
    private Long agentId;
    private Long userId;
    private Long instanceId;

    private MessageRole role;
    private String content; // 用户消息原文
    private String finalResponse; // AI 最终回复
    private Boolean isError;
    private String errorMessage;

    private Long timestamp;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 查询时填充的节点执行详情（从 ai_agent_execution_log 获取）
    private List<NodeExecutionInfo> nodeExecutions;

    /**
     * 消息角色枚举
     */
    public enum MessageRole {
        USER("user"),
        ASSISTANT("assistant");

        private final String value;

        MessageRole(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static MessageRole fromValue(String value) {
            for (MessageRole role : values()) {
                if (role.value.equalsIgnoreCase(value)) {
                    return role;
                }
            }
            return USER;
        }
    }

    /**
     * 节点执行信息
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NodeExecutionInfo {
        private String nodeId;
        private String nodeName;
        private String nodeType;
        private String executeStatus; // RUNNING, SUCCESS, FAILED
        private String outputData; // 节点输出内容
        private Long durationMs;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }
}
