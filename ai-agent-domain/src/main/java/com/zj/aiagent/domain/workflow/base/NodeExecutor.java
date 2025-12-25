package com.zj.aiagent.domain.workflow.base;

import com.zj.aiagent.domain.workflow.interfaces.WorkflowStateListener;

public interface NodeExecutor {
    String getNodeId();
    String getNodeName();
    StateUpdate execute(WorkflowState state);

    StateUpdate execute(WorkflowState state, WorkflowStateListener listener);
}
