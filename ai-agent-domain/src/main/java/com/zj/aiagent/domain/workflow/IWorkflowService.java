package com.zj.aiagent.domain.workflow;

import com.zj.aiagent.domain.workflow.entity.WorkflowGraph;
import com.zj.aiagent.domain.workflow.interfaces.WorkflowStateListener;

public interface IWorkflowService {
    void execute(WorkflowGraph graph, String conversationId,  WorkflowStateListener listener);
}
