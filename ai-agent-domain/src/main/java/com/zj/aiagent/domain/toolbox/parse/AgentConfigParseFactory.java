package com.zj.aiagent.domain.toolbox.parse;

import com.zj.aiagent.domain.agent.dag.entity.GraphJsonSchema;
import com.zj.aiagent.shared.design.workflow.NodeExecutor;
import org.springframework.stereotype.Component;

@Component
public class AgentConfigParseFactory {
    public NodeExecutor createNode(GraphJsonSchema.NodeDefinition nodeDef) {
        // todo
        return null;
    }
}
