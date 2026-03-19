package com.zj.aiagent.application.swarm.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.application.agent.cmd.AgentCommand;
import com.zj.aiagent.application.agent.dto.AgentDetailResult;
import com.zj.aiagent.application.agent.service.AgentApplicationService;
import com.zj.aiagent.application.swarm.SwarmMessageService;
import com.zj.aiagent.application.swarm.SwarmWorkspaceService;
import com.zj.aiagent.application.swarm.dto.*;
import com.zj.aiagent.application.workflow.SchedulerService;
import com.zj.aiagent.application.writing.WritingAgentCoordinatorService;
import com.zj.aiagent.application.writing.WritingDraftService;
import com.zj.aiagent.application.writing.WritingResultService;
import com.zj.aiagent.application.writing.WritingSessionService;
import com.zj.aiagent.application.writing.WritingTaskService;
import com.zj.aiagent.domain.swarm.entity.SwarmAgent;
import com.zj.aiagent.domain.swarm.repository.SwarmAgentRepository;
import com.zj.aiagent.domain.writing.entity.WritingAgent;
import com.zj.aiagent.domain.writing.entity.WritingDraft;
import com.zj.aiagent.domain.writing.entity.WritingResult;
import com.zj.aiagent.domain.writing.entity.WritingSession;
import com.zj.aiagent.domain.writing.entity.WritingTask;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 蜂群内置工具（@Tool 注解方式）
 * 每个 AgentRunner 创建一个实例，绑定 callerAgent 上下文
 */
@Slf4j
public class SwarmTools {

    private static final Pattern TASK_UUID_PATTERN = Pattern.compile(
        "(wtask_[A-Za-z0-9]+)"
    );

    private final SwarmWorkspaceService workspaceService;
    private final SwarmMessageService messageService;
    private final SwarmAgentRepository swarmAgentRepository;
    private final AgentApplicationService workflowAgentService;
    private final SchedulerService schedulerService;
    private final WritingSessionService writingSessionService;
    private final WritingAgentCoordinatorService writingAgentCoordinatorService;
    private final WritingTaskService writingTaskService;
    private final WritingResultService writingResultService;
    private final WritingDraftService writingDraftService;
    private final ObjectMapper objectMapper;
    private final Long callerAgentId;
    private final Long callerWorkspaceId;
    private final Long callerUserId;

    public SwarmTools(
        SwarmWorkspaceService workspaceService,
        SwarmMessageService messageService,
        SwarmAgentRepository swarmAgentRepository,
        AgentApplicationService workflowAgentService,
        SchedulerService schedulerService,
        WritingSessionService writingSessionService,
        WritingAgentCoordinatorService writingAgentCoordinatorService,
        WritingTaskService writingTaskService,
        WritingResultService writingResultService,
        WritingDraftService writingDraftService,
        ObjectMapper objectMapper,
        Long callerAgentId,
        Long callerWorkspaceId,
        Long callerUserId
    ) {
        this.workspaceService = workspaceService;
        this.messageService = messageService;
        this.swarmAgentRepository = swarmAgentRepository;
        this.workflowAgentService = workflowAgentService;
        this.schedulerService = schedulerService;
        this.writingSessionService = writingSessionService;
        this.writingAgentCoordinatorService = writingAgentCoordinatorService;
        this.writingTaskService = writingTaskService;
        this.writingResultService = writingResultService;
        this.writingDraftService = writingDraftService;
        this.objectMapper = objectMapper;
        this.callerAgentId = callerAgentId;
        this.callerWorkspaceId = callerWorkspaceId;
        this.callerUserId = callerUserId;
    }

    @Tool(
        description = "创建一个可执行工作流的 Agent。graphJson 可直接写入工作流定义。当前模式下优先用它来创建工作流，而不是聊天子Agent。"
    )
    public String createAgent(
        @ToolParam(
            description = "子Agent的角色名称，如 researcher/writer/analyst"
        ) String role,
        @ToolParam(
            description = "子Agent的能力边界和职责描述"
        ) String description,
        @ToolParam(
            description = "可选的工作流图JSON，如果不需要工作流可传null"
        ) String graphJson
    ) {
        try {
            log.info(
                "[SwarmTools] createAgent invoked: callerAgentId={}, workspaceId={}, role={}, descriptionPreview={}",
                callerAgentId,
                callerWorkspaceId,
                role,
                preview(description)
            );
            String normalizedRole = (role == null || role.isBlank())
                ? "workflow_agent"
                : role.trim();
            String agentName =
                normalizedRole + "_" + System.currentTimeMillis();
            String effectiveGraphJson = resolveGraphJson(
                graphJson,
                description
            );
            boolean usedFallbackGraph =
                effectiveGraphJson != null &&
                (graphJson == null || !effectiveGraphJson.equals(graphJson));

            AgentCommand.CreateAgentCmd createCmd =
                new AgentCommand.CreateAgentCmd();
            createCmd.setUserId(callerUserId);
            createCmd.setName(agentName);
            createCmd.setDescription(description);
            createCmd.setIcon("robot");

            Long workflowAgentId = workflowAgentService.createAgent(createCmd);

            if (effectiveGraphJson != null && !effectiveGraphJson.isBlank()) {
                AgentDetailResult detail = workflowAgentService.getAgentDetail(
                    workflowAgentId,
                    callerUserId
                );
                AgentCommand.UpdateAgentCmd updateCmd =
                    new AgentCommand.UpdateAgentCmd();
                updateCmd.setId(workflowAgentId);
                updateCmd.setUserId(callerUserId);
                updateCmd.setName(detail.getName());
                updateCmd.setDescription(detail.getDescription());
                updateCmd.setIcon(detail.getIcon());
                updateCmd.setGraphJson(effectiveGraphJson);
                updateCmd.setVersion(detail.getVersion());
                workflowAgentService.updateAgent(updateCmd);
            }

            AgentDetailResult created = workflowAgentService.getAgentDetail(
                workflowAgentId,
                callerUserId
            );
            log.info(
                "[SwarmTools] createAgent completed: callerAgentId={}, workspaceId={}, workflowAgentId={}, name={}, graphMode={}",
                callerAgentId,
                callerWorkspaceId,
                created.getId(),
                created.getName(),
                usedFallbackGraph ? "auto_generated" : "provided"
            );
            return objectMapper.writeValueAsString(
                java.util.Map.of(
                    "agentId",
                    created.getId(),
                    "name",
                    created.getName(),
                    "description",
                    created.getDescription() != null
                        ? created.getDescription()
                        : "",
                    "version",
                    created.getVersion(),
                    "graphReady",
                    created.getGraphJson() != null &&
                        !created.getGraphJson().isBlank(),
                    "graphMode",
                    usedFallbackGraph ? "auto_generated" : "provided",
                    "mode",
                    "workflow_agent"
                )
            );
        } catch (Exception e) {
            log.error("[SwarmTools] createAgent failed", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @Tool(
        description = "执行某个工作流 Agent，并等待执行完成后返回结果。适合主 AI 同步等待工作流输出。"
    )
    public String executeWorkflow(
        @ToolParam(description = "要执行的Agent ID") long agentId,
        @ToolParam(description = "可选的输入内容") String input
    ) {
        try {
            log.info(
                "[SwarmTools] executeWorkflow invoked: callerAgentId={}, workspaceId={}, targetAgentId={}, inputPreview={}",
                callerAgentId,
                callerWorkspaceId,
                agentId,
                preview(input)
            );
            java.util.Map<String, Object> inputs =
                new java.util.LinkedHashMap<>();
            if (input != null && !input.isBlank()) {
                inputs.put("input", input);
            }

            java.util.Map<String, Object> result =
                schedulerService.executeAndWait(
                    agentId,
                    callerUserId,
                    inputs,
                    com.zj.aiagent.domain.workflow.valobj.ExecutionMode.STANDARD,
                    10 * 60 * 1000L
                );
            log.info(
                "[SwarmTools] executeWorkflow completed: callerAgentId={}, targetAgentId={}, resultPreview={}",
                callerAgentId,
                agentId,
                preview(objectMapper.writeValueAsString(result))
            );
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("[SwarmTools] executeWorkflow failed", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @Tool(
        description = "向指定Agent发送消息。用于委派任务给子Agent或回复其他Agent的消息。注意：不要用此工具回复人类。"
    )
    public String send(
        @ToolParam(description = "目标Agent的ID") long agentId,
        @ToolParam(description = "消息内容") String message
    ) {
        try {
            log.info(
                "[SwarmTools] send invoked: callerAgentId={}, workspaceId={}, targetAgentId={}, messagePreview={}",
                callerAgentId,
                callerWorkspaceId,
                agentId,
                preview(message)
            );
            List<SwarmGroupDTO> groups = messageService.listGroups(
                callerWorkspaceId,
                callerAgentId
            );
            Long groupId = null;
            for (SwarmGroupDTO g : groups) {
                if (
                    g.getMemberIds() != null &&
                    g.getMemberIds().contains(agentId)
                ) {
                    groupId = g.getId();
                    break;
                }
            }

            if (groupId == null) {
                return (
                    "{\"error\": \"No group found with agent " + agentId + "\"}"
                );
            }

            SendMessageRequest req = new SendMessageRequest();
            req.setSenderId(callerAgentId);
            req.setContentType("text");
            req.setContent(message);
            SwarmMessageDTO sent = messageService.sendMessage(groupId, req);
            markWritingTaskDispatchedIfPresent(agentId, message);
            log.info(
                "[SwarmTools] send completed: callerAgentId={}, targetAgentId={}, groupId={}, messageId={}",
                callerAgentId,
                agentId,
                groupId,
                sent.getId()
            );
            return objectMapper.writeValueAsString(sent);
        } catch (Exception e) {
            log.error("[SwarmTools] send failed", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    private void markWritingTaskDispatchedIfPresent(
        long targetAgentId,
        String message
    ) {
        String taskUuid = extractTaskUuid(message);
        if (taskUuid == null) {
            return;
        }
        try {
            WritingTask task = writingTaskService.getTaskByUuid(taskUuid);
            if (
                task.getSwarmAgentId() == null ||
                !task.getSwarmAgentId().equals(targetAgentId)
            ) {
                log.info(
                    "[SwarmTools] Skip dispatch mark because task/swarm mismatch: taskUuid={}, expectedSwarmAgentId={}, targetAgentId={}",
                    taskUuid,
                    task.getSwarmAgentId(),
                    targetAgentId
                );
                return;
            }
            if (!"PLANNED".equals(task.getStatus())) {
                return;
            }
            writingTaskService.markDispatchedByUuid(taskUuid);
        } catch (Exception e) {
            log.warn(
                "[SwarmTools] Failed to mark writing task dispatched: taskUuid={}, targetAgentId={}",
                taskUuid,
                targetAgentId,
                e
            );
        }
    }

    private String extractTaskUuid(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        Matcher matcher = TASK_UUID_PATTERN.matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }

    @Tool(
        description = "返回自身信息，包括 agent_id、workspace_id、角色、状态等"
    )
    public String self() {
        try {
            SwarmAgent agent = swarmAgentRepository
                .findById(callerAgentId)
                .orElse(null);
            if (agent == null) return "{\"error\": \"Agent not found\"}";
            return objectMapper.writeValueAsString(
                SwarmAgentDTO.builder()
                    .id(agent.getId())
                    .workspaceId(agent.getWorkspaceId())
                    .role(agent.getRole())
                    .description(agent.getDescription())
                    .parentId(agent.getParentId())
                    .status(
                        agent.getStatus() != null
                            ? agent.getStatus().getCode()
                            : "IDLE"
                    )
                    .build()
            );
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @Tool(
        description = "列出当前workspace中所有Agent，包括它们的ID、角色、状态和父子关系"
    )
    public String listAgents() {
        try {
            List<SwarmAgentDTO> agents = workspaceService.listAgents(
                callerWorkspaceId
            );
            log.info(
                "[SwarmTools] listAgents completed: callerAgentId={}, workspaceId={}, count={}",
                callerAgentId,
                callerWorkspaceId,
                agents.size()
            );
            return objectMapper.writeValueAsString(agents);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @Tool(
        description = "记录一次写作任务，会创建 writing_session。主 Agent 通常在开始多智能体写作前先调用它。"
    )
    public String writing_session(
        @ToolParam(description = "本次写作任务标题") String title,
        @ToolParam(description = "本次写作目标描述") String goal,
        @ToolParam(
            description = "可选的 JSON 字符串，记录约束，如题材、篇幅、风格要求。没有可传 null"
        ) String constraintsJson
    ) {
        try {
            log.info(
                "[SwarmTools] writing_session invoked: callerAgentId={}, workspaceId={}, title={}, goalPreview={}, constraintsPreview={}",
                callerAgentId,
                callerWorkspaceId,
                title,
                preview(goal),
                preview(constraintsJson)
            );
            Long humanAgentId = swarmAgentRepository
                .findByWorkspaceId(callerWorkspaceId)
                .stream()
                .filter(agent -> "human".equals(agent.getRole()))
                .map(SwarmAgent::getId)
                .findFirst()
                .orElse(null);
            Long defaultGroupId = workspaceService
                .getDefaults(callerWorkspaceId)
                .getDefaultGroupId();

            WritingSession session = writingSessionService.createSession(
                callerWorkspaceId,
                callerAgentId,
                humanAgentId,
                defaultGroupId,
                title,
                goal,
                parseJsonOrNull(constraintsJson)
            );
            log.info(
                "[SwarmTools] writing_session completed: sessionId={}, workspaceId={}, rootAgentId={}",
                session.getId(),
                session.getWorkspaceId(),
                session.getRootAgentId()
            );
            return objectMapper.writeValueAsString(session);
        } catch (Exception e) {
            log.error("[SwarmTools] writing_session failed", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @Tool(
        description = "创建一个写作子 Agent。它会先调用现有 swarm 创建 agent，再同步记录 writing_agent。"
    )
    public String writing_agent(
        @ToolParam(description = "写作会话ID") long sessionId,
        @ToolParam(
            description = "子 Agent 的角色名称，如 world_builder/character_writer"
        ) String role,
        @ToolParam(description = "子 Agent 的职责描述") String description,
        @ToolParam(
            description = "可选的 JSON 数组字符串，记录技能标签。没有可传 null"
        ) String skillTagsJson,
        @ToolParam(
            description = "可选排序值，越小越靠前。没有可传 0"
        ) Integer sortOrder
    ) {
        try {
            Long resolvedSessionId = resolveSessionIdForCaller(sessionId);
            log.info(
                "[SwarmTools] writing_agent invoked: callerAgentId={}, requestedSessionId={}, resolvedSessionId={}, role={}, descriptionPreview={}, skillTagsPreview={}",
                callerAgentId,
                sessionId,
                resolvedSessionId,
                role,
                preview(description),
                preview(skillTagsJson)
            );
            WritingAgent agent =
                writingAgentCoordinatorService.createWritingAgent(
                    resolvedSessionId,
                    callerWorkspaceId,
                    role,
                    callerAgentId,
                    description,
                    parseJsonOrNull(skillTagsJson),
                    sortOrder
                );
            log.info(
                "[SwarmTools] writing_agent completed: sessionId={}, writingAgentId={}, swarmAgentId={}, role={}",
                resolvedSessionId,
                agent.getId(),
                agent.getSwarmAgentId(),
                agent.getRole()
            );
            return objectMapper.writeValueAsString(agent);
        } catch (Exception e) {
            log.error("[SwarmTools] writing_agent failed", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @Tool(
        description = "记录主 Agent 拆分出的子任务。建议先创建 writing_task，再通过 send 给子 Agent 派发任务。"
    )
    public String writing_task(
        @ToolParam(description = "写作会话ID") long sessionId,
        @ToolParam(description = "writing_agent 的ID") long writingAgentId,
        @ToolParam(
            description = "负责执行该任务的 swarm agent ID"
        ) long swarmAgentId,
        @ToolParam(
            description = "任务类型，如 OUTLINE/CHARACTER/CHAPTER/REVIEW"
        ) String taskType,
        @ToolParam(description = "任务标题") String title,
        @ToolParam(description = "任务说明") String instruction,
        @ToolParam(
            description = "可选的 JSON 字符串，记录任务输入。没有可传 null"
        ) String inputPayloadJson,
        @ToolParam(
            description = "可选的 JSON 字符串，记录期望输出结构。没有可传 null"
        ) String expectedOutputSchemaJson,
        @ToolParam(description = "优先级，没有可传 0") Integer priority
    ) {
        try {
            Long resolvedSessionId = resolveSessionIdForCaller(sessionId);
            Long resolvedWritingAgentId = resolveWritingAgentIdForSession(
                writingAgentId,
                resolvedSessionId,
                swarmAgentId
            );
            log.info(
                "[SwarmTools] writing_task invoked: callerAgentId={}, requestedSessionId={}, resolvedSessionId={}, requestedWritingAgentId={}, resolvedWritingAgentId={}, swarmAgentId={}, taskType={}, title={}",
                callerAgentId,
                sessionId,
                resolvedSessionId,
                writingAgentId,
                resolvedWritingAgentId,
                swarmAgentId,
                taskType,
                title
            );
            WritingTask task = writingTaskService.createTask(
                resolvedSessionId,
                resolvedWritingAgentId,
                swarmAgentId,
                taskType,
                title,
                instruction,
                parseJsonOrNull(inputPayloadJson),
                parseJsonOrNull(expectedOutputSchemaJson),
                priority,
                callerAgentId
            );
            log.info(
                "[SwarmTools] writing_task completed: taskId={}, taskUuid={}, sessionId={}, writingAgentId={}, swarmAgentId={}",
                task.getId(),
                task.getTaskUuid(),
                resolvedSessionId,
                task.getWritingAgentId(),
                task.getSwarmAgentId()
            );
            return objectMapper.writeValueAsString(task);
        } catch (Exception e) {
            log.error("[SwarmTools] writing_task failed", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    private Long resolveSessionIdForCaller(Long requestedSessionId) {
        List<WritingSession> candidateSessions = writingSessionService
            .listSessions(callerWorkspaceId)
            .stream()
            .filter(session -> callerAgentId.equals(session.getRootAgentId()))
            .sorted(
                Comparator.comparing(
                    WritingSession::getCreatedAt,
                    Comparator.nullsLast(LocalDateTime::compareTo)
                ).reversed()
            )
            .toList();

        if (requestedSessionId != null) {
            for (WritingSession session : candidateSessions) {
                if (requestedSessionId.equals(session.getId())) {
                    return session.getId();
                }
            }
        }

        if (!candidateSessions.isEmpty()) {
            Long fallbackSessionId = candidateSessions.get(0).getId();
            if (
                requestedSessionId != null &&
                !requestedSessionId.equals(fallbackSessionId)
            ) {
                log.warn(
                    "[SwarmTools] Corrected sessionId for caller: callerAgentId={}, workspaceId={}, requestedSessionId={}, fallbackSessionId={}",
                    callerAgentId,
                    callerWorkspaceId,
                    requestedSessionId,
                    fallbackSessionId
                );
            }
            return fallbackSessionId;
        }

        if (requestedSessionId != null) {
            return requestedSessionId;
        }
        throw new IllegalArgumentException(
            "No writing session available for current caller"
        );
    }

    private Long resolveWritingAgentIdForSession(
        Long requestedWritingAgentId,
        Long resolvedSessionId,
        Long swarmAgentId
    ) {
        WritingAgent requestedAgent = writingAgentCoordinatorService.getAgent(
            requestedWritingAgentId
        );
        if (resolvedSessionId.equals(requestedAgent.getSessionId())) {
            return requestedWritingAgentId;
        }

        WritingAgent correctedAgent =
            writingAgentCoordinatorService.findBySessionAndSwarmAgent(
                resolvedSessionId,
                swarmAgentId
            );
        log.warn(
            "[SwarmTools] Corrected writingAgentId for session: callerAgentId={}, workspaceId={}, requestedWritingAgentId={}, correctedWritingAgentId={}, requestedAgentSessionId={}, resolvedSessionId={}, swarmAgentId={}",
            callerAgentId,
            callerWorkspaceId,
            requestedWritingAgentId,
            correctedAgent.getId(),
            requestedAgent.getSessionId(),
            resolvedSessionId,
            swarmAgentId
        );
        return correctedAgent.getId();
    }

    @Tool(
        description = "记录子 Agent 任务结果，同时把对应 writing_task 标记为完成。"
    )
    public String writing_result(
        @ToolParam(description = "写作会话ID") long sessionId,
        @ToolParam(description = "任务ID") long taskId,
        @ToolParam(description = "writing_agent 的ID") long writingAgentId,
        @ToolParam(
            description = "结果类型，如 OUTLINE/SCENE_DRAFT/REVIEW_NOTE/TEXT"
        ) String resultType,
        @ToolParam(description = "结果摘要") String summary,
        @ToolParam(description = "结果正文") String content,
        @ToolParam(
            description = "可选的 JSON 字符串，记录结构化结果。没有可传 null"
        ) String structuredPayloadJson
    ) {
        try {
            log.info(
                "[SwarmTools] writing_result invoked: callerAgentId={}, sessionId={}, taskId={}, writingAgentId={}, resultType={}, summary={}",
                callerAgentId,
                sessionId,
                taskId,
                writingAgentId,
                resultType,
                summary
            );
            WritingResult result = writingResultService.recordTaskResult(
                taskId,
                sessionId,
                writingAgentId,
                resultType,
                summary,
                content,
                parseJsonOrNull(structuredPayloadJson)
            );
            log.info(
                "[SwarmTools] writing_result completed: resultId={}, sessionId={}, taskId={}, swarmAgentId={}",
                result.getId(),
                result.getSessionId(),
                result.getTaskId(),
                result.getSwarmAgentId()
            );
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("[SwarmTools] writing_result failed", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @Tool(
        description = "按 taskId 记录子 Agent 任务结果。推荐子 Agent 优先使用这个工具，避免混淆 sessionId、writingAgentId 与 swarm agentId。"
    )
    public String writing_result_by_task(
        @ToolParam(
            description = "任务ID，系统会自动反查 sessionId、writingAgentId、swarmAgentId"
        ) long taskId,
        @ToolParam(
            description = "结果类型，如 OUTLINE/SCENE_DRAFT/REVIEW_NOTE/TEXT"
        ) String resultType,
        @ToolParam(description = "结果摘要") String summary,
        @ToolParam(description = "结果正文") String content,
        @ToolParam(
            description = "可选的 JSON 字符串，记录结构化结果。没有可传 null"
        ) String structuredPayloadJson
    ) {
        try {
            log.info(
                "[SwarmTools] writing_result_by_task invoked: callerAgentId={}, taskId={}, resultType={}, summary={}",
                callerAgentId,
                taskId,
                resultType,
                summary
            );
            WritingResult result = writingResultService.recordTaskResult(
                taskId,
                null,
                null,
                resultType,
                summary,
                content,
                parseJsonOrNull(structuredPayloadJson)
            );
            log.info(
                "[SwarmTools] writing_result_by_task completed: resultId={}, sessionId={}, taskId={}, swarmAgentId={}",
                result.getId(),
                result.getSessionId(),
                result.getTaskId(),
                result.getSwarmAgentId()
            );
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("[SwarmTools] writing_result_by_task failed", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @Tool(
        description = "按 taskUuid 记录子 Agent 任务结果。强烈推荐子 Agent 优先使用这个工具，只传 taskUuid 即可，避免混淆所有内部数字 ID。"
    )
    public String writing_result_by_task_uuid(
        @ToolParam(
            description = "任务业务唯一标识 taskUuid，由 writing_task 返回，例如 wtask_xxx"
        ) String taskUuid,
        @ToolParam(
            description = "结果类型，如 OUTLINE/SCENE_DRAFT/REVIEW_NOTE/TEXT"
        ) String resultType,
        @ToolParam(description = "结果摘要") String summary,
        @ToolParam(description = "结果正文") String content,
        @ToolParam(
            description = "可选的 JSON 字符串，记录结构化结果。没有可传 null"
        ) String structuredPayloadJson
    ) {
        try {
            log.info(
                "[SwarmTools] writing_result_by_task_uuid invoked: callerAgentId={}, taskUuid={}, resultType={}, summary={}",
                callerAgentId,
                taskUuid,
                resultType,
                summary
            );
            WritingResult result = writingResultService.recordTaskResultByUuid(
                taskUuid,
                resultType,
                summary,
                content,
                parseJsonOrNull(structuredPayloadJson)
            );
            log.info(
                "[SwarmTools] writing_result_by_task_uuid completed: resultId={}, sessionId={}, taskId={}, swarmAgentId={}",
                result.getId(),
                result.getSessionId(),
                result.getTaskId(),
                result.getSwarmAgentId()
            );
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("[SwarmTools] writing_result_by_task_uuid failed", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @Tool(
        description = "保存主 Agent 汇总后的草稿版本，并把该 draft 设为当前版本。"
    )
    public String writing_draft(
        @ToolParam(description = "写作会话ID") long sessionId,
        @ToolParam(description = "草稿版本号，如 1/2/3") int versionNo,
        @ToolParam(description = "草稿标题") String title,
        @ToolParam(description = "草稿正文") String content,
        @ToolParam(
            description = "可选的 JSON 数组字符串，记录来源 writing_result ID 列表。没有可传 null"
        ) String sourceResultIdsJson,
        @ToolParam(description = "草稿状态，可传 DRAFT 或 FINAL") String status
    ) {
        try {
            log.info(
                "[SwarmTools] writing_draft invoked: callerAgentId={}, sessionId={}, versionNo={}, title={}, status={}, sourceResultIdsPreview={}",
                callerAgentId,
                sessionId,
                versionNo,
                title,
                status,
                preview(sourceResultIdsJson)
            );
            WritingDraft draft = writingDraftService.createDraft(
                sessionId,
                versionNo,
                title,
                content,
                parseJsonOrNull(sourceResultIdsJson),
                status,
                callerAgentId,
                true
            );
            log.info(
                "[SwarmTools] writing_draft completed: draftId={}, sessionId={}, versionNo={}, status={}",
                draft.getId(),
                draft.getSessionId(),
                draft.getVersionNo(),
                draft.getStatus()
            );
            return objectMapper.writeValueAsString(draft);
        } catch (Exception e) {
            log.error("[SwarmTools] writing_draft failed", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    private String resolveGraphJson(String graphJson, String description)
        throws Exception {
        if (isValidWorkflowGraph(graphJson)) {
            return graphJson;
        }
        return buildDefaultWorkflowGraph(description);
    }

    private boolean isValidWorkflowGraph(String graphJson) {
        if (graphJson == null || graphJson.isBlank()) {
            return false;
        }

        try {
            com.fasterxml.jackson.databind.JsonNode root =
                objectMapper.readTree(graphJson);
            com.fasterxml.jackson.databind.JsonNode nodes = root.get("nodes");
            com.fasterxml.jackson.databind.JsonNode edges = root.get("edges");
            if (nodes == null || !nodes.isArray() || nodes.isEmpty()) {
                return false;
            }
            if (edges == null || !edges.isArray()) {
                return false;
            }
            for (com.fasterxml.jackson.databind.JsonNode node : nodes) {
                if (
                    !node.hasNonNull("nodeId") || !node.hasNonNull("nodeType")
                ) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.warn(
                "[SwarmTools] Provided graphJson is invalid, fallback to auto-generated workflow",
                e
            );
            return false;
        }
    }

    private String buildDefaultWorkflowGraph(String description)
        throws Exception {
        Long llmConfigId = workspaceService
            .getWorkspace(callerWorkspaceId)
            .getLlmConfigId();
        String effectiveDescription = (description == null ||
            description.isBlank())
            ? "根据用户输入完成任务，并输出最终结果。"
            : description.trim();

        java.util.Map<String, Object> graph = new java.util.LinkedHashMap<>();
        graph.put(
            "dagId",
            "swarm-" + java.util.UUID.randomUUID().toString().substring(0, 8)
        );
        graph.put("version", "1.0");
        graph.put("description", effectiveDescription);
        graph.put("memory", false);
        graph.put("startNodeId", "start-1");

        java.util.List<java.util.Map<String, Object>> nodes =
            new java.util.ArrayList<>();

        nodes.add(
            java.util.Map.of(
                "nodeId",
                "start-1",
                "nodeName",
                "开始",
                "nodeType",
                "START",
                "position",
                java.util.Map.of("x", 120, "y", 160),
                "inputSchema",
                java.util.List.of(
                    java.util.Map.of(
                        "key",
                        "input",
                        "label",
                        "用户输入",
                        "type",
                        "string",
                        "required",
                        true
                    )
                ),
                "outputSchema",
                java.util.List.of(
                    java.util.Map.of(
                        "key",
                        "input",
                        "label",
                        "用户输入",
                        "type",
                        "string"
                    )
                ),
                "userConfig",
                java.util.Map.of()
            )
        );

        java.util.Map<String, Object> llmUserConfig =
            new java.util.LinkedHashMap<>();
        if (llmConfigId != null) {
            llmUserConfig.put("llmConfigId", llmConfigId);
        }
        llmUserConfig.put("systemPrompt", effectiveDescription);
        llmUserConfig.put("userPromptTemplate", "{{input}}");

        nodes.add(
            java.util.Map.of(
                "nodeId",
                "llm-1",
                "nodeName",
                "执行任务",
                "nodeType",
                "LLM",
                "position",
                java.util.Map.of("x", 380, "y", 160),
                "inputSchema",
                java.util.List.of(
                    java.util.Map.of(
                        "key",
                        "input",
                        "label",
                        "输入",
                        "type",
                        "string",
                        "sourceRef",
                        "start-1.output.input"
                    )
                ),
                "outputSchema",
                java.util.List.of(
                    java.util.Map.of(
                        "key",
                        "response",
                        "label",
                        "模型输出",
                        "type",
                        "string"
                    )
                ),
                "userConfig",
                llmUserConfig
            )
        );

        nodes.add(
            java.util.Map.of(
                "nodeId",
                "end-1",
                "nodeName",
                "结束",
                "nodeType",
                "END",
                "position",
                java.util.Map.of("x", 640, "y", 160),
                "inputSchema",
                java.util.List.of(
                    java.util.Map.of(
                        "key",
                        "finalResult",
                        "label",
                        "最终结果",
                        "type",
                        "string",
                        "required",
                        true,
                        "sourceRef",
                        "llm-1.output.response"
                    )
                ),
                "outputSchema",
                java.util.List.of(),
                "userConfig",
                java.util.Map.of()
            )
        );

        graph.put("nodes", nodes);
        graph.put(
            "edges",
            java.util.List.of(
                java.util.Map.of(
                    "edgeId",
                    "edge-start-llm",
                    "source",
                    "start-1",
                    "target",
                    "llm-1",
                    "edgeType",
                    "DEPENDENCY"
                ),
                java.util.Map.of(
                    "edgeId",
                    "edge-llm-end",
                    "source",
                    "llm-1",
                    "target",
                    "end-1",
                    "edgeType",
                    "DEPENDENCY"
                )
            )
        );

        return objectMapper.writeValueAsString(graph);
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

    private com.fasterxml.jackson.databind.JsonNode parseJsonOrNull(String raw)
        throws Exception {
        if (raw == null || raw.isBlank() || "null".equalsIgnoreCase(raw)) {
            return null;
        }
        return objectMapper.readTree(raw);
    }
}
