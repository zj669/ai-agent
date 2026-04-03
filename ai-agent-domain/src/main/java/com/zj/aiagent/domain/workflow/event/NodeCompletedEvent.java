package com.zj.aiagent.domain.workflow.event;

import com.zj.aiagent.domain.workflow.entity.WorkflowNodeExecutionLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 节点完成事件
 * 持有 WorkflowNodeExecutionLog 引用，避免字段重复
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeCompletedEvent {

    private String executionId;
    private String nodeId;

    /**
     * 关联的执行日志引用（包含所有执行详情）
     */
    private WorkflowNodeExecutionLog executionLog;
}
