package com.zj.aiagent.infrastructure.listener;

import com.alibaba.fastjson.JSON;
import com.zj.aiagent.domain.memory.entity.NodeExecutionRecord;
import com.zj.aiagent.domain.memory.repository.ChatHistoryRepository;
import com.zj.aiagent.shared.constants.WorkflowRunningConstants;
import com.zj.aiagent.shared.design.workflow.WorkflowState;
import com.zj.aiagent.shared.design.workflow.WorkflowStateListener;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天历史保存监听器
 * <p>
 * 监听工作流节点执行事件，自动保存节点执行日志到数据库
 * <p>
 * 这是基础设施层的实现，避免了 workflow 领域直接依赖 memory 领域
 * <p>
 * 通过配置 `memory.storage=database` 启用
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "memory.storage", havingValue = "database")
@AllArgsConstructor
public class ChatHistorySaveListener implements WorkflowStateListener {

    private final ChatHistoryRepository chatHistoryRepository;

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
     * 使用 Map 替代 ThreadLocal，以 executionId 为键
     * 这样可以支持异步执行的场景
     */
    private final Map<String, NodeExecutionContext> contextMap = new ConcurrentHashMap<>();

    @Override
    public void onWorkflowStarted(int totalNodes) {
        // 聊天历史保存不需要处理工作流级别事件
        log.debug("[ChatHistorySave] 工作流开始: totalNodes={}", totalNodes);
    }

    @Override
    public void onWorkflowCompleted(boolean success) {
        // 清理所有上下文
        int size = contextMap.size();
        contextMap.clear();
        log.debug("[ChatHistorySave] 工作流完成，清理 {} 个节点上下文", size);
    }

    @Override
    public void onWorkflowFailed(Throwable t) {
        // 清理所有上下文
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

        // 使用 nodeId 作为键（简化版，实际可以用 executionId + nodeId）
        contextMap.put(nodeId, context);

        log.debug("[ChatHistorySave] 节点开始: nodeId={}, nodeName={}", nodeId, nodeName);
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
            // 从 WorkflowState 获取必要信息
            String conversationId = result.get(WorkflowRunningConstants.Workflow.EXECUTION_ID_KEY, String.class);

            // instanceId 可能在不同的键中，尝试多个可能的键
            Long instanceId = getInstanceId(result);
            String agentId = result.get("agentId", String.class);

            if (conversationId == null) {
                log.warn("[ChatHistorySave] 缺少 conversationId，跳过保存: nodeId={}", nodeId);
                return;
            }

            if (instanceId == null) {
                log.debug("[ChatHistorySave] 缺少 instanceId，尝试使用 conversationId: nodeId={}", nodeId);
                // 某些场景下可能没有独立的 instanceId，可以使用 conversationId 的 hash
                instanceId = (long) conversationId.hashCode();
            }

            // 构建节点执行记录
            LocalDateTime endTime = LocalDateTime.now();

            NodeExecutionRecord record = NodeExecutionRecord.builder()
                    .nodeId(nodeId)
                    .nodeName(nodeName) // 使用参数传入的nodeName
                    .executeStatus("SUCCESS") // 到这里说明执行成功
                    .startTime(context.startTime)
                    .agentId(result.get(WorkflowRunningConstants.Workflow.AGENT_ID_KEY))
                    .endTime(endTime)
                    .durationMs(durationMs) // 使用参数传入的duration
                    .outputData(buildOutputData(context, result))
                    .build();

            // 保存到数据库
            chatHistoryRepository.saveNodeExecution(conversationId, instanceId, record);

            log.debug("[ChatHistorySave] 节点执行记录已保存: nodeId={}, conversationId={}, duration={}ms",
                    nodeId, conversationId, durationMs);

        } catch (Exception e) {
            log.error("[ChatHistorySave] 保存节点执行记录失败: nodeId={}", nodeId, e);
        } finally {
            // 清理上下文
            contextMap.remove(nodeId);
        }
    }

    @Override
    public void onNodeFailed(String nodeId, String nodeName, String error, long durationMs) {
        // 可以选择保存失败记录
        log.warn("[ChatHistorySave] 节点失败: nodeId={}, nodeName={}, error={}", nodeId, nodeName, error);
        contextMap.remove(nodeId);
    }

    @Override
    public void onNodePaused(String nodeId, String nodeName, String message) {
        // 暂停状态不需要保存
        log.debug("[ChatHistorySave] 节点暂停: nodeId={}, nodeName={}, message={}", nodeId, nodeName, message);
    }

    @Override
    public void onFinalAnswer(String contentChunk) {
        // 最终回复由SSE监听器处理，这里不需要保存
        log.trace("[ChatHistorySave] 最终回复 chunk: {} 字符", contentChunk.length());
    }

    /**
     * 尝试从多个可能的位置获取 instanceId
     */
    private Long getInstanceId(WorkflowState state) {
        // 尝试直接获取
        Long instanceId = state.get("instanceId", Long.class);
        if (instanceId != null) {
            return instanceId;
        }

        // 尝试从 metadata 获取
        Object metadata = state.get("metadata");
        if (metadata instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> metadataMap = (Map<String, Object>) metadata;
            Object idObj = metadataMap.get("instanceId");
            if (idObj instanceof Long) {
                return (Long) idObj;
            } else if (idObj instanceof Integer) {
                return ((Integer) idObj).longValue();
            }
        }

        return null;
    }

    /**
     * 构建输出数据
     */
    private String buildOutputData(NodeExecutionContext context, WorkflowState result) {
        // 优先使用流式内容
        if (context.streamingContent.length() > 0) {
            return context.streamingContent.toString();
        }

        // 否则使用 State 中的数据（序列化为 JSON）
        try {
            return JSON.toJSONString(result.getAll());
        } catch (Exception e) {
            log.warn("序列化 WorkflowState 失败: {}", e.getMessage());
            return "";
        }
    }
}
