package com.zj.aiagent.domain.agent.dag.service;

import com.zj.aiagent.domain.agent.dag.context.DagExecutionContext;
import com.zj.aiagent.domain.agent.dag.entity.DagGraph;
import com.zj.aiagent.domain.agent.dag.executor.DagExecutor;
import com.zj.aiagent.domain.agent.dag.repository.IHumanInterventionRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class DagExecuteService {
    @Resource
    private DagExecutor dagExecutor;

    @Resource
    private IHumanInterventionRepository humanInterventionRepository;

    /**
     * 保存正在执行的上下文（conversationId -> DagExecutionContext）
     * 用于取消执行时查找对应的上下文
     */
    private final ConcurrentHashMap<String, DagExecutionContext> activeContexts = new ConcurrentHashMap<>();

    /**
     * 获取执行上下文
     *
     * @param conversationId 会话ID
     * @return 执行上下文，如果不存在则返回 null
     */
    public DagExecutionContext getExecutionContext(String conversationId) {
        return activeContexts.get(conversationId);
    }

    public DagExecutor.DagExecutionResult executeDag(DagGraph dagGraph, String conversationId, String userMessage,
            ResponseBodyEmitter emitter, String agentId) {
        DagExecutionContext context = new DagExecutionContext(conversationId, emitter, Long.valueOf(agentId));
        context.setUserInput(userMessage);

        // 注册上下文
        activeContexts.put(conversationId, context);
        log.debug("注册执行上下文: conversationId={}", conversationId);

        try {
            return dagExecutor.execute(dagGraph, context);
        } finally {
            // 清理上下文
            activeContexts.remove(conversationId);
            log.debug("清理执行上下文: conversationId={}", conversationId);
        }
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

        // 注册上下文
        activeContexts.put(conversationId, context);
        log.debug("注册恢复执行上下文: conversationId={}", conversationId);

        try {
            // 调用执行器恢复执行
            return dagExecutor.resumeFromPause(dagGraph, context);
        } finally {
            // 清理上下文
            activeContexts.remove(conversationId);
            log.debug("清理恢复执行上下文: conversationId={}", conversationId);
        }
    }
}
