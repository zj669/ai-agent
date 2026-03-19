package com.zj.aiagent.application.swarm.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.application.swarm.SwarmMessageService;
import com.zj.aiagent.application.swarm.dto.SendMessageRequest;
import com.zj.aiagent.application.swarm.prompt.SwarmPromptTemplate;
import com.zj.aiagent.application.writing.WritingAgentCoordinatorService;
import com.zj.aiagent.application.writing.WritingResultService;
import com.zj.aiagent.application.writing.WritingSessionService;
import com.zj.aiagent.application.writing.WritingTaskService;
import com.zj.aiagent.domain.llm.entity.LlmProviderConfig;
import com.zj.aiagent.domain.swarm.entity.SwarmAgent;
import com.zj.aiagent.domain.swarm.entity.SwarmMessage;
import com.zj.aiagent.domain.swarm.repository.SwarmAgentRepository;
import com.zj.aiagent.domain.swarm.repository.SwarmGroupRepository;
import com.zj.aiagent.domain.swarm.repository.SwarmMessageRepository;
import com.zj.aiagent.domain.swarm.service.SwarmDomainService;
import com.zj.aiagent.domain.swarm.valobj.SwarmAgentStatus;
import com.zj.aiagent.domain.writing.entity.WritingAgent;
import com.zj.aiagent.domain.writing.entity.WritingResult;
import com.zj.aiagent.domain.writing.entity.WritingSession;
import com.zj.aiagent.domain.writing.entity.WritingTask;
import com.zj.aiagent.infrastructure.swarm.llm.SwarmLlmCaller;
import com.zj.aiagent.infrastructure.swarm.llm.SwarmLlmResponse;
import com.zj.aiagent.infrastructure.swarm.sse.SwarmAgentEventBus;
import com.zj.aiagent.infrastructure.swarm.sse.SwarmUIEventBus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

/**
 * 单个 Agent 的运行循环（虚拟线程）
 */
@Slf4j
public class SwarmAgentRunner implements Runnable {

    private static final java.util.Set<String> SUB_AGENT_FORBIDDEN_TOOLS =
        java.util.Set.of(
            "createAgent",
            "executeWorkflow",
            "listAgents",
            "writing_session",
            "writing_agent",
            "writing_task",
            "writing_draft"
        );

    private final SwarmAgent agent;
    private final SwarmDomainService domainService;
    private final SwarmAgentRepository agentRepository;
    private final SwarmGroupRepository groupRepository;
    private final SwarmMessageRepository messageRepository;
    private final SwarmMessageService messageService;
    private final SwarmLlmCaller llmCaller;
    private final SwarmTools swarmTools;
    private final ToolCallback[] toolCallbacks;
    private final ObjectMapper objectMapper;
    private final SwarmAgentEventBus agentEventBus;
    private final SwarmUIEventBus uiEventBus;
    private final WritingSessionService writingSessionService;
    private final WritingAgentCoordinatorService writingAgentCoordinatorService;
    private final WritingTaskService writingTaskService;
    private final WritingResultService writingResultService;
    private final int maxRoundsPerTurn;
    private final Long humanAgentId;
    private final LlmProviderConfig llmConfig;
    private final boolean isRoot;

    private volatile boolean running = true;
    private volatile Thread runThread;
    private final CompletableFuture<Void> wakeSignal =
        new CompletableFuture<>();
    private CompletableFuture<Void> currentWakeSignal;

    public SwarmAgentRunner(
        SwarmAgent agent,
        SwarmDomainService domainService,
        SwarmAgentRepository agentRepository,
        SwarmGroupRepository groupRepository,
        SwarmMessageRepository messageRepository,
        SwarmMessageService messageService,
        SwarmLlmCaller llmCaller,
        SwarmTools swarmTools,
        ObjectMapper objectMapper,
        SwarmAgentEventBus agentEventBus,
        SwarmUIEventBus uiEventBus,
        WritingSessionService writingSessionService,
        WritingAgentCoordinatorService writingAgentCoordinatorService,
        WritingTaskService writingTaskService,
        WritingResultService writingResultService,
        int maxRoundsPerTurn,
        Long humanAgentId,
        LlmProviderConfig llmConfig,
        boolean isRoot
    ) {
        this.agent = agent;
        this.domainService = domainService;
        this.agentRepository = agentRepository;
        this.groupRepository = groupRepository;
        this.messageRepository = messageRepository;
        this.messageService = messageService;
        this.llmCaller = llmCaller;
        this.swarmTools = swarmTools;
        this.toolCallbacks = ToolCallbacks.from(swarmTools);
        this.objectMapper = objectMapper;
        this.agentEventBus = agentEventBus;
        this.uiEventBus = uiEventBus;
        this.writingSessionService = writingSessionService;
        this.writingAgentCoordinatorService = writingAgentCoordinatorService;
        this.writingTaskService = writingTaskService;
        this.writingResultService = writingResultService;
        this.maxRoundsPerTurn = maxRoundsPerTurn;
        this.humanAgentId = humanAgentId;
        this.llmConfig = llmConfig;
        this.isRoot = isRoot;
        this.currentWakeSignal = new CompletableFuture<>();
    }

    @Override
    public void run() {
        runThread = Thread.currentThread();
        log.info(
            "[Swarm] AgentRunner started: agent={}, role={}",
            agent.getId(),
            agent.getRole()
        );

        while (running) {
            try {
                currentWakeSignal.join();
                if (!running) break;

                currentWakeSignal = new CompletableFuture<>();

                processTurn();
            } catch (Exception e) {
                if (!running) break;
                if (Thread.currentThread().isInterrupted()) {
                    log.info(
                        "[Swarm] AgentRunner interrupted: agent={}",
                        agent.getId()
                    );
                    break;
                }
                log.error(
                    "[Swarm] AgentRunner error: agent={}",
                    agent.getId(),
                    e
                );
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
     * 停止 Agent — 设置标志 + 中断线程以打断 blockLast 等阻塞调用
     */
    public void stop() {
        running = false;
        currentWakeSignal.complete(null);
        Thread t = runThread;
        if (t != null) {
            t.interrupt();
        }
    }

    /**
     * 执行一轮推理
     */
    private void processTurn() {
        SwarmAgentStatus finalStatus = SwarmAgentStatus.IDLE;
        agentRepository.updateStatus(
            agent.getId(),
            SwarmAgentStatus.BUSY.getCode()
        );

        try {
            // 拉取未读消息
            Map<Long, List<SwarmMessage>> unreadByGroup =
                domainService.getUnreadMessagesByAgent(agent.getId());
            if (unreadByGroup.isEmpty()) {
                log.info(
                    "[Swarm] No unread messages, agent returns idle: agent={}, workspace={}",
                    agent.getId(),
                    agent.getWorkspaceId()
                );
                agentRepository.updateStatus(
                    agent.getId(),
                    SwarmAgentStatus.IDLE.getCode()
                );
                return;
            }

            // 分离人类消息和 agent 消息
            Map<Long, List<SwarmMessage>> humanMessages = new LinkedHashMap<>();
            Map<Long, List<SwarmMessage>> agentMessages = new LinkedHashMap<>();

            for (Map.Entry<
                Long,
                List<SwarmMessage>
            > entry : unreadByGroup.entrySet()) {
                boolean fromHuman =
                    humanAgentId != null &&
                    entry
                        .getValue()
                        .stream()
                        .anyMatch(m -> m.getSenderId().equals(humanAgentId));
                if (fromHuman) {
                    humanMessages.put(entry.getKey(), entry.getValue());
                } else {
                    agentMessages.put(entry.getKey(), entry.getValue());
                }
            }

            log.info(
                "[Swarm] Turn started: agent={}, workspace={}, role={}, unreadGroups={}, unreadMessages={}, humanGroups={}, agentGroups={}",
                agent.getId(),
                agent.getWorkspaceId(),
                agent.getRole(),
                unreadByGroup.size(),
                countMessages(unreadByGroup),
                humanMessages.size(),
                agentMessages.size()
            );

            // 先处理人类消息（优先级高）
            if (!humanMessages.isEmpty()) {
                if (processHumanMessages(humanMessages)) {
                    finalStatus = SwarmAgentStatus.WAITING;
                }
            }

            // 再处理 agent 消息
            if (!agentMessages.isEmpty()) {
                if (processAgentMessages(agentMessages)) {
                    finalStatus = SwarmAgentStatus.WAITING;
                }
            }

            if (
                finalStatus != SwarmAgentStatus.WAITING &&
                shouldRemainWaitingAfterTurn()
            ) {
                finalStatus = SwarmAgentStatus.WAITING;
            }

            // 标记已读
            for (Map.Entry<
                Long,
                List<SwarmMessage>
            > entry : unreadByGroup.entrySet()) {
                List<SwarmMessage> msgs = entry.getValue();
                if (!msgs.isEmpty()) {
                    domainService.markRead(
                        entry.getKey(),
                        agent.getId(),
                        msgs.get(msgs.size() - 1).getId()
                    );
                }
            }
        } finally {
            if (isRoot && finalStatus != SwarmAgentStatus.WAITING) {
                emitWaitingDone(null);
            }
            agentRepository.updateStatus(agent.getId(), finalStatus.getCode());
            emitEvent("agent.done", null, "");
        }
    }

    /**
     * 处理来自人类的消息：流式输出 + 自动投递到 human-子agent 群
     */
    private boolean processHumanMessages(
        Map<Long, List<SwarmMessage>> humanMsgsByGroup
    ) {
        // 独立构建 LLM 消息列表
        List<Message> messages = buildMessages(humanMsgsByGroup);

        Long primaryGroupId = humanMsgsByGroup.keySet().iterator().next();
        log.info(
            "[Swarm] Processing human messages: agent={}, workspace={}, groups={}, totalMessages={}, primaryGroup={}",
            agent.getId(),
            agent.getWorkspaceId(),
            humanMsgsByGroup.keySet(),
            countMessages(humanMsgsByGroup),
            primaryGroupId
        );
        int round = 0;
        boolean waitingForChildAgents = false;

        while (round < maxRoundsPerTurn && running) {
            round++;
            log.info(
                "[Swarm] Agent {} human-round {}/{}",
                agent.getId(),
                round,
                maxRoundsPerTurn
            );

            // emit stream.start
            emitUIEvent(
                "ui.agent.stream.start",
                "{\"agentId\":" +
                    agent.getId() +
                    ",\"groupId\":" +
                    primaryGroupId +
                    "}"
            );

            SwarmLlmResponse response = llmCaller.callStreamWithTools(
                messages,
                toolCallbacks,
                chunk -> {
                    emitContentChunk(primaryGroupId, chunk, true);
                },
                llmConfig
            );

            log.info(
                "[Swarm] LLM response ready (human): agent={}, round={}, contentPreview={}, toolCallCount={}",
                agent.getId(),
                round,
                preview(response.getContent()),
                response.hasToolCalls() ? response.getToolCalls().size() : 0
            );

            List<AssistantMessage.ToolCall> executableToolCalls =
                filterExecutableToolCalls(response.getToolCalls());
            if (
                response.hasToolCalls() &&
                executableToolCalls.size() != response.getToolCalls().size()
            ) {
                log.warn(
                    "[Swarm] Dropped invalid streamed tool calls before execution (human): agent={}, rawCount={}, executableCount={}",
                    agent.getId(),
                    response.getToolCalls().size(),
                    executableToolCalls.size()
                );
            }

            // emit stream.done
            emitUIEvent(
                "ui.agent.stream.done",
                "{\"agentId\":" +
                    agent.getId() +
                    ",\"groupId\":" +
                    primaryGroupId +
                    "}"
            );

            // 把完整 content 存为消息到 human-agent P2P 群
            if (
                response.getContent() != null &&
                !response.getContent().isEmpty()
            ) {
                SwarmMessage replyMsg = SwarmMessage.builder()
                    .workspaceId(agent.getWorkspaceId())
                    .groupId(primaryGroupId)
                    .senderId(agent.getId())
                    .contentType("text")
                    .content(response.getContent())
                    .sendTime(LocalDateTime.now())
                    .build();
                messageRepository.save(replyMsg);
                log.info(
                    "[Swarm] Agent reply persisted to human group: agent={}, groupId={}, messageId={}, preview={}",
                    agent.getId(),
                    primaryGroupId,
                    replyMsg.getId(),
                    preview(replyMsg.getContent())
                );

                emitUIEvent(
                    "ui.message.created",
                    "{\"groupId\":" +
                        primaryGroupId +
                        ",\"messageId\":" +
                        replyMsg.getId() +
                        ",\"senderId\":" +
                        agent.getId() +
                        "}"
                );
            }

            if (!executableToolCalls.isEmpty()) {
                for (AssistantMessage.ToolCall toolCall : executableToolCalls) {
                    if (!running) break;
                    String toolName = toolCall.name();
                    String toolArgs = toolCall.arguments();
                    String toolCallEventId =
                        agent.getId() +
                        "-" +
                        System.nanoTime() +
                        "-" +
                        toolName;

                    log.info(
                        "[Swarm] Agent {} calling tool: {} args: {}",
                        agent.getId(),
                        toolName,
                        toolArgs
                    );
                    emitEvent(
                        "agent.stream",
                        "tool_calls",
                        toolName + ": " + toolArgs
                    );
                    emitToolCallUIEvent(
                        "ui.agent.tool_call.start",
                        primaryGroupId,
                        toolCallEventId,
                        toolName,
                        toolArgs,
                        null
                    );

                    if ("send".equals(toolName)) {
                        try {
                            Long targetAgentId = extractSendTargetAgentId(
                                toolArgs
                            );
                            if (shouldEnterWaitingAfterSend(targetAgentId)) {
                                waitingForChildAgents = true;
                                emitUIEvent(
                                    "ui.agent.waiting",
                                    "{\"agentId\":" +
                                        agent.getId() +
                                        ",\"groupId\":" +
                                        primaryGroupId +
                                        ",\"targetAgentId\":" +
                                        targetAgentId +
                                        "}"
                                );
                            }
                        } catch (Exception e) {
                            log.warn(
                                "[Swarm] Failed to emit waiting event: agent={}, toolArgs={}",
                                agent.getId(),
                                toolArgs
                            );
                        }
                    }

                    SwarmAgent freshAgent = agentRepository
                        .findById(agent.getId())
                        .orElse(agent);
                    String result = executeToolCall(toolName, toolArgs);
                    log.info(
                        "[Swarm] Tool call completed (human): agent={}, tool={}, resultPreview={}",
                        agent.getId(),
                        toolName,
                        preview(result)
                    );
                    emitToolCallUIEvent(
                        "ui.agent.tool_call.done",
                        primaryGroupId,
                        toolCallEventId,
                        toolName,
                        toolArgs,
                        result
                    );

                    saveToolCallMessage(
                        primaryGroupId,
                        toolName,
                        toolArgs,
                        result
                    );

                    messages.add(
                        new AssistantMessage(
                            response.getContent(),
                            Map.of(),
                            List.of(toolCall)
                        )
                    );
                    messages.add(
                        new org.springframework.ai.chat.messages.ToolResponseMessage(
                            List.of(
                                new org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse(
                                    toolCall.id(),
                                    toolCall.name(),
                                    result
                                )
                            )
                        )
                    );
                }
            } else {
                break;
            }
        }

        if (round >= maxRoundsPerTurn) {
            log.warn(
                "[Swarm] Agent {} hit max rounds limit (human): {}",
                agent.getId(),
                maxRoundsPerTurn
            );
            emitEvent(
                "agent.error",
                null,
                "Max rounds limit reached: " + maxRoundsPerTurn
            );
        }

        // 落库 llm_history（人类对话）
        saveLlmHistory(messages);
        return waitingForChildAgents;
    }

    /**
     * 处理来自其他 agent 的消息：agent 间通信模式
     */
    private boolean processAgentMessages(
        Map<Long, List<SwarmMessage>> agentMsgsByGroup
    ) {
        // 独立构建 LLM 消息列表
        List<Message> messages = buildMessages(agentMsgsByGroup);

        Long primaryGroupId = agentMsgsByGroup.keySet().iterator().next();

        // 判断当前 agent 是否是协调者（有 human P2P 群）
        boolean isCoordinator = (humanAgentId != null &&
            findHumanGroupId() != null);

        if (!isCoordinator) {
            markWritingAssignmentRunningIfNeeded();
        }
        boolean waitingForChildAgents = false;
        Long streamGroupId = isCoordinator
            ? findHumanGroupId()
            : primaryGroupId;

        log.info(
            "[Swarm] Processing agent messages: agent={}, workspace={}, coordinator={}, groups={}, totalMessages={}, primaryGroup={}",
            agent.getId(),
            agent.getWorkspaceId(),
            isCoordinator,
            agentMsgsByGroup.keySet(),
            countMessages(agentMsgsByGroup),
            primaryGroupId
        );

        emitStreamStart(streamGroupId);
        SwarmLlmResponse response = llmCaller.callStreamWithTools(
            messages,
            toolCallbacks,
            chunk -> emitContentChunk(streamGroupId, chunk, true),
            llmConfig
        );
        emitStreamDone(streamGroupId);

        log.info(
            "[Swarm] LLM response ready (agent): agent={}, coordinator={}, contentPreview={}, toolCallCount={}",
            agent.getId(),
            isCoordinator,
            preview(response.getContent()),
            response.hasToolCalls() ? response.getToolCalls().size() : 0
        );

        List<AssistantMessage.ToolCall> executableToolCalls =
            filterExecutableToolCalls(response.getToolCalls());
        if (
            response.hasToolCalls() &&
            executableToolCalls.size() != response.getToolCalls().size()
        ) {
            log.warn(
                "[Swarm] Dropped invalid streamed tool calls before execution (agent): agent={}, rawCount={}, executableCount={}",
                agent.getId(),
                response.getToolCalls().size(),
                executableToolCalls.size()
            );
        }

        if (!executableToolCalls.isEmpty()) {
            for (AssistantMessage.ToolCall toolCall : executableToolCalls) {
                String toolName = toolCall.name();
                String toolArgs = toolCall.arguments();
                String toolCallEventId =
                    agent.getId() + "-" + System.nanoTime() + "-" + toolName;

                Long sendTargetAgentId = "send".equals(toolName)
                    ? extractSendTargetAgentId(toolArgs)
                    : null;

                if (
                    isCoordinator &&
                    "send".equals(toolName) &&
                    sendTargetAgentId != null &&
                    sendTargetAgentId.equals(humanAgentId)
                ) {
                    // 协调者只拦截显式发给人类的 send，发给子 Agent 的 send 仍应正常执行
                    log.info(
                        "[Swarm] Agent {} (coordinator) redirecting send-to-human",
                        agent.getId(),
                        toolName
                    );
                    saveToolCallMessage(
                        primaryGroupId,
                        toolName,
                        toolArgs,
                        "skipped: auto-delivered to human instead"
                    );
                    emitToolCallUIEvent(
                        "ui.agent.tool_call.done",
                        primaryGroupId,
                        toolCallEventId,
                        toolName,
                        toolArgs,
                        "skipped: auto-delivered to human instead"
                    );
                    try {
                        JsonNode sendArgs =
                            SwarmToolArgumentUtils.parseArgumentsObject(
                                "send",
                                toolArgs,
                                objectMapper
                            );
                        String sendMessage =
                            SwarmToolArgumentUtils.getOptionalText(
                                sendArgs,
                                "message"
                            );
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
                                log.info(
                                    "[Swarm] Coordinator redirected send to human: agent={}, humanGroupId={}, messageId={}, preview={}",
                                    agent.getId(),
                                    humanGroupId,
                                    msg.getId(),
                                    preview(sendMessage)
                                );
                                emitUIEvent(
                                    "ui.message.created",
                                    "{\"groupId\":" +
                                        humanGroupId +
                                        ",\"messageId\":" +
                                        msg.getId() +
                                        ",\"senderId\":" +
                                        agent.getId() +
                                        "}"
                                );
                                log.info(
                                    "[Swarm] Agent {} redirected send message to human group {}",
                                    agent.getId(),
                                    humanGroupId
                                );
                            }
                        }
                    } catch (Exception e) {
                        log.warn(
                            "[Swarm] Failed to redirect send message to human: agent={}, toolArgs={}",
                            agent.getId(),
                            toolArgs
                        );
                    }
                    continue;
                }

                log.info(
                    "[Swarm] Agent {} calling tool: {} args: {}",
                    agent.getId(),
                    toolName,
                    toolArgs
                );
                emitEvent(
                    "agent.stream",
                    "tool_calls",
                    toolName + ": " + toolArgs
                );
                emitToolCallUIEvent(
                    "ui.agent.tool_call.start",
                    primaryGroupId,
                    toolCallEventId,
                    toolName,
                    toolArgs,
                    null
                );

                if (shouldEnterWaitingAfterSend(sendTargetAgentId)) {
                    waitingForChildAgents = true;
                    emitUIEvent(
                        "ui.agent.waiting",
                        "{\"agentId\":" +
                            agent.getId() +
                            ",\"groupId\":" +
                            primaryGroupId +
                            ",\"targetAgentId\":" +
                            sendTargetAgentId +
                            "}"
                    );
                }

                String result = executeToolCall(toolName, toolArgs);
                log.info(
                    "[Swarm] Tool call completed (agent): agent={}, tool={}, resultPreview={}",
                    agent.getId(),
                    toolName,
                    preview(result)
                );
                emitToolCallUIEvent(
                    "ui.agent.tool_call.done",
                    primaryGroupId,
                    toolCallEventId,
                    toolName,
                    toolArgs,
                    result
                );
                saveToolCallMessage(primaryGroupId, toolName, toolArgs, result);
                if (!isCoordinator && isWritingResultTool(toolName)) {
                    notifyParentAfterWritingResult(primaryGroupId, toolArgs);
                }
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
                    log.info(
                        "[Swarm] Coordinator auto-delivered reply to human: agent={}, humanGroupId={}, messageId={}, preview={}",
                        agent.getId(),
                        humanGroupId,
                        msg.getId(),
                        preview(content)
                    );
                    emitUIEvent(
                        "ui.message.created",
                        "{\"groupId\":" +
                            humanGroupId +
                            ",\"messageId\":" +
                            msg.getId() +
                            ",\"senderId\":" +
                            agent.getId() +
                            "}"
                    );
                    log.info(
                        "[Swarm] Agent {} auto-delivered agent reply to human group {}",
                        agent.getId(),
                        humanGroupId
                    );
                }
            }
        } else if (
            response.getContent() != null &&
            !response.getContent().isBlank() &&
            !response.hasToolCalls()
        ) {
            autoPersistWritingResultAndReply(
                primaryGroupId,
                response.getContent()
            );
        }

        // 落库 llm_history
        saveLlmHistory(messages);
        return waitingForChildAgents;
    }

    private Long extractSendTargetAgentId(String toolArgs) {
        try {
            JsonNode sendArgs = SwarmToolArgumentUtils.tryParseArgumentsObject(
                toolArgs,
                objectMapper
            );
            return sendArgs == null
                ? null
                : SwarmToolArgumentUtils.getOptionalLong(
                      sendArgs,
                      "agentId",
                      "agent_id"
                  );
        } catch (Exception e) {
            log.warn(
                "[Swarm] Failed to parse send target: agent={}, toolArgs={}",
                agent.getId(),
                toolArgs
            );
            return null;
        }
    }

    private boolean shouldEnterWaitingAfterSend(Long targetAgentId) {
        return (
            isRoot &&
            targetAgentId != null &&
            !targetAgentId.equals(humanAgentId)
        );
    }

    private void emitWaitingDone(Long targetAgentId) {
        Long humanGroupId = findHumanGroupId();
        if (humanGroupId == null) {
            return;
        }
        String targetAgentJson =
            targetAgentId == null ? "null" : String.valueOf(targetAgentId);
        emitUIEvent(
            "ui.agent.waiting.done",
            "{\"agentId\":" +
                agent.getId() +
                ",\"groupId\":" +
                humanGroupId +
                ",\"targetAgentId\":" +
                targetAgentJson +
                "}"
        );
    }

    private boolean isWritingResultTool(String toolName) {
        return (
            "writing_result".equals(toolName) ||
            "writing_result_by_task".equals(toolName) ||
            "writing_result_by_task_uuid".equals(toolName)
        );
    }

    private void notifyParentAfterWritingResult(
        Long primaryGroupId,
        String toolArgs
    ) {
        try {
            JsonNode args = SwarmToolArgumentUtils.tryParseArgumentsObject(
                toolArgs,
                objectMapper
            );
            if (args == null) {
                return;
            }

            String taskUuid = SwarmToolArgumentUtils.getOptionalText(
                args,
                "taskUuid",
                "task_uuid"
            );
            Long taskId = SwarmToolArgumentUtils.getOptionalLong(
                args,
                "taskId",
                "task_id"
            );
            String resultType = SwarmToolArgumentUtils.getOptionalText(
                args,
                "resultType",
                "result_type"
            );
            String summary = SwarmToolArgumentUtils.getOptionalText(
                args,
                "summary"
            );

            StringBuilder content = new StringBuilder("子任务已完成并回写结果");
            if (taskUuid != null && !taskUuid.isBlank()) {
                content.append(" taskUuid=").append(taskUuid);
            } else if (taskId != null) {
                content.append(" taskId=").append(taskId);
            }
            if (resultType != null && !resultType.isBlank()) {
                content.append("，类型=").append(resultType);
            }
            if (summary != null && !summary.isBlank()) {
                content.append("。\n摘要：").append(summary);
            }
            content.append("\n请继续整合已有结果并推进最终总结/成稿。");

            publishTextMessage(primaryGroupId, content.toString());
        } catch (Exception e) {
            log.warn(
                "[Swarm] Failed to notify parent after writing result: agent={}, groupId={}",
                agent.getId(),
                primaryGroupId,
                e
            );
        }
    }

    private boolean shouldRemainWaitingAfterTurn() {
        if (!isRoot) {
            return false;
        }
        try {
            return writingSessionService
                .listSessions(agent.getWorkspaceId())
                .stream()
                .filter(session ->
                    agent.getId().equals(session.getRootAgentId())
                )
                .filter(
                    session ->
                        !"COMPLETED".equals(session.getStatus()) &&
                        !"FAILED".equals(session.getStatus())
                )
                .anyMatch(session ->
                    writingTaskService
                        .listBySession(session.getId())
                        .stream()
                        .anyMatch(
                            task ->
                                "PENDING".equals(task.getStatus()) ||
                                "RUNNING".equals(task.getStatus())
                        )
                );
        } catch (Exception e) {
            log.warn(
                "[Swarm] Failed to determine waiting status after turn: agent={}, workspace={}",
                agent.getId(),
                agent.getWorkspaceId(),
                e
            );
            return false;
        }
    }

    private void autoPersistWritingResultAndReply(
        Long primaryGroupId,
        String content
    ) {
        try {
            Optional<ResolvedWritingAssignment> assignmentOptional =
                resolveWritingAssignment();
            if (assignmentOptional.isEmpty()) {
                log.info(
                    "[Swarm] No writing assignment resolved for implicit result fallback: agent={}, workspace={}",
                    agent.getId(),
                    agent.getWorkspaceId()
                );
                autoReplyToParent(primaryGroupId, content, null);
                return;
            }

            ResolvedWritingAssignment assignment = assignmentOptional.get();
            List<WritingResult> existingResults =
                writingResultService.listByTask(assignment.task().getId());
            WritingResult latestResult = existingResults
                .stream()
                .max(
                    Comparator.comparing(
                        WritingResult::getCreatedAt,
                        Comparator.nullsLast(LocalDateTime::compareTo)
                    )
                )
                .orElse(null);

            String summary = buildAutoSummary(assignment.task(), content);

            if (latestResult == null) {
                writingTaskService.markCompleted(assignment.task().getId());
                writingAgentCoordinatorService.updateStatus(
                    assignment.writingAgent().getId(),
                    "DONE"
                );
                latestResult = writingResultService.createResult(
                    assignment.session().getId(),
                    assignment.task().getId(),
                    assignment.writingAgent().getId(),
                    assignment.task().getSwarmAgentId(),
                    normalizeResultType(assignment.task().getTaskType()),
                    summary,
                    content,
                    null
                );
                log.info(
                    "[Swarm] Implicit writing_result persisted: agent={}, sessionId={}, taskId={}, writingAgentId={}, resultId={}",
                    agent.getId(),
                    assignment.session().getId(),
                    assignment.task().getId(),
                    assignment.writingAgent().getId(),
                    latestResult.getId()
                );
            } else {
                log.info(
                    "[Swarm] Skip implicit writing_result because task already has result: agent={}, taskId={}, resultId={}",
                    agent.getId(),
                    assignment.task().getId(),
                    latestResult.getId()
                );
            }

            autoReplyToParent(primaryGroupId, content, assignment.task());
        } catch (Exception e) {
            log.warn(
                "[Swarm] Failed implicit writing result fallback: agent={}, workspace={}",
                agent.getId(),
                agent.getWorkspaceId(),
                e
            );
            autoReplyToParent(primaryGroupId, content, null);
        }
    }

    private Optional<ResolvedWritingAssignment> resolveWritingAssignment() {
        return writingSessionService
            .listSessions(agent.getWorkspaceId())
            .stream()
            .sorted(
                Comparator.comparing(
                    WritingSession::getCreatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())
                )
            )
            .map(session -> {
                WritingAgent writingAgent = writingAgentCoordinatorService
                    .listAgents(session.getId())
                    .stream()
                    .filter(candidate ->
                        agent.getId().equals(candidate.getSwarmAgentId())
                    )
                    .findFirst()
                    .orElse(null);
                if (writingAgent == null) {
                    return null;
                }

                WritingTask task = writingTaskService
                    .listByWritingAgent(writingAgent.getId())
                    .stream()
                    .sorted(
                        Comparator.comparing((WritingTask candidate) ->
                            taskRank(candidate.getStatus())
                        )
                            .thenComparing(
                                WritingTask::getPriority,
                                Comparator.nullsLast(Comparator.reverseOrder())
                            )
                            .thenComparing(
                                WritingTask::getCreatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())
                            )
                    )
                    .findFirst()
                    .orElse(null);
                if (task == null) {
                    return null;
                }
                return new ResolvedWritingAssignment(
                    session,
                    writingAgent,
                    task
                );
            })
            .filter(java.util.Objects::nonNull)
            .findFirst();
    }

    private void markWritingAssignmentRunningIfNeeded() {
        try {
            Optional<ResolvedWritingAssignment> assignmentOptional =
                resolveWritingAssignment();
            if (assignmentOptional.isEmpty()) {
                return;
            }

            ResolvedWritingAssignment assignment = assignmentOptional.get();
            WritingTask task = assignment.task();
            WritingAgent writingAgent = assignment.writingAgent();

            if (!"RUNNING".equals(task.getStatus())) {
                writingTaskService.markRunning(task.getId());
            }
            if (!"RUNNING".equals(writingAgent.getStatus())) {
                writingAgentCoordinatorService.updateStatus(
                    writingAgent.getId(),
                    "RUNNING"
                );
            }
        } catch (Exception e) {
            log.warn(
                "[Swarm] Failed to mark writing assignment running: agent={}, workspace={}",
                agent.getId(),
                agent.getWorkspaceId(),
                e
            );
        }
    }

    private void autoReplyToParent(
        Long primaryGroupId,
        String content,
        WritingTask task
    ) {
        String replyContent =
            task == null
                ? content
                : "任务 " +
                  task.getId() +
                  "（taskUuid=" +
                  task.getTaskUuid() +
                  "）" +
                  "《" +
                  (task.getTitle() == null ? "未命名任务" : task.getTitle()) +
                  "》已完成。\n摘要：" +
                  buildAutoSummary(task, content) +
                  "\n\n" +
                  content;
        publishTextMessage(primaryGroupId, replyContent);
        log.info(
            "[Swarm] Implicit reply published to parent: agent={}, groupId={}, taskId={}, preview={}",
            agent.getId(),
            primaryGroupId,
            task != null ? task.getId() : null,
            preview(replyContent)
        );
    }

    private void publishTextMessage(Long groupId, String content) {
        SendMessageRequest request = new SendMessageRequest();
        request.setSenderId(agent.getId());
        request.setContentType("text");
        request.setContent(content);
        messageService.sendMessage(groupId, request);
    }

    private String buildAutoSummary(WritingTask task, String content) {
        String titlePrefix =
            task != null && task.getTitle() != null
                ? task.getTitle() + "："
                : "";
        return titlePrefix + preview(content);
    }

    private String normalizeResultType(String taskType) {
        if (taskType == null || taskType.isBlank()) {
            return "TEXT";
        }
        return taskType;
    }

    private int taskRank(String status) {
        if ("RUNNING".equals(status)) {
            return 0;
        }
        if ("PENDING".equals(status)) {
            return 1;
        }
        if ("PLANNED".equals(status)) {
            return 2;
        }
        if ("FAILED".equals(status)) {
            return 3;
        }
        if ("DONE".equals(status)) {
            return 4;
        }
        return 5;
    }

    private record ResolvedWritingAssignment(
        WritingSession session,
        WritingAgent writingAgent,
        WritingTask task
    ) {}

    /**
     * 从持久化的 llm_history 中恢复历史消息上下文
     */
    private void loadLlmHistory(List<Message> messages) {
        try {
            SwarmAgent freshAgent = agentRepository
                .findById(agent.getId())
                .orElse(agent);
            String historyJson = freshAgent.getLlmHistory();
            if (historyJson == null || historyJson.isBlank()) return;

            com.fasterxml.jackson.databind.JsonNode historyArray =
                objectMapper.readTree(historyJson);
            if (!historyArray.isArray()) return;

            for (com.fasterxml.jackson.databind.JsonNode entry : historyArray) {
                String role = entry.has("role")
                    ? entry.get("role").asText()
                    : "";
                String content = entry.has("content")
                    ? entry.get("content").asText()
                    : "";
                if (content.isEmpty()) continue;
                switch (role) {
                    case "user", "USER" -> messages.add(
                        SwarmLlmCaller.userMessage(content)
                    );
                    case "assistant", "ASSISTANT" -> messages.add(
                        SwarmLlmCaller.assistantMessage(content)
                    );
                    default -> {
                    } // skip system messages (already added)
                }
            }
            log.debug(
                "[Swarm] Loaded {} history entries for agent {}",
                historyArray.size(),
                agent.getId()
            );
        } catch (Exception e) {
            log.warn(
                "[Swarm] Failed to load llm_history for agent {}",
                agent.getId(),
                e
            );
        }
    }

    /**
     * 落库 llm_history
     */
    private void saveLlmHistory(List<Message> messages) {
        try {
            String historyJson = objectMapper.writeValueAsString(
                messages
                    .stream()
                    .map(m ->
                        Map.of(
                            "role",
                            m.getMessageType().getValue(),
                            "content",
                            m.getText() != null ? m.getText() : ""
                        )
                    )
                    .toList()
            );
            agentRepository.updateLlmHistory(agent.getId(), historyJson);
        } catch (Exception e) {
            log.error(
                "[Swarm] Failed to save llm_history for agent {}",
                agent.getId(),
                e
            );
        }
    }

    /**
     * 查找当前 agent 和 human 的 P2P 群 ID
     */
    private Long findHumanGroupId() {
        if (humanAgentId == null) return null;
        try {
            var groups = groupRepository.findByWorkspaceId(
                agent.getWorkspaceId()
            );
            for (var group : groups) {
                List<Long> memberIds = groupRepository.findMemberIds(
                    group.getId()
                );
                if (
                    memberIds.size() == 2 &&
                    memberIds.contains(agent.getId()) &&
                    memberIds.contains(humanAgentId)
                ) {
                    return group.getId();
                }
            }
        } catch (Exception e) {
            log.warn(
                "[Swarm] Failed to find human group for agent {}",
                agent.getId(),
                e
            );
        }
        return null;
    }

    /**
     * 构建 LLM 消息列表
     */
    private List<Message> buildMessages(
        Map<Long, List<SwarmMessage>> unreadByGroup
    ) {
        List<Message> messages = new ArrayList<>();

        // System prompt: Root 用协调者模板，Sub 用执行者模板
        String systemPrompt;
        if (isRoot) {
            systemPrompt = SwarmPromptTemplate.buildRootPrompt(
                agent.getId(),
                agent.getWorkspaceId(),
                agent.getRole(),
                humanAgentId
            );
        } else {
            systemPrompt = SwarmPromptTemplate.buildSubPrompt(
                agent.getId(),
                agent.getWorkspaceId(),
                agent.getRole(),
                humanAgentId,
                agent.getParentId()
            );
        }
        messages.add(SwarmLlmCaller.systemMessage(systemPrompt));

        loadLlmHistory(messages);

        // 未读消息作为 user messages
        for (Map.Entry<
            Long,
            List<SwarmMessage>
        > entry : unreadByGroup.entrySet()) {
            Long groupId = entry.getKey();
            for (SwarmMessage msg : entry.getValue()) {
                String prefix =
                    "[group:" +
                    groupId +
                    " from:agent_" +
                    msg.getSenderId() +
                    "] ";
                messages.add(
                    SwarmLlmCaller.userMessage(prefix + msg.getContent())
                );
            }
        }

        log.info(
            "[Swarm] Built LLM messages: agent={}, workspace={}, unreadGroups={}, unreadMessages={}, totalPromptMessages={}",
            agent.getId(),
            agent.getWorkspaceId(),
            unreadByGroup.size(),
            countMessages(unreadByGroup),
            messages.size()
        );

        return messages;
    }

    private void emitEvent(String type, String subType, String data) {
        agentEventBus.emit(
            agent.getId(),
            SwarmAgentEventBus.AgentEvent.builder()
                .type(type)
                .subType(subType)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build()
        );
    }

    private void emitUIEvent(String type, String data) {
        uiEventBus.emit(
            agent.getWorkspaceId(),
            SwarmUIEventBus.UIEvent.builder()
                .type(type)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build()
        );
    }

    private void emitToolCallUIEvent(
        String type,
        Long groupId,
        String toolCallId,
        String toolName,
        String toolArgs,
        String toolResult
    ) {
        try {
            String payload = objectMapper.writeValueAsString(
                Map.of(
                    "agentId",
                    agent.getId(),
                    "groupId",
                    groupId,
                    "toolCallId",
                    toolCallId,
                    "tool",
                    toolName,
                    "argsPreview",
                    preview(toolArgs),
                    "resultPreview",
                    preview(toolResult)
                )
            );
            emitUIEvent(type, payload);
        } catch (Exception e) {
            log.warn(
                "[Swarm] Failed to emit tool call UI event: agent={}, groupId={}, tool={}",
                agent.getId(),
                groupId,
                toolName,
                e
            );
        }
    }

    private void emitStreamStart(Long groupId) {
        if (groupId == null) {
            return;
        }
        emitUIEvent(
            "ui.agent.stream.start",
            "{\"agentId\":" + agent.getId() + ",\"groupId\":" + groupId + "}"
        );
    }

    private void emitStreamDone(Long groupId) {
        if (groupId == null) {
            return;
        }
        emitUIEvent(
            "ui.agent.stream.done",
            "{\"agentId\":" + agent.getId() + ",\"groupId\":" + groupId + "}"
        );
    }

    private void emitContentChunk(
        Long groupId,
        String chunk,
        boolean emitUiChunk
    ) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        emitEvent("agent.stream", "content", chunk);
        if (!emitUiChunk || groupId == null) {
            return;
        }
        try {
            String chunkData = objectMapper.writeValueAsString(
                Map.of(
                    "agentId",
                    agent.getId(),
                    "groupId",
                    groupId,
                    "chunk",
                    chunk
                )
            );
            emitUIEvent("ui.agent.stream.chunk", chunkData);
        } catch (Exception e) {
            log.warn(
                "[Swarm] Failed to emit stream chunk: agent={}, groupId={}",
                agent.getId(),
                groupId,
                e
            );
        }
    }

    private List<AssistantMessage.ToolCall> filterExecutableToolCalls(
        List<AssistantMessage.ToolCall> toolCalls
    ) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return List.of();
        }
        List<AssistantMessage.ToolCall> executable = new ArrayList<>();
        for (AssistantMessage.ToolCall toolCall : toolCalls) {
            if (toolCall == null) {
                continue;
            }
            if (toolCall.name() == null || toolCall.name().isBlank()) {
                log.warn(
                    "[Swarm] Ignoring tool call without name: agent={}, toolCallId={}, argsPreview={}",
                    agent.getId(),
                    toolCall.id(),
                    preview(toolCall.arguments())
                );
                continue;
            }
            if (
                SwarmToolArgumentUtils.tryParseArgumentsObject(
                    toolCall.arguments(),
                    objectMapper
                ) ==
                null
            ) {
                log.warn(
                    "[Swarm] Ignoring tool call with incomplete arguments: agent={}, tool={}, argsPreview={}",
                    agent.getId(),
                    toolCall.name(),
                    preview(toolCall.arguments())
                );
                continue;
            }
            executable.add(toolCall);
        }
        return executable;
    }

    /**
     * 通过 SwarmTools 执行工具调用
     */
    private String executeToolCall(String toolName, String toolArgs) {
        try {
            log.info(
                "[Swarm] Executing tool call: agent={}, workspace={}, role={}, isRoot={}, tool={}, argsPreview={}",
                agent.getId(),
                agent.getWorkspaceId(),
                agent.getRole(),
                isRoot,
                toolName,
                preview(toolArgs)
            );
            if (!isRoot && SUB_AGENT_FORBIDDEN_TOOLS.contains(toolName)) {
                log.warn(
                    "[Swarm] Sub agent {} attempted forbidden tool: {}",
                    agent.getId(),
                    toolName
                );
                return errorJson(
                    "你是执行者 Agent，不允许使用 " +
                        toolName +
                        " 工具。你只能使用 writing_result_by_task_uuid、writing_result_by_task、writing_result、send 和 self。"
                );
            }

            JsonNode args = SwarmToolArgumentUtils.parseArgumentsObject(
                toolName,
                toolArgs,
                objectMapper
            );
            String result = switch (toolName) {
                case "createAgent" -> swarmTools.createAgent(
                    SwarmToolArgumentUtils.getOptionalText(args, "role") != null
                        ? SwarmToolArgumentUtils.getOptionalText(args, "role")
                        : "assistant",
                    SwarmToolArgumentUtils.getOptionalText(
                            args,
                            "description"
                        ) !=
                        null
                        ? SwarmToolArgumentUtils.getOptionalText(
                              args,
                              "description"
                          )
                        : "",
                    SwarmToolArgumentUtils.getOptionalText(args, "graphJson")
                );
                case "executeWorkflow" -> swarmTools.executeWorkflow(
                    SwarmToolArgumentUtils.getRequiredLong(
                        args,
                        "agentId",
                        "agent_id"
                    ),
                    SwarmToolArgumentUtils.getOptionalText(args, "input")
                );
                case "send" -> swarmTools.send(
                    SwarmToolArgumentUtils.getRequiredLong(
                        args,
                        "agentId",
                        "agent_id"
                    ),
                    SwarmToolArgumentUtils.getRequiredText(args, "message")
                );
                case "writing_session" -> swarmTools.writing_session(
                    SwarmToolArgumentUtils.getRequiredText(args, "title"),
                    SwarmToolArgumentUtils.getRequiredText(args, "goal"),
                    SwarmToolArgumentUtils.getOptionalText(
                        args,
                        "constraintsJson",
                        "constraints_json"
                    )
                );
                case "writing_agent" -> swarmTools.writing_agent(
                    SwarmToolArgumentUtils.getRequiredLong(
                        args,
                        "sessionId",
                        "session_id"
                    ),
                    SwarmToolArgumentUtils.getRequiredText(args, "role"),
                    SwarmToolArgumentUtils.getRequiredText(args, "description"),
                    SwarmToolArgumentUtils.getOptionalText(
                        args,
                        "skillTagsJson",
                        "skill_tags_json"
                    ),
                    SwarmToolArgumentUtils.getOptionalInt(
                        args,
                        "sortOrder",
                        "sort_order"
                    )
                );
                case "writing_task" -> swarmTools.writing_task(
                    SwarmToolArgumentUtils.getRequiredLong(
                        args,
                        "sessionId",
                        "session_id"
                    ),
                    SwarmToolArgumentUtils.getRequiredLong(
                        args,
                        "writingAgentId",
                        "writing_agent_id"
                    ),
                    SwarmToolArgumentUtils.getRequiredLong(
                        args,
                        "swarmAgentId",
                        "swarm_agent_id"
                    ),
                    SwarmToolArgumentUtils.getRequiredText(
                        args,
                        "taskType",
                        "task_type"
                    ),
                    SwarmToolArgumentUtils.getRequiredText(args, "title"),
                    SwarmToolArgumentUtils.getRequiredText(args, "instruction"),
                    SwarmToolArgumentUtils.getOptionalText(
                        args,
                        "inputPayloadJson",
                        "input_payload_json"
                    ),
                    SwarmToolArgumentUtils.getOptionalText(
                        args,
                        "expectedOutputSchemaJson",
                        "expected_output_schema_json"
                    ),
                    SwarmToolArgumentUtils.getOptionalInt(args, "priority")
                );
                case "writing_result" -> swarmTools.writing_result(
                    SwarmToolArgumentUtils.getRequiredLong(
                        args,
                        "sessionId",
                        "session_id"
                    ),
                    SwarmToolArgumentUtils.getRequiredLong(
                        args,
                        "taskId",
                        "task_id"
                    ),
                    SwarmToolArgumentUtils.getRequiredLong(
                        args,
                        "writingAgentId",
                        "writing_agent_id"
                    ),
                    SwarmToolArgumentUtils.getRequiredText(
                        args,
                        "resultType",
                        "result_type"
                    ),
                    SwarmToolArgumentUtils.getRequiredText(args, "summary"),
                    SwarmToolArgumentUtils.getRequiredText(args, "content"),
                    SwarmToolArgumentUtils.getOptionalText(
                        args,
                        "structuredPayloadJson",
                        "structured_payload_json"
                    )
                );
                case "writing_result_by_task" -> swarmTools.writing_result_by_task(
                    SwarmToolArgumentUtils.getRequiredLong(
                        args,
                        "taskId",
                        "task_id"
                    ),
                    SwarmToolArgumentUtils.getRequiredText(
                        args,
                        "resultType",
                        "result_type"
                    ),
                    SwarmToolArgumentUtils.getRequiredText(args, "summary"),
                    SwarmToolArgumentUtils.getRequiredText(args, "content"),
                    SwarmToolArgumentUtils.getOptionalText(
                        args,
                        "structuredPayloadJson",
                        "structured_payload_json"
                    )
                );
                case "writing_result_by_task_uuid" -> swarmTools.writing_result_by_task_uuid(
                    SwarmToolArgumentUtils.getRequiredText(
                        args,
                        "taskUuid",
                        "task_uuid"
                    ),
                    SwarmToolArgumentUtils.getRequiredText(
                        args,
                        "resultType",
                        "result_type"
                    ),
                    SwarmToolArgumentUtils.getRequiredText(args, "summary"),
                    SwarmToolArgumentUtils.getRequiredText(args, "content"),
                    SwarmToolArgumentUtils.getOptionalText(
                        args,
                        "structuredPayloadJson",
                        "structured_payload_json"
                    )
                );
                case "writing_draft" -> swarmTools.writing_draft(
                    SwarmToolArgumentUtils.getRequiredLong(
                        args,
                        "sessionId",
                        "session_id"
                    ),
                    SwarmToolArgumentUtils.getRequiredInt(
                        args,
                        "versionNo",
                        "version_no"
                    ),
                    SwarmToolArgumentUtils.getRequiredText(args, "title"),
                    SwarmToolArgumentUtils.getRequiredText(args, "content"),
                    SwarmToolArgumentUtils.getOptionalText(
                        args,
                        "sourceResultIdsJson",
                        "source_result_ids_json"
                    ),
                    SwarmToolArgumentUtils.getOptionalText(args, "status")
                );
                case "self" -> swarmTools.self();
                case "listAgents" -> swarmTools.listAgents();
                default -> errorJson("Unknown tool: " + toolName);
            };
            log.info(
                "[Swarm] Tool execution succeeded: agent={}, tool={}, resultPreview={}",
                agent.getId(),
                toolName,
                preview(result)
            );
            return result;
        } catch (IllegalArgumentException e) {
            log.warn(
                "[Swarm] Invalid tool arguments: tool={}, agent={}, args={}",
                toolName,
                agent.getId(),
                toolArgs
            );
            return errorJson(e.getMessage());
        } catch (Exception e) {
            log.error(
                "[Swarm] Tool execution failed: tool={}, agent={}",
                toolName,
                agent.getId(),
                e
            );
            return errorJson(e.getMessage());
        }
    }

    /**
     * 存 tool_call 消息到 group，并 emit UI 事件让前端实时显示
     */
    private void saveToolCallMessage(
        Long groupId,
        String toolName,
        String toolArgs,
        String toolResult
    ) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("tool", toolName);
            payload.put(
                "args",
                SwarmToolArgumentUtils.normalizeForStorage(
                    toolArgs,
                    objectMapper
                )
            );
            payload.put("result", toolResult == null ? "" : toolResult);
            String content = objectMapper.writeValueAsString(payload);
            SwarmMessage msg = SwarmMessage.builder()
                .workspaceId(agent.getWorkspaceId())
                .groupId(groupId)
                .senderId(agent.getId())
                .contentType("tool_call")
                .content(content)
                .sendTime(LocalDateTime.now())
                .build();
            messageRepository.save(msg);
            log.info(
                "[Swarm] Tool call message persisted: agent={}, groupId={}, messageId={}, tool={}, resultPreview={}",
                agent.getId(),
                groupId,
                msg.getId(),
                toolName,
                preview(toolResult)
            );

            // emit UI 事件
            uiEventBus.emit(
                agent.getWorkspaceId(),
                SwarmUIEventBus.UIEvent.builder()
                    .type("ui.message.created")
                    .data(
                        "{\"groupId\":" +
                            groupId +
                            ",\"messageId\":" +
                            msg.getId() +
                            ",\"senderId\":" +
                            agent.getId() +
                            "}"
                    )
                    .timestamp(System.currentTimeMillis())
                    .build()
            );
        } catch (Exception e) {
            log.warn(
                "[Swarm] Failed to save tool_call message: agent={}, tool={}",
                agent.getId(),
                toolName,
                e
            );
        }
    }

    private String errorJson(String message) {
        String safeMessage = message == null ? "unknown error" : message;
        try {
            return objectMapper.writeValueAsString(
                Map.of("error", safeMessage)
            );
        } catch (Exception ignored) {
            return "{\"error\":\"" + safeMessage.replace("\"", "\\\"") + "\"}";
        }
    }

    private int countMessages(Map<Long, List<SwarmMessage>> messagesByGroup) {
        return messagesByGroup.values().stream().mapToInt(List::size).sum();
    }

    private String preview(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 160
            ? normalized
            : normalized.substring(0, 160) + "...";
    }
}
