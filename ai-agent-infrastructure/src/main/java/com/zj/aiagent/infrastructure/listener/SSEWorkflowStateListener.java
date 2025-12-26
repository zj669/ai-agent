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

/**
 * 基于SSE的工作流状态监听器实现
 * 将工作流执行状态通过Server-Sent Events推送给客户端
 */
@Slf4j
@AllArgsConstructor
public class SSEWorkflowStateListener implements WorkflowStateListener {

    private final ResponseBodyEmitter emitter;
    private final String conversationId;

    @Override
    public void onNodeStarted(String nodeId, String nodeName) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "node_execute");
        event.put("status", "starting");
        event.put("nodeId", nodeId);
        event.put("nodeName", nodeName);
        event.put("content", "");
        event.put("completed", false);
        event.put("timestamp", System.currentTimeMillis());
        event.put("conversationId", conversationId);

        sendEvent(event);
    }

    @Override
    public void onNodeStreaming(String nodeId, String contentChunk) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "node_execute");
        event.put("status", "streaming");
        event.put("nodeId", nodeId);
        event.put("content", contentChunk);
        event.put("completed", false);
        event.put("timestamp", System.currentTimeMillis());
        event.put("conversationId", conversationId);

        sendEvent(event);
    }

    @Override
    public void onNodeCompleted(String nodeId, WorkflowState result) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "node_execute");
        event.put("status", "completed");
        event.put("nodeId", nodeId);
        event.put("content", result != null ? JSON.toJSONString(result.getAll()) : "");
        event.put("completed", true);
        event.put("timestamp", System.currentTimeMillis());
        event.put("conversationId", conversationId);

        sendEvent(event);
    }

    @Override
    public void onWorkflowFailed(Throwable t) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "error");
        event.put("status", "failed");
        event.put("content", t != null ? t.getMessage() : "Unknown error");
        event.put("completed", true);
        event.put("timestamp", System.currentTimeMillis());
        event.put("conversationId", conversationId);

        sendEvent(event);
    }

    /**
     * 发送SSE事件
     * 
     * @param event 事件数据
     */
    private void sendEvent(Map<String, Object> event) {
        if (emitter == null) {
            log.warn("Emitter is null, event will not be pushed to client");
            return;
        }

        try {
            String message = "data: " + JSON.toJSONString(event) + "\n\n";
            emitter.send(message);
            log.debug("SSE event sent: type={}, nodeId={}",
                    event.get("type"), event.get("nodeId"));
        } catch (IOException e) {
            log.warn("Client disconnected, failed to send SSE event: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to send SSE event", e);
        }
    }
}
