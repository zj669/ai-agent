package com.zj.aiagent.domain.agent.dag.service;

import com.zj.aiagent.domain.agent.dag.context.ContextKey;
import com.zj.aiagent.domain.agent.dag.context.DagExecutionContext;
import com.zj.aiagent.domain.agent.dag.entity.DagGraph;
import com.zj.aiagent.domain.agent.dag.executor.DagExecutor;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

@Service
public class DagExecuteService {
    @Resource
    private DagExecutor dagExecutor;

    public DagExecutor.DagExecutionResult executeDag(DagGraph dagGraph, String conversationId, String userMessage,
            ResponseBodyEmitter emitter, String agentId) {
        DagExecutionContext context = new DagExecutionContext(conversationId, emitter, Long.valueOf(agentId));
        context.setUserInput(userMessage);
        return dagExecutor.execute(dagGraph, context);
    }
}
