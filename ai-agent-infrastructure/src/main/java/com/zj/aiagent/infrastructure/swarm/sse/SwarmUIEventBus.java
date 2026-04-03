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
 * UI 级事件总线：agent.created/message.created/llm.start/llm.done/tool_call.start/tool_call.done
 */
@Slf4j
@Component
public class SwarmUIEventBus {

    /** workspaceId -> 订阅者列表 */
    private final Map<Long, List<Consumer<UIEvent>>> subscribers = new ConcurrentHashMap<>();

    public void emit(Long workspaceId, UIEvent event) {
        List<Consumer<UIEvent>> subs = subscribers.get(workspaceId);
        if (subs != null) {
            for (Consumer<UIEvent> sub : subs) {
                try {
                    sub.accept(event);
                } catch (IllegalStateException e) {
                    // emitter 已被标记为 complete（通常因为前一次发送时客户端已断开），
                    // 属于正常生命周期，忽略即可
                    log.debug("[Swarm] UIEventBus: emitter already completed, skipping: workspace={}", workspaceId);
                } catch (Exception e) {
                    // 客户端断开（SSE flush 失败）或订阅者内部异常，均不影响主流程
                    log.warn("[Swarm] UIEventBus subscriber error: workspace={}, type={}, error={}",
                        workspaceId, e.getClass().getSimpleName(), e.getMessage());
                }
            }
        }
    }

    public void subscribe(Long workspaceId, Consumer<UIEvent> subscriber) {
        subscribers.computeIfAbsent(workspaceId, k -> new CopyOnWriteArrayList<>()).add(subscriber);
    }

    public void unsubscribe(Long workspaceId, Consumer<UIEvent> subscriber) {
        List<Consumer<UIEvent>> subs = subscribers.get(workspaceId);
        if (subs != null) subs.remove(subscriber);
    }

    @Data
    @Builder
    public static class UIEvent {
        private String type; // ui.agent.created, ui.message.created, ui.agent.llm.start, ui.agent.llm.done, ui.agent.tool_call.start, ui.agent.tool_call.done, ui.agent.stream.start, ui.agent.stream.chunk, ui.agent.stream.done
        private String data; // JSON payload
        private long timestamp;
    }
}
