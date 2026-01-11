package com.zj.aiagent.domain.workflow.valobj;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 检查点值对象
 * 用于持久化和恢复执行状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Checkpoint {

    /**
     * 检查点ID
     */
    private String checkpointId;

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 当前节点ID
     */
    private String currentNodeId;

    /**
     * 执行上下文快照
     */
    private ExecutionContext contextSnapshot;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 是否为暂停点
     */
    private boolean pausePoint;

    /**
     * 创建检查点
     */
    public static Checkpoint create(String executionId, String nodeId, ExecutionContext context) {
        return Checkpoint.builder()
                .checkpointId(executionId + "_" + nodeId + "_" + System.currentTimeMillis())
                .executionId(executionId)
                .currentNodeId(nodeId)
                .contextSnapshot(context.snapshot())
                .createdAt(LocalDateTime.now())
                .pausePoint(false)
                .build();
    }

    /**
     * 创建暂停点
     */
    public static Checkpoint createPausePoint(String executionId, String nodeId, ExecutionContext context) {
        Checkpoint checkpoint = create(executionId, nodeId, context);
        checkpoint.setPausePoint(true);
        return checkpoint;
    }
}
