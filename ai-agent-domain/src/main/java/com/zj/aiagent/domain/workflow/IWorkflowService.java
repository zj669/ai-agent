package com.zj.aiagent.domain.workflow;

import com.zj.aiagent.domain.workflow.entity.ExecutionContextSnapshot;
import com.zj.aiagent.domain.workflow.entity.WorkflowGraph;
import com.zj.aiagent.shared.design.workflow.WorkflowStateListener;

public interface IWorkflowService {
    /**
     * 执行工作流
     *
     * @param graph          工作流图
     * @param conversationId 会话ID
     * @param listener       状态监听器
     * @param agentId        Agent ID
     */
    void execute(WorkflowGraph graph, String conversationId, WorkflowStateListener listener, String agentId);

    /**
     * 恢复工作流执行
     * <p>
     * 从暂停点恢复工作流执行，通常在人工审核批准后调用
     *
     * @param graph          工作流图
     * @param conversationId 会话ID
     * @param fromNodeId     暂停的节点ID
     * @param listener       状态监听器
     * @param agentId        Agent ID
     */
    void resume(WorkflowGraph graph, String conversationId, String fromNodeId, WorkflowStateListener listener,
            String agentId);

    /**
     * 获取执行上下文快照
     * <p>
     * 获取指定会话的最后一次执行快照，包含状态数据、执行节点等信息
     *
     * @param conversationId 会话ID
     * @return 执行上下文快照，如果不存在则返回null
     */
    ExecutionContextSnapshot getExecutionSnapshot(String conversationId);

    /**
     * 更新执行快照
     *
     * @param conversationId 会话ID
     * @param nodeId         节点ID
     * @param stateData      更新后的状态数据
     */
    void updateExecutionSnapshot(String conversationId, String nodeId, java.util.Map<String, Object> stateData);

    /**
     * 取消工作流执行
     *
     * @param conversationId 会话ID
     */
    void cancel(String conversationId);
}
