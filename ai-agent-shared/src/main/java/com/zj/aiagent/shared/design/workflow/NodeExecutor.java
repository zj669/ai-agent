package com.zj.aiagent.shared.design.workflow;

public interface NodeExecutor {
    String getNodeId();
    String getNodeName();
    StateUpdate execute(WorkflowState state);
}
