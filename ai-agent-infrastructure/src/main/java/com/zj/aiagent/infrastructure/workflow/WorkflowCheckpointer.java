package com.zj.aiagent.infrastructure.workflow;

import com.zj.aiagent.domain.workflow.interfaces.Checkpointer;
import com.zj.aiagent.shared.design.workflow.WorkflowState;
import org.springframework.stereotype.Component;

@Component
public class WorkflowCheckpointer implements Checkpointer {
    @Override
    public void save(String executionId, String nodeId, WorkflowState state) {

    }

    @Override
    public WorkflowState load(String executionId) {
        return null;
    }

    @Override
    public WorkflowState loadAt(String executionId, String nodeId) {
        return null;
    }

    @Override
    public String getLastNodeId(String executionId) {
        return "";
    }

    @Override
    public void clear(String executionId) {

    }
}
