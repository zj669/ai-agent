package com.zj.aiagent.infrastructure.parse.adpater;

import com.zj.aiagent.shared.design.workflow.NodeExecutor;
import com.zj.aiagent.shared.design.workflow.StateUpdate;
import com.zj.aiagent.shared.design.workflow.WorkflowState;
import lombok.Data;
import org.springframework.ai.openai.OpenAiChatModel;

@Data
public class WorkflowExecutorAdapter implements NodeExecutor {
    private String nodeId;
    private String nodeName;
    private OpenAiChatModel chatModel;
    private String prompt;
    private String nodeType;

    @Override
    public StateUpdate execute(WorkflowState state) {
        return null;
    }
}
