package com.zj.aiagent.infrastructure.swarm.sse;

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
        private String type; // agent.stream, agent.done, agent.error, agent.wakeup
        private String subType; // reasoning, content, tool_calls, tool_result
        private String data;
        private long timestamp;
    }
}
