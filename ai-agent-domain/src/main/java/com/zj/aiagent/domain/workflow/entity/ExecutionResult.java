package com.zj.aiagent.domain.workflow.entity;

import com.zj.aiagent.shared.design.workflow.WorkflowState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
@Builder
public class ExecutionResult {
    private String nodeId;
    private String error;
    /**
     * 暂停原因
     */
    private String pauseReason;
    /**
     * 元数据
     */
    private Object metadata;

    public static ExecutionResult error(String nodeId, String message) {
        return ExecutionResult.builder()
                .nodeId(nodeId)
                .error(message)
                .build();
    }

    public static ExecutionResult success(WorkflowState initialState) {
        return ExecutionResult.builder()
                .build();
    }

    public static ExecutionResult pause(String nodeId) {
        return ExecutionResult.builder()
                .nodeId(nodeId)
                .build();
    }

    /**
     * 等待人工审核
     *
     * @param nodeId 节点 ID
     * @param timing 时机（BEFORE/AFTER）
     */
    public static ExecutionResult waitingForHumanReview(String nodeId, String timing) {
        return ExecutionResult.builder()
                .nodeId(nodeId)
                .pauseReason("HUMAN_INTERVENTION_" + timing)
                .metadata(Map.of("timing", timing))
                .build();
    }

    /**
     * 等待限流恢复
     */
    public static ExecutionResult waitingForRateLimit(String nodeId) {
        return ExecutionResult.builder()
                .nodeId(nodeId)
                .pauseReason("RATE_LIMIT")
                .build();
    }
}
