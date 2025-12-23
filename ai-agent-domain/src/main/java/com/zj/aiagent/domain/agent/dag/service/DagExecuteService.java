package com.zj.aiagent.domain.agent.dag.service;

import com.zj.aiagent.domain.agent.dag.context.DagExecutionContext;
import com.zj.aiagent.domain.agent.dag.entity.DagGraph;
import com.zj.aiagent.domain.agent.dag.executor.DagExecutor;
import com.zj.aiagent.domain.agent.dag.repository.IHumanInterventionRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

@Slf4j
@Service
public class DagExecuteService {
    @Resource
    private DagExecutor dagExecutor;

    @Resource
    private IHumanInterventionRepository humanInterventionRepository;

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
     * @return 执行结果
     */
    public DagExecutor.DagExecutionResult resumeDag(
            DagGraph dagGraph,
            String conversationId,
            ResponseBodyEmitter emitter,
            String agentId,
            boolean approved) {

        // 创建新的上下文
        DagExecutionContext context = new DagExecutionContext(conversationId, emitter, Long.valueOf(agentId));

        // 【新增】从 Redis 快照恢复完整上下文
        try {
            com.zj.aiagent.domain.agent.dag.context.ExecutionContextSnapshot snapshot = humanInterventionRepository
                    .loadContextSnapshot(conversationId);

            if (snapshot != null) {
                snapshot.restoreToContext(context);
                log.info("已从快照恢复执行上下文: conversationId={}, executedNodes={}",
                        conversationId, snapshot.getExecutedNodeIds().size());
            } else {
                log.warn("未找到快照，将使用空上下文: conversationId={}", conversationId);
            }
        } catch (Exception e) {
            log.warn("加载快照失败，将使用空上下文: conversationId={}", conversationId, e);
        }

        // 设置人工审核结果到上下文
        context.getHumanInterventionData().setReviewResult(approved, null);

        // 调用执行器恢复执行
        return dagExecutor.resumeFromPause(dagGraph, context);
    }
}
