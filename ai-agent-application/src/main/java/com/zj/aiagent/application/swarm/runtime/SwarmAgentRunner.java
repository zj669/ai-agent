package com.zj.aiagent.application.swarm.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.application.swarm.prompt.SwarmPromptTemplate;
import com.zj.aiagent.domain.swarm.entity.SwarmAgent;
import com.zj.aiagent.domain.swarm.entity.SwarmMessage;
import com.zj.aiagent.domain.swarm.repository.SwarmAgentRepository;
import com.zj.aiagent.domain.swarm.service.SwarmDomainService;
import com.zj.aiagent.domain.swarm.valobj.SwarmAgentStatus;
import com.zj.aiagent.infrastructure.swarm.llm.SwarmLlmCaller;
import com.zj.aiagent.infrastructure.swarm.llm.SwarmLlmResponse;
import com.zj.aiagent.infrastructure.swarm.sse.SwarmAgentEventBus;
import com.zj.aiagent.infrastructure.swarm.tool.SwarmToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 单个 Agent 的运行循环（虚拟线程）
 */
@Slf4j
public class SwarmAgentRunner implements Runnable {

    private final SwarmAgent agent;
    private final SwarmDomainService domainService;
    private final SwarmAgentRepository agentRepository;
    private final SwarmLlmCaller llmCaller;
    private final SwarmToolRegistry toolRegistry;
    private final SwarmToolExecutor toolExecutor;
    private final ObjectMapper objectMapper;
    private final SwarmAgentEventBus agentEventBus;
    private final int maxRoundsPerTurn;

    private volatile boolean running = true;
    private final CompletableFuture<Void> wakeSignal = new CompletableFuture<>();
    private CompletableFuture<Void> currentWakeSignal;

    public SwarmAgentRunner(
            SwarmAgent agent,
            SwarmDomainService domainService,
            SwarmAgentRepository agentRepository,
            SwarmLlmCaller llmCaller,
            SwarmToolRegistry toolRegistry,
            SwarmToolExecutor toolExecutor,
            ObjectMapper objectMapper,
            SwarmAgentEventBus agentEventBus,
            int maxRoundsPerTurn) {
        this.agent = agent;
        this.domainService = domainService;
        this.agentRepository = agentRepository;
        this.llmCaller = llmCaller;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.objectMapper = objectMapper;
        this.agentEventBus = agentEventBus;
        this.maxRoundsPerTurn = maxRoundsPerTurn;
        this.currentWakeSignal = new CompletableFuture<>();
    }

    @Override
    public void run() {
        log.info("[Swarm] AgentRunner started: agent={}, role={}", agent.getId(), agent.getRole());

        while (running) {
            try {
                // 阻塞等待唤醒信号
                currentWakeSignal.join();
                if (!running) break;

                // 重置唤醒信号
                currentWakeSignal = new CompletableFuture<>();

                // 执行一轮推理
                processTurn();

            } catch (Exception e) {
                if (!running) break;
                log.error("[Swarm] AgentRunner error: agent={}", agent.getId(), e);
                emitEvent("agent.error", null, e.getMessage());
            }
        }

        log.info("[Swarm] AgentRunner stopped: agent={}", agent.getId());
    }

    /**
     * 唤醒 Agent
     */
    public void wake() {
        currentWakeSignal.complete(null);
    }

    /**
     * 停止 Agent
     */
    public void stop() {
        running = false;
        currentWakeSignal.complete(null);
    }

    /**
     * 执行一轮推理
     */
    private void processTurn() {
        agentRepository.updateStatus(agent.getId(), SwarmAgentStatus.BUSY.getCode());

        try {
            // 拉取未读消息
            Map<Long, List<SwarmMessage>> unreadByGroup = domainService.getUnreadMessagesByAgent(agent.getId());
            if (unreadByGroup.isEmpty()) {
                agentRepository.updateStatus(agent.getId(), SwarmAgentStatus.IDLE.getCode());
                return;
            }

            // 构建 LLM 消息列表
            List<Message> messages = buildMessages(unreadByGroup);

            int round = 0;
            boolean calledSend = false;

            while (round < maxRoundsPerTurn) {
                round++;
                log.info("[Swarm] Agent {} round {}/{}", agent.getId(), round, maxRoundsPerTurn);

                // 调用 LLM
                SwarmLlmResponse response = llmCaller.callStream(
                        messages, toolRegistry.getAllTools(), null);

                // TODO P3: emit agent.stream 事件
                emitEvent("agent.stream", "content", response.getContent());

                if (response.hasToolCalls()) {
                    // 处理 tool_calls
                    for (AssistantMessage.ToolCall toolCall : response.getToolCalls()) {
                        String toolName = toolCall.name();
                        String toolArgs = toolCall.arguments();

                        log.info("[Swarm] Agent {} calling tool: {} args: {}", agent.getId(), toolName, toolArgs);
                        emitEvent("agent.stream", "tool_calls", toolName + ": " + toolArgs);

                        // 刷新 agent 状态（可能被 create 改了）
                        SwarmAgent freshAgent = agentRepository.findById(agent.getId()).orElse(agent);
                        String result = toolExecutor.execute(toolName, toolArgs, freshAgent);

                        // 检查是否调了 send 类工具
                        if ("send".equals(toolName) || "send_group_message".equals(toolName)) {
                            calledSend = true;
                        }

                        // 把 tool result 加入消息列表
                        messages.add(new AssistantMessage(response.getContent(),
                                Map.of(), List.of(toolCall)));
                        messages.add(new org.springframework.ai.chat.messages.ToolResponseMessage(
                                List.of(new org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse(
                                        toolCall.id(), toolCall.name(), result))));
                    }
                } else {
                    // 没有 tool_calls
                    if (!calledSend && round < maxRoundsPerTurn) {
                        // 追加提醒再跑一轮
                        messages.add(new AssistantMessage(response.getContent()));
                        messages.add(SwarmLlmCaller.userMessage(
                                "You haven't sent any message yet. Remember: your replies are NOT automatically delivered. " +
                                "Use `send` or `send_group_message` to communicate."));
                    } else {
                        break;
                    }
                }
            }

            if (round >= maxRoundsPerTurn) {
                log.warn("[Swarm] Agent {} hit max rounds limit: {}", agent.getId(), maxRoundsPerTurn);
                emitEvent("agent.error", null, "Max rounds limit reached: " + maxRoundsPerTurn);
            }

            // 标记已读
            for (Map.Entry<Long, List<SwarmMessage>> entry : unreadByGroup.entrySet()) {
                List<SwarmMessage> msgs = entry.getValue();
                if (!msgs.isEmpty()) {
                    domainService.markRead(entry.getKey(), agent.getId(),
                            msgs.get(msgs.size() - 1).getId());
                }
            }

            // 落库 llm_history
            try {
                String historyJson = objectMapper.writeValueAsString(messages.stream()
                        .map(m -> Map.of("role", m.getMessageType().getValue(), "content", m.getText() != null ? m.getText() : ""))
                        .toList());
                agentRepository.updateLlmHistory(agent.getId(), historyJson);
            } catch (Exception e) {
                log.error("[Swarm] Failed to save llm_history for agent {}", agent.getId(), e);
            }

        } finally {
            agentRepository.updateStatus(agent.getId(), SwarmAgentStatus.IDLE.getCode());
            emitEvent("agent.done", null, "");
        }
    }

    /**
     * 构建 LLM 消息列表
     */
    private List<Message> buildMessages(Map<Long, List<SwarmMessage>> unreadByGroup) {
        List<Message> messages = new ArrayList<>();

        // System prompt
        messages.add(SwarmLlmCaller.systemMessage(
                SwarmPromptTemplate.build(agent.getId(), agent.getWorkspaceId(), agent.getRole())));

        // 加载已有 llm_history
        // TODO: 从 agent.getLlmHistory() 解析并加入

        // 未读消息作为 user messages
        for (Map.Entry<Long, List<SwarmMessage>> entry : unreadByGroup.entrySet()) {
            Long groupId = entry.getKey();
            for (SwarmMessage msg : entry.getValue()) {
                String prefix = "[group:" + groupId + " from:agent_" + msg.getSenderId() + "] ";
                messages.add(SwarmLlmCaller.userMessage(prefix + msg.getContent()));
            }
        }

        return messages;
    }

    private void emitEvent(String type, String subType, String data) {
        agentEventBus.emit(agent.getId(), SwarmAgentEventBus.AgentEvent.builder()
                .type(type)
                .subType(subType)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build());
    }
}
