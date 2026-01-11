package com.zj.aiagent.domain.workflow.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 节点完成事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeCompletedEvent {

    private String executionId;
    private String nodeId;
    private String nodeName;
    private String nodeType;

    // Render Config
    private String renderMode; // HIDDEN, THOUGHT, MESSAGE

    private Integer status; // 0:Running, 1:Success, 2:Failed

    private Map<String, Object> inputs;
    private Map<String, Object> outputs;

    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
