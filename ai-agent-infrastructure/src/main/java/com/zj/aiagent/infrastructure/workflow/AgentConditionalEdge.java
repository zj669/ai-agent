package com.zj.aiagent.infrastructure.workflow;

import com.zj.aiagent.domain.workflow.entity.RouterEntity;
import com.zj.aiagent.domain.workflow.interfaces.ConditionalEdge;
import com.zj.aiagent.shared.design.workflow.WorkflowState;
import org.springframework.stereotype.Component;

import java.util.List;
@Component
public class AgentConditionalEdge implements ConditionalEdge {
    @Override
    public List<String> evaluate(WorkflowState state, List<RouterEntity> condition) {
        return List.of();
    }
}
