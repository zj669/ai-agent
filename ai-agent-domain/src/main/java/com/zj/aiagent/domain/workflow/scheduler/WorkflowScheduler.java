package com.zj.aiagent.domain.workflow.scheduler;

import com.zj.aiagent.domain.workflow.entity.ExecutionResult;
import com.zj.aiagent.domain.workflow.entity.WorkflowGraph;
import com.zj.aiagent.shared.design.workflow.WorkflowState;

public interface WorkflowScheduler {

    /**
     * 执行工作流
     */
    ExecutionResult execute(WorkflowGraph graph, WorkflowState initialState);

    /**
     * 从暂停状态恢复执行
     */
    ExecutionResult resume(WorkflowGraph graph, WorkflowState state, String fromNodeId);

    /**
     * 取消执行
     */
    void cancel(String conversationId);
}
