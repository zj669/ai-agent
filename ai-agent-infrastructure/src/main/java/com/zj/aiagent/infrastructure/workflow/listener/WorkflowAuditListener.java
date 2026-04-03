package com.zj.aiagent.infrastructure.workflow.listener;

import com.zj.aiagent.domain.workflow.entity.WorkflowNodeExecutionLog;
import com.zj.aiagent.domain.workflow.event.NodeCompletedEvent;
import com.zj.aiagent.domain.workflow.port.WorkflowNodeExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 工作流审计监听器
 * 异步监听 NodeCompletedEvent 并记录执行日志
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowAuditListener {

    private final WorkflowNodeExecutionLogRepository executionLogRepository;

    @Async
    @EventListener
    public void handleNodeCompleted(NodeCompletedEvent event) {
        log.debug("[Audit] Processing NodeCompletedEvent for node: {}", event.getNodeId());

        try {
            WorkflowNodeExecutionLog executionLog = event.getExecutionLog();
            if (executionLog != null) {
                executionLogRepository.save(executionLog);
                log.debug("[Audit] Saved execution log for node: {}", event.getNodeId());
            } else {
                log.warn("[Audit] No execution log in event for node: {}", event.getNodeId());
            }
        } catch (Exception e) {
            log.error("[Audit] Failed to save execution log for node: {}", event.getNodeId(), e);
        }
    }
}
