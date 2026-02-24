package com.zj.aiagent.interfaces.swarm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.infrastructure.swarm.sse.SwarmAgentEventBus;
import com.zj.aiagent.infrastructure.swarm.sse.SwarmUIEventBus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.function.Consumer;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SwarmSseController {

    private final SwarmAgentEventBus agentEventBus;
    private final SwarmUIEventBus uiEventBus;
    private final ObjectMapper objectMapper;

    /**
     * Agent 级 SSE：reasoning/content/tool_calls/tool_result/done/error
     */
    @GetMapping("/api/swarm/agent/{agentId}/stream")
    public SseEmitter agentStream(@PathVariable Long agentId) {
        SseEmitter emitter = new SseEmitter(0L); // 无超时

        Consumer<SwarmAgentEventBus.AgentEvent> subscriber = event -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.getType())
                        .data(event.getData() != null ? event.getData() : ""));
            } catch (IOException e) {
                agentEventBus.unsubscribe(agentId, null); // 会在 completion 里清理
            }
        };

        agentEventBus.subscribe(agentId, subscriber);

        emitter.onCompletion(() -> agentEventBus.unsubscribe(agentId, subscriber));
        emitter.onTimeout(() -> agentEventBus.unsubscribe(agentId, subscriber));
        emitter.onError(e -> agentEventBus.unsubscribe(agentId, subscriber));

        return emitter;
    }

    /**
     * UI 级 SSE：agent.created/message.created/llm.start/llm.done
     */
    @GetMapping("/api/swarm/workspace/{workspaceId}/ui-stream")
    public SseEmitter uiStream(@PathVariable Long workspaceId) {
        SseEmitter emitter = new SseEmitter(0L);

        Consumer<SwarmUIEventBus.UIEvent> subscriber = event -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.getType())
                        .data(event.getData() != null ? event.getData() : ""));
            } catch (IOException e) {
                // 连接断开
            }
        };

        uiEventBus.subscribe(workspaceId, subscriber);

        emitter.onCompletion(() -> uiEventBus.unsubscribe(workspaceId, subscriber));
        emitter.onTimeout(() -> uiEventBus.unsubscribe(workspaceId, subscriber));
        emitter.onError(e -> uiEventBus.unsubscribe(workspaceId, subscriber));

        return emitter;
    }
}
