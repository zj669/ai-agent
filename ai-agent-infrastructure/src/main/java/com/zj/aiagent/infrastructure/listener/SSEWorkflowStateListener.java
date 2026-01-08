package com.zj.aiagent.infrastructure.listener;

import com.alibaba.fastjson.JSON;
import com.zj.aiagent.shared.design.workflow.WorkflowState;
import com.zj.aiagent.shared.design.workflow.WorkflowStateListener;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于SSE的工作流状态监听器实现
 * 严格遵循前端 AgentChat.tsx 的协议定义
 */
@Slf4j
public class SSEWorkflowStateListener implements WorkflowStateListener {

    private final ResponseBodyEmitter emitter;
    private final String conversationId;

    // 状态跟踪
    private int totalNodes = 0;
    private int completedNodes = 0;
    private final Map<String, Long> nodeStartTimes = new ConcurrentHashMap<>();

    public SSEWorkflowStateListener(ResponseBodyEmitter emitter, String conversationId) {
        this.emitter = emitter;
        this.conversationId = conversationId;
    }

    // ==================== 工作流级别事件 ====================

    @Override
    public void onWorkflowStarted(int totalNodes) {
        this.totalNodes = totalNodes;
        this.completedNodes = 0;

        Map<String, Object> event = new HashMap<>();
        event.put("type", "dag_start");
        event.put("conversationId", conversationId);
        event.put("totalNodes", totalNodes);

        sendEvent(event);
        log.info("[{}] 工作流开始，总节点数: {}", conversationId, totalNodes);
    }

    @Override
    public void onWorkflowCompleted(boolean success) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "dag_complete");
        event.put("status", success ? "success" : "failed");

        sendEvent(event);
        log.info("[{}] 工作流完成，状态: {}", conversationId, success ? "成功" : "失败");

        // 延迟关闭emitter，确保事件已完全发送到客户端
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 完成后关闭 emitter
        try {
            if (emitter != null) {
                emitter.complete();
            }
        } catch (Exception e) {
            log.warn("[{}] 关闭 emitter 失败: {}", conversationId, e.getMessage());
        }
    }

    @Override
    public void onWorkflowFailed(Throwable t) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "error");
        event.put("errorCode", "WORKFLOW_ERROR");
        event.put("message", t != null ? t.getMessage() : "Unknown error");

        sendEvent(event);
        log.error("[{}] 工作流失败: {}", conversationId, t != null ? t.getMessage() : "Unknown", t);

        // 延迟关闭确保事件发送
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 失败后也要完成emitter
        try {
            if (emitter != null) {
                emitter.completeWithError(t);
            }
        } catch (Exception e) {
            log.warn("[{}] 完成 emitter 失败: {}", conversationId, e.getMessage());
        }
    }

    // ==================== 节点级别事件 ====================

    @Override
    public void onNodeStarted(String nodeId, String nodeName) {
        // 记录开始时间用于计算duration
        nodeStartTimes.put(nodeId, System.currentTimeMillis());

        Map<String, Object> event = new HashMap<>();
        event.put("type", "node_lifecycle");
        event.put("status", "starting");
        event.put("nodeId", nodeId);
        event.put("nodeName", nodeName);
        event.put("timestamp", System.currentTimeMillis());

        sendEvent(event);
        log.debug("[{}] 节点开始: {} ({})", conversationId, nodeName, nodeId);
    }

    @Override
    public void onNodeStreaming(String nodeId, String nodeName, String contentChunk) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "node_execute");
        event.put("nodeName", nodeName); // 前端用nodeName匹配节点
        event.put("content", contentChunk);

        sendEvent(event);
        log.trace("[{}] 节点流式输出: {} - {} 字符", conversationId, nodeName, contentChunk.length());
    }

    @Override
    public void onNodeCompleted(String nodeId, String nodeName, WorkflowState result, long durationMs) {
        completedNodes++;

        // 计算进度
        Map<String, Object> progress = new HashMap<>();
        progress.put("current", completedNodes);
        progress.put("total", totalNodes);
        progress.put("percentage", totalNodes > 0 ? (completedNodes * 100.0 / totalNodes) : 0);

        Map<String, Object> event = new HashMap<>();
        event.put("type", "node_lifecycle");
        event.put("status", "completed");
        event.put("nodeId", nodeId);
        event.put("durationMs", durationMs);
        event.put("result", result != null ? JSON.toJSONString(result.getAll()) : "");
        event.put("progress", progress);

        sendEvent(event);
        log.debug("[{}] 节点完成: {} ({}) - {}ms, 进度: {}/{}",
                conversationId, nodeName, nodeId, durationMs, completedNodes, totalNodes);

        // 清理开始时间
        nodeStartTimes.remove(nodeId);
    }

    @Override
    public void onNodeFailed(String nodeId, String nodeName, String error, long durationMs) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "node_lifecycle");
        event.put("status", "failed");
        event.put("nodeId", nodeId);
        event.put("durationMs", durationMs);
        event.put("result", error);

        sendEvent(event);
        log.error("[{}] 节点失败: {} ({}) - {}", conversationId, nodeName, nodeId, error);

        // 清理开始时间
        nodeStartTimes.remove(nodeId);
    }

    @Override
    public void onNodePaused(String nodeId, String nodeName, String message) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "node_lifecycle");
        event.put("status", "paused");
        event.put("nodeId", nodeId);
        event.put("nodeName", nodeName);
        event.put("result", "WAITING_FOR_HUMAN:" + message);

        sendEvent(event);
        log.info("[{}] 节点暂停等待人工介入: {} ({}) - {}", conversationId, nodeName, nodeId, message);
    }

    // ==================== 用户交互事件 ====================

    @Override
    public void onFinalAnswer(String contentChunk) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "token"); // 前端支持 token 或 answer
        event.put("content", contentChunk);

        sendEvent(event);
        log.trace("[{}] 最终回复: {} 字符", conversationId, contentChunk.length());
    }

    // ==================== 内部工具方法 ====================

    /**
     * 发送SSE事件
     */
    private void sendEvent(Map<String, Object> event) {
        if (emitter == null) {
            log.warn("[{}] Emitter is null, event will not be pushed to client", conversationId);
            return;
        }

        try {
            String message = "data: " + JSON.toJSONString(event) + "\n\n";
            emitter.send(message);
            log.trace("[{}] SSE event sent: type={}", conversationId, event.get("type"));
        } catch (IOException e) {
            log.warn("[{}] Client disconnected, failed to send SSE event: {}", conversationId, e.getMessage());
        } catch (Exception e) {
            log.error("[{}] Failed to send SSE event", conversationId, e);
        }
    }
}
