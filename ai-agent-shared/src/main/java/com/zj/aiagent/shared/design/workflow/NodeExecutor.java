package com.zj.aiagent.shared.design.workflow;

public interface NodeExecutor {
    String getNodeId();
    String getNodeName();
    String getDescription();
    StateUpdate execute(WorkflowState state);
}
