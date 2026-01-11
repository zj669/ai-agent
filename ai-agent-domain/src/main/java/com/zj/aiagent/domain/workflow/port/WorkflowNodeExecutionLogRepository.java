package com.zj.aiagent.domain.workflow.port;

import com.zj.aiagent.domain.workflow.entity.WorkflowNodeExecutionLog;

import java.util.List;

/**
 * 节点执行日志仓储接口
 */
public interface WorkflowNodeExecutionLogRepository {

    /**
     * 保存执行日志
     */
    void save(WorkflowNodeExecutionLog log);

    /**
     * 根据执行ID查询所有节点日志
     */
    List<WorkflowNodeExecutionLog> findByExecutionId(String executionId);

    /**
     * 查询单个节点的执行日志
     */
    WorkflowNodeExecutionLog findByExecutionIdAndNodeId(String executionId, String nodeId);
}
