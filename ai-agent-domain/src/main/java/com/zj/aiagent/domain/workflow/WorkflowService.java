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
    public void execute(WorkflowGraph graph, String conversationId, WorkflowStateListener listener) {
        WorkflowState workflowState = new WorkflowState(listener);
        workflowState.put(WorkflowRunningConstants.Workflow.EXECUTION_ID_KEY, conversationId);
        workflowScheduler.execute(graph, workflowState);
    }

    @Override
    public void resume(WorkflowGraph graph, String conversationId, String fromNodeId, WorkflowStateListener listener) {
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

            // 3. 调用调度器的resume方法
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
}
