package com.zj.aiagent.infrastructure.swarm.sse;

import com.zj.aiagent.domain.swarm.valobj.TaskNotificationEvent;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Agent 级事件总线：reasoning/content/tool_calls/tool_result/done/error/wakeup
 */
@Slf4j
@Component
public class SwarmAgentEventBus {

    /** agentId -> 订阅者列表 */
    private final Map<Long, List<Consumer<AgentEvent>>> subscribers = new ConcurrentHashMap<>();

    /** agentId -> 历史事件（用于 getSince） */
    private final Map<Long, List<AgentEvent>> history = new ConcurrentHashMap<>();

    public void emit(Long agentId, AgentEvent event) {
        history.computeIfAbsent(agentId, k -> new CopyOnWriteArrayList<>()).add(event);
        List<Consumer<AgentEvent>> subs = subscribers.get(agentId);
        if (subs != null) {
            for (Consumer<AgentEvent> sub : subs) {
                try {
                    sub.accept(event);
                } catch (Exception e) {
                    log.warn("[Swarm] AgentEventBus subscriber error: agent={}", agentId, e);
                }
            }
        }
    }

    public void subscribe(Long agentId, Consumer<AgentEvent> subscriber) {
        subscribers.computeIfAbsent(agentId, k -> new CopyOnWriteArrayList<>()).add(subscriber);
    }

    public void unsubscribe(Long agentId, Consumer<AgentEvent> subscriber) {
        List<Consumer<AgentEvent>> subs = subscribers.get(agentId);
        if (subs != null) subs.remove(subscriber);
    }

    public List<AgentEvent> getSince(Long agentId, int fromIndex) {
        List<AgentEvent> events = history.getOrDefault(agentId, List.of());
        if (fromIndex >= events.size()) return List.of();
        return events.subList(fromIndex, events.size());
    }

    @Data
    @Builder
    public static class AgentEvent {
        private String type; // agent.stream, agent.done, agent.error, agent.wakeup, agent.task-notification
        private String subType; // reasoning, content, tool_calls, tool_result
        private String data;
        private long timestamp;
    }

    /**
     * 发布 TaskNotification 事件到 EventBus
     * @param parentAgentId 订阅者（通常是 Coordinator）的 Agent ID
     * @param event 任务通知事件（来自 domain 层）
     */
    public void emitTaskNotification(Long parentAgentId, TaskNotificationEvent event) {
        event.setTimestamp(System.currentTimeMillis());
        // 将事件序列化后作为 AgentEvent.data 发送
        try {
            String eventData = String.format(
                "<task-notification>\n" +
                "  <task-id>%d</task-id>\n" +
                "  <status>%s</status>\n" +
                "  <summary>%s</summary>\n" +
                "  <result>%s</result>\n" +
                "  <phase>%s</phase>\n" +
                "  <taskUuid>%s</taskUuid>\n" +
                "  <usage>\n" +
                "    <total_tokens>%s</total_tokens>\n" +
                "    <tool_uses>%s</tool_uses>\n" +
                "    <duration_ms>%s</duration_ms>\n" +
                "  </usage>\n" +
                "</task-notification>",
                event.getAgentId(),
                event.getStatus(),
                escapeXml(event.getSummary()),
                escapeXml(event.getResult()),
                event.getPhase() != null ? event.getPhase() : "",
                event.getTaskUuid() != null ? event.getTaskUuid() : "",
                event.getUsage() != null && event.getUsage().getTotalTokens() != null
                    ? event.getUsage().getTotalTokens()
                    : "0",
                event.getUsage() != null && event.getUsage().getToolUses() != null
                    ? event.getUsage().getToolUses()
                    : "0",
                event.getUsage() != null && event.getUsage().getDurationMs() != null
                    ? event.getUsage().getDurationMs()
                    : "0"
            );
            emit(parentAgentId, AgentEvent.builder()
                .type("agent.task-notification")
                .subType("task-notification")
                .data(eventData)
                .timestamp(event.getTimestamp())
                .build());
        } catch (Exception e) {
            log.warn("[Swarm] Failed to emit task notification: agent={}", parentAgentId, e);
        }
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}
