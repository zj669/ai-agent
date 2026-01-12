package com.zj.aiagent.domain.workflow.event;

import com.zj.aiagent.domain.workflow.valobj.ExecutionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工作流执行完成事件
 */
@Data
@Builder
public class ExecutionCompletedEvent {
    private String executionId;
    private String conversationId;
    private ExecutionStatus status;
    private Map<String, Object> outputs;
    private LocalDateTime completedAt;
}
