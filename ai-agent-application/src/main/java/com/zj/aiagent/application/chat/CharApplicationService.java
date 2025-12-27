package com.zj.aiagent.application.chat;

import com.alibaba.fastjson.JSON;
import com.zj.aiagent.application.chat.command.ChatCommand;
import com.zj.aiagent.domain.workflow.IWorkflowService;
import com.zj.aiagent.domain.workflow.entity.WorkflowGraph;
import com.zj.aiagent.infrastructure.listener.SSEWorkflowStateListener;
import com.zj.aiagent.infrastructure.parse.WorkflowGraphFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
public class CharApplicationService implements ICharApplicationService {
    @Resource
    private IWorkflowService workflowService;
    @Resource
    private WorkflowGraphFactory workflowGraphFactory;
    @Resource
    private ExecutorService executorService;

    @Override
    public void chat(ChatCommand command) {
        WorkflowGraph graph = workflowGraphFactory.loadDagByAgentId(command.getAgentId());
        workflowService.execute(graph, command.getConversationId(),
                new SSEWorkflowStateListener(command.getEmitter(), command.getConversationId()));
    }

    @Override
    public List<String> queryHistoryId(Long userId, String agentId) {
        return List.of();
    }

    @Override
    public void review(Long userId, String conversationId, String nodeId, Boolean approved, String agentId,
            ResponseBodyEmitter emitter) {
        log.info("人工审核: userId={}, conversationId={}, nodeId={}, approved={}, agentId={}",
                userId, conversationId, nodeId, approved, agentId);

        // 异步执行，避免阻塞 HTTP 线程
        executorService.submit(() -> {
            try {
                if (Boolean.TRUE.equals(approved)) {
                    // 审批通过：恢复工作流执行
                    handleApproved(conversationId, nodeId, agentId, emitter);
                } else {
                    // 审批拒绝：发送拒绝事件并完成
                    handleRejected(conversationId, nodeId, emitter);
                }
            } catch (Exception e) {
                log.error("人工审核处理失败: conversationId={}", conversationId, e);
                sendErrorEvent(emitter, conversationId, e.getMessage());
            }
        });
    }

    /**
     * 处理审批通过：恢复工作流执行
     */
    private void handleApproved(String conversationId, String nodeId, String agentId, ResponseBodyEmitter emitter) {
        log.info("审批通过，恢复工作流执行: conversationId={}, nodeId={}", conversationId, nodeId);

        // 发送审批通过事件
        sendReviewEvent(emitter, conversationId, nodeId, true, "审批通过，正在恢复执行...");

        // 加载工作流图
        WorkflowGraph graph = workflowGraphFactory.loadDagByAgentId(agentId);

        // 创建 SSE 监听器
        SSEWorkflowStateListener listener = new SSEWorkflowStateListener(emitter, conversationId);

        // 恢复执行
        workflowService.resume(graph, conversationId, nodeId, listener);

        log.info("工作流恢复执行完成: conversationId={}", conversationId);
    }

    /**
     * 处理审批拒绝
     */
    private void handleRejected(String conversationId, String nodeId, ResponseBodyEmitter emitter) {
        log.info("审批拒绝: conversationId={}, nodeId={}", conversationId, nodeId);

        // 发送审批拒绝事件
        sendReviewEvent(emitter, conversationId, nodeId, false, "审批已拒绝");

        // 完成 emitter
        try {
            emitter.complete();
        } catch (Exception e) {
            log.warn("关闭 emitter 失败: {}", e.getMessage());
        }
    }

    /**
     * 发送审核结果事件
     */
    private void sendReviewEvent(ResponseBodyEmitter emitter, String conversationId, String nodeId,
            boolean approved, String message) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "review_result");
        event.put("status", approved ? "approved" : "rejected");
        event.put("nodeId", nodeId);
        event.put("message", message);
        event.put("timestamp", System.currentTimeMillis());
        event.put("conversationId", conversationId);

        try {
            String eventData = "data: " + JSON.toJSONString(event) + "\n\n";
            emitter.send(eventData);
        } catch (Exception e) {
            log.warn("发送审核事件失败: {}", e.getMessage());
        }
    }

    /**
     * 发送错误事件
     */
    private void sendErrorEvent(ResponseBodyEmitter emitter, String conversationId, String errorMessage) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "error");
        event.put("status", "failed");
        event.put("content", errorMessage);
        event.put("timestamp", System.currentTimeMillis());
        event.put("conversationId", conversationId);

        try {
            String eventData = "data: " + JSON.toJSONString(event) + "\n\n";
            emitter.send(eventData);
            emitter.complete();
        } catch (Exception e) {
            log.warn("发送错误事件失败: {}", e.getMessage());
        }
    }
}
