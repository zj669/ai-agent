package com.zj.aiagent.domain.chat.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSE 事件负载
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseEventPayload {
    private String executionId;
    private String nodeId;
    private String nodeType;
    private long timestamp;

    private RenderConfig renderConfig;

    /**
     * 是否为思考过程
     */
    private boolean isThought;

    /**
     * 增量文本内容
     */
    private String content;

    /**
     * 额外数据（如完整 inputs/outputs，调试用）
     */
    private Object data;
}
