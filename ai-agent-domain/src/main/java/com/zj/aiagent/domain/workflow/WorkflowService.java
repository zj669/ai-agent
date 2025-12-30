package com.zj.aiagent.domain.workflow;

import com.zj.aiagent.domain.workflow.entity.ExecutionContextSnapshot;
import com.zj.aiagent.domain.workflow.entity.WorkflowGraph;
import com.zj.aiagent.domain.workflow.interfaces.Checkpointer;
import com.zj.aiagent.shared.constants.WorkflowRunningConstants;
import com.zj.aiagent.shared.design.workflow.WorkflowState;
import com.zj.aiagent.shared.design.workflow.WorkflowStateListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService implements IWorkflowService {
    private final com.zj.aiagent.domain.workflow.scheduler.WorkflowScheduler workflowScheduler;
    private final Checkpointer checkpointer;

    @Override
    public void execute(WorkflowGraph graph, String conversationId, WorkflowStateListener listener, String agentId,
            String userMessage) {
        WorkflowState workflowState = new WorkflowState(listener);
        workflowState.put(WorkflowRunningConstants.Workflow.EXECUTION_ID_KEY, conversationId);
        workflowState.put(WorkflowRunningConstants.Workflow.AGENT_ID_KEY, agentId);

        // 存储用户消息
        if (userMessage != null && !userMessage.isEmpty()) {
            workflowState.put(WorkflowRunningConstants.Context.USER_QUESTION_KEY, userMessage);
            workflowState.put(WorkflowRunningConstants.Prompt.USER_MESSAGE_KEY, userMessage);
            log.info("存储用户消息到 WorkflowState: {}",
                    userMessage.length() > 50 ? userMessage.substring(0, 50) + "..." : userMessage);
        }

        workflowScheduler.execute(graph, workflowState);
    }

    @Override
    public void resume(WorkflowGraph graph, String conversationId, String fromNodeId, WorkflowStateListener listener,
            String agentId) {
        log.info("恢复工作流执行, conversationId={}, fromNodeId={}", conversationId, fromNodeId);

        try {
            // 1. 从检查点加载暂停时的状态
            WorkflowState workflowState = checkpointer.loadAt(conversationId, fromNodeId);

            if (workflowState == null) {
                log.warn("未找到暂停状态, 尝试加载最后一次检查点: conversationId={}", conversationId);
                workflowState = checkpointer.load(conversationId);
            }

            if (workflowState == null) {
                throw new RuntimeException("未找到工作流状态: conversationId=" + conversationId);
            }

            // 2. 设置监听器（用于SSE推送）
            workflowState.setWorkflowStateListener(listener);

            // 3. 确保 agentId 存在
            if (workflowState.get(WorkflowRunningConstants.Workflow.AGENT_ID_KEY, String.class) == null) {
                workflowState.put(WorkflowRunningConstants.Workflow.AGENT_ID_KEY, agentId);
            }

            // 4. ⭐ 添加标记：这次执行是从人工审核恢复的，拦截器应该跳过
            workflowState.put("_SKIP_HUMAN_INTERVENTION_NODE_", fromNodeId);
            log.info("设置跳过人工干预标记，避免重复暂停: conversationId={}, nodeId={}",
                    conversationId, fromNodeId);

            // 5. 调用调度器的resume方法
            workflowScheduler.resume(graph, workflowState, fromNodeId);

            log.info("工作流恢复执行成功: conversationId={}", conversationId);

        } catch (Exception e) {
            log.error("恢复工作流执行失败: conversationId={}", conversationId, e);
            throw new RuntimeException("恢复执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    public ExecutionContextSnapshot getExecutionSnapshot(String conversationId) {
        log.info("领域服务: 获取执行快照, conversationId={}", conversationId);

        try {
            // 1. 从检查点加载最后执行的状态
            WorkflowState workflowState = checkpointer.load(conversationId);

            if (workflowState == null) {
                log.debug("未找到执行快照: conversationId={}", conversationId);
                return null;
            }

            // 2. 获取最后执行的节点ID
            String lastNodeId = checkpointer.getLastNodeId(conversationId);

            // 3. 构建快照对象（状态默认为COMPLETED，具体状态由应用层判断）
            return ExecutionContextSnapshot.builder()
                    .executionId(conversationId)
                    .lastNodeId(lastNodeId)
                    .stateData(workflowState.getAll())
                    .timestamp(System.currentTimeMillis())
                    .status("COMPLETED")
                    .build();

        } catch (Exception e) {
            log.error("获取执行快照失败: conversationId={}", conversationId, e);
            throw new RuntimeException("获取执行快照失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateExecutionSnapshot(String conversationId, String nodeId,
            java.util.Map<String, Object> stateData) {
        log.info("领域服务: 更新执行快照, conversationId={}, nodeId={}", conversationId, nodeId);

        try {
            // 1. ⭐ 先加载现有的快照数据（如果存在）
            WorkflowState workflowState = checkpointer.loadAt(conversationId, nodeId);

            // 2. 如果不存在，尝试加载最新的
            if (workflowState == null) {
                workflowState = checkpointer.load(conversationId);
            }

            // 3. 如果仍然不存在，创建新的
            if (workflowState == null) {
                log.warn("未找到现有快照，创建新的: conversationId={}", conversationId);
                workflowState = new WorkflowState(null);
            }

            // 4. ⭐ 合并更新：将新数据覆盖到现有状态中
            if (stateData != null && !stateData.isEmpty()) {
                stateData.forEach(workflowState::put);
                log.info("合并更新了 {} 个字段", stateData.size());
            }

            // 5. 调用 Checkpointer 保存更新后的状态
            checkpointer.update(conversationId, nodeId, workflowState);

            log.info("执行快照更新成功: conversationId={}, nodeId={}", conversationId, nodeId);

        } catch (Exception e) {
            log.error("更新执行快照失败: conversationId={}, nodeId={}", conversationId, nodeId, e);
            throw new RuntimeException("更新执行快照失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void cancel(String conversationId) {
        log.info("领域服务: 取消工作流执行, conversationId={}", conversationId);
        workflowScheduler.cancel(conversationId);
    }
}
