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

    /**
     * 从暂停节点恢复 DAG 执行
     *
     * @param dagGraph       DAG 图
     * @param conversationId 会话ID
     * @param emitter        SSE 响应流
     * @param agentId        Agent ID
     * @param approved       是否批准
     * @param modifiedOutput 修改后的输出
     * @return 执行结果
     */
    public DagExecutor.DagExecutionResult resumeDag(
            DagGraph dagGraph,
            String conversationId,
            ResponseBodyEmitter emitter,
            String agentId,
            boolean approved,
            String modifiedOutput) {

        // 创建新的上下文
        DagExecutionContext context = new DagExecutionContext(conversationId, emitter, Long.valueOf(agentId));

        // 设置人工审核结果到上下文
        if (modifiedOutput != null) {
            context.getHumanInterventionData().setReviewResult(approved, null, modifiedOutput);
        } else {
            context.getHumanInterventionData().setReviewResult(approved, null);
        }

        // 调用执行器恢复执行
        return dagExecutor.resumeFromPause(dagGraph, context);
    }
}
