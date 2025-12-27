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
    @Resource
    private com.zj.aiagent.domain.memory.MemoryProvider memoryService;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.zj.aiagent.infrastructure.listener.ChatHistorySaveListener chatHistorySaveListener;

    @Override
    public void chat(ChatCommand command) {
        WorkflowGraph graph = workflowGraphFactory.loadDagByAgentId(command.getAgentId());

        // 创建 SSE 监听器
        SSEWorkflowStateListener sseListener = new SSEWorkflowStateListener(
                command.getEmitter(), command.getConversationId());

        // 如果启用了数据库存储，创建复合监听器
        com.zj.aiagent.shared.design.workflow.WorkflowStateListener listener;
        if (chatHistorySaveListener != null) {
            listener = new com.zj.aiagent.infrastructure.listener.CompositeWorkflowStateListener(
                    sseListener, chatHistorySaveListener);
            log.debug("[{}] 使用复合监听器: SSE + ChatHistorySave", command.getConversationId());
        } else {
            listener = sseListener;
            log.debug("[{}] 仅使用 SSE 监听器", command.getConversationId());
        }

        workflowService.execute(graph, command.getConversationId(), listener);
    }

    @Override
    public List<String> queryHistoryId(Long userId, String agentId) {
        log.info("查询历史会话ID: userId={}, agentId={}", userId, agentId);
        return memoryService.queryConversationIds(userId, agentId);
    }

    @Override
    public com.zj.aiagent.domain.workflow.entity.ExecutionContextSnapshot getSnapshot(
            Long userId, String agentId, String conversationId) {
        log.info("获取执行快照: userId={}, agentId={}, conversationId={}",
                userId, agentId, conversationId);

        // 调用 WorkflowService 获取快照
        return workflowService.getExecutionSnapshot(conversationId);
    }

    @Override
    public void updateSnapshot(Long userId, String agentId, String conversationId, String nodeId,
            java.util.Map<String, Object> stateData) {
        log.info("更新执行快照: userId={}, agentId={}, conversationId={}, nodeId={}",
                userId, agentId, conversationId, nodeId);

        // 调用 WorkflowService 更新快照
        workflowService.updateExecutionSnapshot(conversationId, nodeId, stateData);
    }

    @Override
    public List<com.zj.aiagent.domain.memory.dto.ChatHistoryDTO> queryHistory(Long userId, String agentId,
            String conversationId) {
        log.info("查询历史消息: userId={}, agentId={}, conversationId={}", userId, agentId, conversationId);

        // 1. 从 MemoryService 加载带节点执行详情的消息
        List<com.zj.aiagent.domain.memory.entity.ChatMessage> messages = memoryService
                .loadChatHistoryWithNodeExecutions(conversationId, 100);

        // 2. 转换为 DTO
        return messages.stream()
                .map(this::toChatHistoryDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * ChatMessage → ChatHistoryDTO
     */
    private com.zj.aiagent.domain.memory.dto.ChatHistoryDTO toChatHistoryDTO(
            com.zj.aiagent.domain.memory.entity.ChatMessage msg) {

        com.zj.aiagent.domain.memory.dto.ChatHistoryDTO dto = com.zj.aiagent.domain.memory.dto.ChatHistoryDTO.builder()
                .role(msg.getRole())
                .content(msg.getContent())
                .timestamp(msg.getTimestamp() != null
                        ? msg.getTimestamp().toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
                        : null)
                .error(msg.getIsError())
                .build();

        // 转换节点执行记录
        if (msg.getNodeExecutions() != null && !msg.getNodeExecutions().isEmpty()) {
            List<com.zj.aiagent.domain.memory.dto.ChatHistoryDTO.NodeExecutionDTO> nodeDTOs = msg.getNodeExecutions()
                    .stream()
                    .map(this::toNodeExecutionDTO)
                    .collect(java.util.stream.Collectors.toList());
            dto.setNodes(nodeDTOs);
        }

        return dto;
    }

    /**
     * NodeExecutionRecord → NodeExecutionDTO
     */
    private com.zj.aiagent.domain.memory.dto.ChatHistoryDTO.NodeExecutionDTO toNodeExecutionDTO(
            com.zj.aiagent.domain.memory.entity.NodeExecutionRecord record) {

        return com.zj.aiagent.domain.memory.dto.ChatHistoryDTO.NodeExecutionDTO.builder()
                .nodeId(record.getNodeId())
                .nodeName(record.getNodeName())
                .status(record.getExecuteStatus())
                .content(record.getOutputData())
                .duration(record.getDurationMs())
                .build();
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
        SSEWorkflowStateListener sseListener = new SSEWorkflowStateListener(emitter, conversationId);

        // 如果启用了数据库存储，创建复合监听器
        com.zj.aiagent.shared.design.workflow.WorkflowStateListener listener;
        if (chatHistorySaveListener != null) {
            listener = new com.zj.aiagent.infrastructure.listener.CompositeWorkflowStateListener(
                    sseListener, chatHistorySaveListener);
            log.debug("[{}] Resume 使用复合监听器: SSE + ChatHistorySave", conversationId);
        } else {
            listener = sseListener;
            log.debug("[{}] Resume 仅使用 SSE 监听器", conversationId);
        }

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
