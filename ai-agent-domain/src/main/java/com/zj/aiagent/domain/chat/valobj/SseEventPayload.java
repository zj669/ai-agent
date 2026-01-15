package com.zj.aiagent.domain.chat.valobj;

import com.zj.aiagent.domain.workflow.valobj.ExecutionStatus;
import com.zj.aiagent.domain.workflow.valobj.SseEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSE 事件负载
 * 支持层级关系、状态标识、事件语义和增量内容
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseEventPayload {

    // --- 身份标识 ---

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 节点ID
     */
    private String nodeId;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 父节点ID（并行组ID，支持树形渲染）
     */
    private String parentId;

    // --- 事件元数据 ---

    /**
     * 事件类型：START/UPDATE/FINISH/ERROR
     */
    private SseEventType eventType;

    /**
     * 节点状态：RUNNING/SUCCEEDED/FAILED 等
     */
    private ExecutionStatus status;

    /**
     * 时间戳
     */
    private long timestamp;

    // --- 内容载荷 ---

    /**
     * 内容载荷
     */
    private ContentPayload payload;

    /**
     * 内容载荷内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentPayload {

        /**
         * 步骤标题
         */
        private String title;

        /**
         * 完整内容（用于 FINISH 事件）
         */
        private String content;

        /**
         * 增量内容（流式专用，用于打字机效果）
         */
        private String delta;

        /**
         * 是否为思考过程
         */
        private boolean isThought;

        /**
         * 渲染模式：TEXT/MARKDOWN/JSON/TABLE/THOUGHT
         */
        private String renderMode;
    }
}
