package com.zj.aiagent.infrastructure.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.memory.entity.NodeExecutionRecord;
import com.zj.aiagent.domain.memory.repository.ChatHistoryRepository;
import com.zj.aiagent.shared.constants.WorkflowRunningConstants;
import com.zj.aiagent.shared.design.workflow.WorkflowState;
import com.zj.aiagent.shared.design.workflow.WorkflowStateListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天历史保存监听器
 * <p>
 * 监听工作流节点执行事件，自动保存节点执行日志到数据库
 * 同时触发记忆提取事件
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "memory.storage", havingValue = "database")
@RequiredArgsConstructor
public class ChatHistorySaveListener implements WorkflowStateListener {

    private final ChatHistoryRepository chatHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final com.zj.aiagent.domain.memory.MemoryProvider memoryProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 节点执行上下文（临时存储，用于关联 onNodeStarted 和 onNodeCompleted）
     */
    private static class NodeExecutionContext {
        String nodeId;
        String nodeName;
        LocalDateTime startTime;
        StringBuilder streamingContent = new StringBuilder();
    }

    /**
     * 工作流执行上下文(临时存储,用于在工作流完成时保存最终回答)
     */
    private static class WorkflowExecutionContext {
        String conversationId;
        WorkflowState finalState;
        Long agentId;
        Long instanceId; // 新增:用于关联节点执行记录
    }

    /**
     * 使用 Map 替代 ThreadLocal，以 executionId 为键
     */
    private final Map<String, NodeExecutionContext> contextMap = new ConcurrentHashMap<>();
    private final Map<String, WorkflowExecutionContext> workflowContextMap = new ConcurrentHashMap<>();

    @Override
    public void onWorkflowStarted(int totalNodes) {
        log.debug("[ChatHistorySave] 工作流开始: totalNodes={}", totalNodes);
    }

    @Override
    public void onWorkflowCompleted(boolean success) {
        // 如果工作流成功完成,保存AI的最终回答
        if (success && memoryProvider != null) {
            // 遍历所有工作流上下文,保存最终回答
            for (WorkflowExecutionContext workflowContext : workflowContextMap.values()) {
                try {
                    saveFinalAnswer(workflowContext);
                } catch (Exception e) {
                    log.error("[ChatHistorySave] 保存最终回答失败: conversationId={}",
                            workflowContext.conversationId, e);
                }
            }
        }

        // 清理上下文
        int nodeContextSize = contextMap.size();
        int workflowContextSize = workflowContextMap.size();
        contextMap.clear();
        workflowContextMap.clear();
        log.debug("[ChatHistorySave] 工作流完成,清理 {} 个节点上下文和 {} 个工作流上下文",
                nodeContextSize, workflowContextSize);
    }

    @Override
    public void onWorkflowFailed(Throwable t) {
        int size = contextMap.size();
        contextMap.clear();
        log.warn("[ChatHistorySave] 工作流失败，清理 {} 个节点上下文", size);
    }

    @Override
    public void onNodeStarted(String nodeId, String nodeName) {
        NodeExecutionContext context = new NodeExecutionContext();
        context.nodeId = nodeId;
        context.nodeName = nodeName;
        context.startTime = LocalDateTime.now();
        contextMap.put(nodeId, context);
    }

    @Override
    public void onNodeStreaming(String nodeId, String nodeName, String contentChunk) {
        NodeExecutionContext context = contextMap.get(nodeId);
        if (context != null) {
            context.streamingContent.append(contentChunk);
        }
    }

    @Override
    public void onNodeCompleted(String nodeId, String nodeName, WorkflowState result, long durationMs) {
        NodeExecutionContext context = contextMap.get(nodeId);
        if (context == null) {
            log.warn("[ChatHistorySave] 节点完成但无上下文: nodeId={}", nodeId);
            return;
        }

        try {
            String conversationId = result.get(WorkflowRunningConstants.Workflow.EXECUTION_ID_KEY, String.class);
            Long instanceId = getInstanceId(result);
            String agentId = result.get(WorkflowRunningConstants.Workflow.AGENT_ID_KEY, String.class);
            String userId = result.get(WorkflowRunningConstants.Workflow.USER_ID_KEY, String.class);

            if (conversationId == null) {
                log.warn("[ChatHistorySave] 缺少 conversationId,跳过保存: nodeId={}", nodeId);
                return;
            }

            if (instanceId == null) {
                instanceId = (long) conversationId.hashCode();
            }

            // 保存工作流上下文信息（用于最终回答保存）
            WorkflowExecutionContext workflowContext = workflowContextMap.computeIfAbsent(
                    conversationId, k -> new WorkflowExecutionContext());
            workflowContext.conversationId = conversationId;
            workflowContext.finalState = result;
            workflowContext.instanceId = instanceId; // 保存instanceId
            if (agentId != null) {
                try {
                    workflowContext.agentId = Long.parseLong(agentId);
                } catch (NumberFormatException e) {
                    log.warn("[ChatHistorySave] agentId 格式错误: {}", agentId);
                }
            }

            // 保存节点执行记录
            NodeExecutionRecord record = NodeExecutionRecord.builder()
                    .nodeId(nodeId)
                    .nodeName(nodeName)
                    .executeStatus("SUCCESS")
                    .startTime(context.startTime)
                    .agentId(agentId)
                    .endTime(LocalDateTime.now())
                    .durationMs(durationMs)
                    .outputData(buildOutputData(context, result))
                    .build();

            chatHistoryRepository.saveNodeExecution(conversationId, instanceId, record);

        } catch (Exception e) {
            log.error("[ChatHistorySave] 保存节点记录/触发记忆提取失败: nodeId={}", nodeId, e);
        } finally {
            contextMap.remove(nodeId);
        }
    }

    @Override
    public void onNodeFailed(String nodeId, String nodeName, String error, long durationMs) {
        contextMap.remove(nodeId);
    }

    @Override
    public void onNodePaused(String nodeId, String nodeName, String message) {
        // No op
    }

    @Override
    public void onFinalAnswer(String contentChunk) {
        // No op
    }

    private Long getInstanceId(WorkflowState state) {
        Long instanceId = state.get("instanceId", Long.class);
        if (instanceId != null)
            return instanceId;

        Object metadata = state.get("metadata");
        if (metadata instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> metadataMap = (Map<String, Object>) metadata;
            Object idObj = metadataMap.get("instanceId");
            if (idObj instanceof Number) {
                return ((Number) idObj).longValue();
            }
        }
        return null;
    }

    private String buildOutputData(NodeExecutionContext context, WorkflowState result) {
        if (context.streamingContent.length() > 0) {
            return context.streamingContent.toString();
        }
        try {
            return objectMapper.writeValueAsString(result.getAll());
        } catch (Exception e) {
            log.warn("序列化 WorkflowState 失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 保存AI的最终回答
     */
    private void saveFinalAnswer(WorkflowExecutionContext workflowContext) {
        if (workflowContext.finalState == null || workflowContext.conversationId == null) {
            log.debug("[ChatHistorySave] 工作流上下文不完整,跳过保存最终回答");
            return;
        }

        // 从 WorkflowState 中提取最终答案
        String finalAnswer = extractFinalAnswer(workflowContext.finalState);

        if (finalAnswer == null || finalAnswer.trim().isEmpty()) {
            log.debug("[ChatHistorySave] 未找到最终答案,跳过保存");
            return;
        }

        // 创建 assistant 消息
        com.zj.aiagent.domain.memory.entity.ChatMessage assistantMessage = com.zj.aiagent.domain.memory.entity.ChatMessage
                .builder()
                .conversationId(workflowContext.conversationId)
                .agentId(workflowContext.agentId)
                .instanceId(workflowContext.instanceId)
                .role("assistant")
                .content(finalAnswer)
                .timestamp(LocalDateTime.now())
                .isError(false)
                .build();

        // 保存消息
        memoryProvider.saveChatMessage(workflowContext.conversationId, assistantMessage);

        log.info("[ChatHistorySave] 保存AI最终回答成功: conversationId={}, 内容长度={}",
                workflowContext.conversationId, finalAnswer.length());
    }

    /**
     * 从 WorkflowState 中提取最终答案
     * <p>
     * 提取策略:
     * 1. 查找所有以 "_output" 结尾的字段
     * 2. 优先使用最后一个节点的输出
     * 3. 如果没有,尝试从常见字段获取: finalAnswer, response, answer
     */
    private String extractFinalAnswer(WorkflowState state) {
        Map<String, Object> allState = state.getAll();

        // 策略1: 查找所有 _output 字段
        String lastOutput = null;
        for (Map.Entry<String, Object> entry : allState.entrySet()) {
            String key = entry.getKey();
            if (key.endsWith("_output") && entry.getValue() != null) {
                Object value = entry.getValue();
                lastOutput = convertToString(value);
            }
        }

        if (lastOutput != null && !lastOutput.trim().isEmpty()) {
            return lastOutput;
        }

        // 策略2: 从常见字段获取
        String[] commonKeys = { "finalAnswer", "response", "answer", "result" };
        for (String key : commonKeys) {
            Object value = state.get(key);
            if (value != null) {
                String result = convertToString(value);
                if (result != null && !result.trim().isEmpty()) {
                    return result;
                }
            }
        }

        log.debug("[ChatHistorySave] 未找到最终答案, State keys: {}", allState.keySet());
        return null;
    }

    /**
     * 将对象转换为字符串
     */
    private String convertToString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return value.toString();
        }
    }
}
