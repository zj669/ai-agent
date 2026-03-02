package com.zj.aiagent.application.swarm.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.application.swarm.prompt.SwarmPromptTemplate;
import com.zj.aiagent.domain.llm.entity.LlmProviderConfig;
import com.zj.aiagent.domain.swarm.entity.SwarmAgent;
import com.zj.aiagent.domain.swarm.entity.SwarmMessage;
import com.zj.aiagent.domain.swarm.repository.SwarmAgentRepository;
import com.zj.aiagent.domain.swarm.repository.SwarmGroupRepository;
import com.zj.aiagent.domain.swarm.repository.SwarmMessageRepository;
import com.zj.aiagent.domain.swarm.service.SwarmDomainService;
import com.zj.aiagent.domain.swarm.valobj.SwarmAgentStatus;
import com.zj.aiagent.infrastructure.swarm.llm.SwarmLlmCaller;
import com.zj.aiagent.infrastructure.swarm.llm.SwarmLlmResponse;
import com.zj.aiagent.infrastructure.swarm.sse.SwarmAgentEventBus;
import com.zj.aiagent.infrastructure.swarm.sse.SwarmUIEventBus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.support.ToolCallbacks;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    private final SwarmGroupRepository groupRepository;
    private final SwarmMessageRepository messageRepository;
    private final SwarmLlmCaller llmCaller;
    private final SwarmTools swarmTools;
    private final ToolCallback[] toolCallbacks;
    private final ObjectMapper objectMapper;
    private final SwarmAgentEventBus agentEventBus;
    private final SwarmUIEventBus uiEventBus;
    private final int maxRoundsPerTurn;
    private final Long humanAgentId;
    private final LlmProviderConfig llmConfig;

    private volatile boolean running = true;
    private final CompletableFuture<Void> wakeSignal = new CompletableFuture<>();
    private CompletableFuture<Void> currentWakeSignal;

    public SwarmAgentRunner(
            SwarmAgent agent,
            SwarmDomainService domainService,
            SwarmAgentRepository agentRepository,
            SwarmGroupRepository groupRepository,
            SwarmMessageRepository messageRepository,
            SwarmLlmCaller llmCaller,
            SwarmTools swarmTools,
            ObjectMapper objectMapper,
            SwarmAgentEventBus agentEventBus,
            SwarmUIEventBus uiEventBus,
            int maxRoundsPerTurn,
            Long humanAgentId,
            LlmProviderConfig llmConfig) {
        this.agent = agent;
        this.domainService = domainService;
        this.agentRepository = agentRepository;
        this.groupRepository = groupRepository;
        this.messageRepository = messageRepository;
        this.llmCaller = llmCaller;
        this.swarmTools = swarmTools;
        this.toolCallbacks = ToolCallbacks.from(swarmTools);
        this.objectMapper = objectMapper;
        this.agentEventBus = agentEventBus;
        this.uiEventBus = uiEventBus;
        this.maxRoundsPerTurn = maxRoundsPerTurn;
        this.humanAgentId = humanAgentId;
        this.llmConfig = llmConfig;
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

            // 分离人类消息和 agent 消息
            Map<Long, List<SwarmMessage>> humanMessages = new LinkedHashMap<>();
            Map<Long, List<SwarmMessage>> agentMessages = new LinkedHashMap<>();

            for (Map.Entry<Long, List<SwarmMessage>> entry : unreadByGroup.entrySet()) {
                boolean fromHuman = humanAgentId != null && entry.getValue().stream()
                        .anyMatch(m -> m.getSenderId().equals(humanAgentId));
                if (fromHuman) {
                    humanMessages.put(entry.getKey(), entry.getValue());
                } else {
                    agentMessages.put(entry.getKey(), entry.getValue());
                }
            }

            // 先处理人类消息（优先级高）
            if (!humanMessages.isEmpty()) {
                processHumanMessages(humanMessages);
            }

            // 再处理 agent 消息
            if (!agentMessages.isEmpty()) {
                processAgentMessages(agentMessages);
            }

            // 标记已读
            for (Map.Entry<Long, List<SwarmMessage>> entry : unreadByGroup.entrySet()) {
                List<SwarmMessage> msgs = entry.getValue();
                if (!msgs.isEmpty()) {
                    domainService.markRead(entry.getKey(), agent.getId(),
                            msgs.get(msgs.size() - 1).getId());
                }
            }

        } finally {
            agentRepository.updateStatus(agent.getId(), SwarmAgentStatus.IDLE.getCode());
            emitEvent("agent.done", null, "");
        }
    }

    /**
     * 处理来自人类的消息：流式输出 + 自动投递到 human-子agent 群
     */
    private void processHumanMessages(Map<Long, List<SwarmMessage>> humanMsgsByGroup) {
        // 独立构建 LLM 消息列表
        List<Message> messages = buildMessages(humanMsgsByGroup);

        Long primaryGroupId = humanMsgsByGroup.keySet().iterator().next();
        int round = 0;

        while (round < maxRoundsPerTurn) {
            round++;
            log.info("[Swarm] Agent {} human-round {}/{}", agent.getId(), round, maxRoundsPerTurn);

            // emit stream.start
            emitUIEvent("ui.agent.stream.start",
                    "{\"agentId\":" + agent.getId() + ",\"groupId\":" + primaryGroupId + "}");

            SwarmLlmResponse response = llmCaller.callStreamWithTools(
                    messages, toolCallbacks, chunk -> {
                        try {
                            String chunkData = objectMapper.writeValueAsString(Map.of(
                                    "agentId", agent.getId(),
                                    "groupId", primaryGroupId,
                                    "chunk", chunk));
                            emitUIEvent("ui.agent.stream.chunk", chunkData);
                        } catch (Exception e) {
                            log.warn("[Swarm] Failed to emit stream chunk", e);
                        }
                    }, llmConfig);

            // emit stream.done
            emitUIEvent("ui.agent.stream.done",
                    "{\"agentId\":" + agent.getId() + ",\"groupId\":" + primaryGroupId + "}");

            emitEvent("agent.stream", "content", response.getContent());

            // 把完整 content 存为消息到 human-agent P2P 群
            if (response.getContent() != null && !response.getContent().isEmpty()) {
                SwarmMessage replyMsg = SwarmMessage.builder()
                        .workspaceId(agent.getWorkspaceId())
                        .groupId(primaryGroupId)
                        .senderId(agent.getId())
                        .contentType("text")
                        .content(response.getContent())
                        .sendTime(LocalDateTime.now())
                        .build();
                messageRepository.save(replyMsg);

                emitUIEvent("ui.message.created",
                        "{\"groupId\":" + primaryGroupId + ",\"messageId\":" + replyMsg.getId() + ",\"senderId\":" + agent.getId() + "}");
            }

            if (response.hasToolCalls()) {
                for (AssistantMessage.ToolCall toolCall : response.getToolCalls()) {
                    String toolName = toolCall.name();
                    String toolArgs = toolCall.arguments();

                    log.info("[Swarm] Agent {} calling tool: {} args: {}", agent.getId(), toolName, toolArgs);
                    emitEvent("agent.stream", "tool_calls", toolName + ": " + toolArgs);

                    if ("send".equals(toolName)) {
                        try {
                            com.fasterxml.jackson.databind.JsonNode sendArgs = objectMapper.readTree(toolArgs);
                            long targetAgentId = sendArgs.has("agent_id") ? sendArgs.get("agent_id").asLong() : 0;
                            emitUIEvent("ui.agent.waiting",
                                    "{\"agentId\":" + agent.getId() + ",\"groupId\":" + primaryGroupId + ",\"targetAgentId\":" + targetAgentId + "}");
                        } catch (Exception e) {
                            log.warn("[Swarm] Failed to emit waiting event", e);
                        }
                    }

                    SwarmAgent freshAgent = agentRepository.findById(agent.getId()).orElse(agent);
                    String result = executeToolCall(toolName, toolArgs);

                    if ("send".equals(toolName)) {
                        emitUIEvent("ui.agent.waiting.done",
                                "{\"agentId\":" + agent.getId() + ",\"groupId\":" + primaryGroupId + "}");
                    }

                    saveToolCallMessage(primaryGroupId, toolName, toolArgs, result);

                    if ("send".equals(toolName)) {
                        // send 工具执行后 break，等子 agent 回复后再被唤醒继续
                        break;
                    }

                    messages.add(new AssistantMessage(response.getContent(),
                            Map.of(), List.of(toolCall)));
                    messages.add(new org.springframework.ai.chat.messages.ToolResponseMessage(
                            List.of(new org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse(
                                    toolCall.id(), toolCall.name(), result))));
                }
                // 如果最后一个 tool 是 send，跳出外层循环
                if (response.getToolCalls().stream().anyMatch(tc -> "send".equals(tc.name()))) {
                    break;
                }
                // 继续下一轮 LLM（非 send 工具如 create）
            } else {
                break;
            }
        }

        if (round >= maxRoundsPerTurn) {
            log.warn("[Swarm] Agent {} hit max rounds limit (human): {}", agent.getId(), maxRoundsPerTurn);
            emitEvent("agent.error", null, "Max rounds limit reached: " + maxRoundsPerTurn);
        }

        // 落库 llm_history（人类对话）
        saveLlmHistory(messages);
    }

    /**
     * 处理来自其他 agent 的消息：agent 间通信模式
     */
    private void processAgentMessages(Map<Long, List<SwarmMessage>> agentMsgsByGroup) {
        // 独立构建 LLM 消息列表
        List<Message> messages = buildMessages(agentMsgsByGroup);

        Long primaryGroupId = agentMsgsByGroup.keySet().iterator().next();

        // 判断当前 agent 是否是协调者（有 human P2P 群）
        boolean isCoordinator = (humanAgentId != null && findHumanGroupId() != null);

        log.info("[Swarm] Agent {} processing agent messages (single round, coordinator={})", agent.getId(), isCoordinator);

        SwarmLlmResponse response = llmCaller.callStreamWithTools(
                messages, toolCallbacks, null, llmConfig);

        emitEvent("agent.stream", "content", response.getContent());

        if (response.hasToolCalls()) {
            for (AssistantMessage.ToolCall toolCall : response.getToolCalls()) {
                String toolName = toolCall.name();
                String toolArgs = toolCall.arguments();

                if (isCoordinator && ("send".equals(toolName) || "sendGroupMessage".equals(toolName))) {
                    // 协调者收到子 agent 回复后，跳过 send 防止 ping-pong
                    // 把 send 的 message 内容提取出来作为投递给人类的文字
                    log.info("[Swarm] Agent {} (coordinator) skipping {} to prevent ping-pong", agent.getId(), toolName);
                    saveToolCallMessage(primaryGroupId, toolName, toolArgs, "skipped: auto-delivered to human instead");
                    try {
                        com.fasterxml.jackson.databind.JsonNode sendArgs = objectMapper.readTree(toolArgs);
                        String sendMessage = sendArgs.has("message") ? sendArgs.get("message").asText() : null;
                        if (sendMessage != null && !sendMessage.isBlank()) {
                            Long humanGroupId = findHumanGroupId();
                            if (humanGroupId != null) {
                                SwarmMessage msg = SwarmMessage.builder()
                                        .workspaceId(agent.getWorkspaceId())
                                        .groupId(humanGroupId)
                                        .senderId(agent.getId())
                                        .contentType("text")
                                        .content(sendMessage)
                                        .sendTime(LocalDateTime.now())
                                        .build();
                                messageRepository.save(msg);
                                emitUIEvent("ui.message.created",
                                        "{\"groupId\":" + humanGroupId + ",\"messageId\":" + msg.getId() + ",\"senderId\":" + agent.getId() + "}");
                                log.info("[Swarm] Agent {} redirected send message to human group {}", agent.getId(), humanGroupId);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("[Swarm] Failed to redirect send message to human", e);
                    }
                    continue;
                }

                log.info("[Swarm] Agent {} calling tool: {} args: {}", agent.getId(), toolName, toolArgs);
                emitEvent("agent.stream", "tool_calls", toolName + ": " + toolArgs);

                String result = executeToolCall(toolName, toolArgs);
                saveToolCallMessage(primaryGroupId, toolName, toolArgs, result);
            }
        }

        // 协调者：有文字内容就自动投递给人类
        if (isCoordinator) {
            String content = response.getContent();
            if (content != null && !content.isBlank()) {
                Long humanGroupId = findHumanGroupId();
                if (humanGroupId != null) {
                    SwarmMessage msg = SwarmMessage.builder()
                            .workspaceId(agent.getWorkspaceId())
                            .groupId(humanGroupId)
                            .senderId(agent.getId())
                            .contentType("text")
                            .content(content)
                            .sendTime(LocalDateTime.now())
                            .build();
                    messageRepository.save(msg);
                    emitUIEvent("ui.message.created",
                            "{\"groupId\":" + humanGroupId + ",\"messageId\":" + msg.getId() + ",\"senderId\":" + agent.getId() + "}");
                    log.info("[Swarm] Agent {} auto-delivered agent reply to human group {}", agent.getId(), humanGroupId);
                }
            }
        }

        // 落库 llm_history
        saveLlmHistory(messages);
    }

    /**
     * 落库 llm_history
     */
    private void saveLlmHistory(List<Message> messages) {
        try {
            String historyJson = objectMapper.writeValueAsString(messages.stream()
                    .map(m -> Map.of("role", m.getMessageType().getValue(), "content", m.getText() != null ? m.getText() : ""))
                    .toList());
            agentRepository.updateLlmHistory(agent.getId(), historyJson);
        } catch (Exception e) {
            log.error("[Swarm] Failed to save llm_history for agent {}", agent.getId(), e);
        }
    }

    /**
     * 查找当前 agent 和 human 的 P2P 群 ID
     */
    private Long findHumanGroupId() {
        if (humanAgentId == null) return null;
        try {
            var groups = groupRepository.findByWorkspaceId(agent.getWorkspaceId());
            for (var group : groups) {
                List<Long> memberIds = groupRepository.findMemberIds(group.getId());
                if (memberIds.size() == 2 && memberIds.contains(agent.getId()) && memberIds.contains(humanAgentId)) {
                    return group.getId();
                }
            }
        } catch (Exception e) {
            log.warn("[Swarm] Failed to find human group for agent {}", agent.getId(), e);
        }
        return null;
    }

    /**
     * 构建 LLM 消息列表
     */
    private List<Message> buildMessages(Map<Long, List<SwarmMessage>> unreadByGroup) {
        List<Message> messages = new ArrayList<>();

        // System prompt
        messages.add(SwarmLlmCaller.systemMessage(
                SwarmPromptTemplate.build(agent.getId(), agent.getWorkspaceId(), agent.getRole(), humanAgentId)));

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

    private void emitUIEvent(String type, String data) {
        uiEventBus.emit(agent.getWorkspaceId(), SwarmUIEventBus.UIEvent.builder()
                .type(type)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build());
    }

    /**
     * 通过 SwarmTools 执行工具调用
     */
    private String executeToolCall(String toolName, String toolArgs) {
        try {
            com.fasterxml.jackson.databind.JsonNode args = objectMapper.readTree(toolArgs != null ? toolArgs : "{}");
            return switch (toolName) {
                case "create" -> swarmTools.create(
                        args.has("role") ? args.get("role").asText() : "assistant",
                        args.has("description") ? args.get("description").asText() : "");
                case "send" -> swarmTools.send(
                        args.get("agentId").asLong(),
                        args.get("message").asText());
                case "self" -> swarmTools.self();
                case "listAgents" -> swarmTools.listAgents();
                case "sendGroupMessage" -> swarmTools.sendGroupMessage(
                        args.get("groupId").asLong(),
                        args.get("message").asText());
                case "listGroups" -> swarmTools.listGroups();
                default -> "{\"error\": \"Unknown tool: " + toolName + "\"}";
            };
        } catch (Exception e) {
            log.error("[Swarm] Tool execution failed: tool={}, agent={}", toolName, agent.getId(), e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 存 tool_call 消息到 group，并 emit UI 事件让前端实时显示
     */
    private void saveToolCallMessage(Long groupId, String toolName, String toolArgs, String toolResult) {
        try {
            String content = "{\"tool\":\"" + toolName + "\",\"args\":" + (toolArgs != null ? toolArgs : "{}") + ",\"result\":" + objectMapper.writeValueAsString(toolResult) + "}";
            SwarmMessage msg = SwarmMessage.builder()
                    .workspaceId(agent.getWorkspaceId())
                    .groupId(groupId)
                    .senderId(agent.getId())
                    .contentType("tool_call")
                    .content(content)
                    .sendTime(LocalDateTime.now())
                    .build();
            messageRepository.save(msg);

            // emit UI 事件
            uiEventBus.emit(agent.getWorkspaceId(), SwarmUIEventBus.UIEvent.builder()
                    .type("ui.message.created")
                    .data("{\"groupId\":" + groupId + ",\"messageId\":" + msg.getId() + ",\"senderId\":" + agent.getId() + "}")
                    .timestamp(System.currentTimeMillis())
                    .build());
        } catch (Exception e) {
            log.warn("[Swarm] Failed to save tool_call message: agent={}, tool={}", agent.getId(), toolName, e);
        }
    }
}
