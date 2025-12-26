package com.zj.aiagent.domain.workflow;

import com.zj.aiagent.domain.workflow.entity.WorkflowGraph;
import com.zj.aiagent.domain.workflow.scheduler.WorkflowScheduler;
import com.zj.aiagent.shared.constants.WorkflowRunningConstants;
import com.zj.aiagent.shared.design.workflow.WorkflowState;
import com.zj.aiagent.shared.design.workflow.WorkflowStateListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkflowService implements IWorkflowService{
    private final WorkflowScheduler workflowScheduler;
    @Override
    public void execute(WorkflowGraph graph, String conversationId,  WorkflowStateListener listener) {
        WorkflowState workflowState = new WorkflowState(listener);
        workflowState.put(WorkflowRunningConstants.Workflow.EXECUTION_ID_KEY, conversationId);
        workflowScheduler.execute(graph, workflowState);
    }
}
