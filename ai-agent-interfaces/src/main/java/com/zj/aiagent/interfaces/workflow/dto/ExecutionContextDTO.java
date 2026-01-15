package com.zj.aiagent.interfaces.workflow.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 执行上下文 DTO - 用于调试接口返回工作流执行的上下文快照
 */
@Data
public class ExecutionContextDTO {
    /**
     * 执行 ID
     */
    private String executionId;

    /**
     * 长期记忆 (LTM)
     */
    private List<String> longTermMemories;

    /**
     * 聊天历史 (STM - Short Term Memory)
     */
    private List<ChatMessage> chatHistory;

    /**
     * 执行日志/轨迹 (节点执行顺序)
     */
    private String executionLog;

    /**
     * 全局变量
     */
    private Map<String, Object> globalVariables;

    /**
     * 聊天消息
     */
    @Data
    public static class ChatMessage {
        private String role; // "user" | "assistant" | "system"
        private String content;
        private Long timestamp;
    }
}
