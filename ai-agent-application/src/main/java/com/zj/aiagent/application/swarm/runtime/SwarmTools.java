package com.zj.aiagent.application.swarm.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.application.agent.cmd.AgentCommand;
import com.zj.aiagent.application.agent.dto.AgentDetailResult;
import com.zj.aiagent.application.agent.service.AgentApplicationService;
import com.zj.aiagent.application.swarm.SwarmMessageService;
import com.zj.aiagent.application.swarm.SwarmWorkspaceService;
import com.zj.aiagent.application.swarm.dto.SendMessageRequest;
import com.zj.aiagent.application.swarm.dto.SwarmAgentDTO;
import com.zj.aiagent.application.swarm.dto.SwarmGroupDTO;
import com.zj.aiagent.application.swarm.dto.SwarmMessageDTO;
import com.zj.aiagent.application.swarm.dto.WorkspaceDefaultsDTO;
import com.zj.aiagent.application.workflow.SchedulerService;
import com.zj.aiagent.application.writing.WritingResultService;
import com.zj.aiagent.application.writing.WritingSessionService;
import com.zj.aiagent.application.writing.WritingTaskService;
import com.zj.aiagent.domain.swarm.entity.SwarmAgent;
import com.zj.aiagent.domain.swarm.repository.SwarmAgentRepository;
import com.zj.aiagent.domain.writing.entity.WritingResult;
import com.zj.aiagent.domain.writing.entity.WritingTask;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 蜂群内置工具（@Tool 注解方式）。
 *
 * 重构目标：从 12 个工具简化为 6 个核心工具。
 *
 * 协作模式：Coordinator + Workers（类 Claude-Code）
 * <pre>
 *   用户 → Coordinator（规划）→ Workers（执行）→ Coordinator（整合）→ 用户
 * </pre>
 *
 * 工具角色分配：
 * - Coordinator：create_worker / delegate_task / executeWorkflow / send / self / listAgents
 * - Worker：     submit_result / executeWorkflow / send / self / listAgents
 */
@Slf4j
public class SwarmTools {

    private final SwarmWorkspaceService workspaceService;
    private final SwarmMessageService messageService;
    private final SwarmAgentRepository swarmAgentRepository;
    private final AgentApplicationService workflowAgentService;
    private final SchedulerService schedulerService;
    private final WritingSessionService writingSessionService;
    private final WritingTaskService writingTaskService;
    private final WritingResultService writingResultService;
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
        WritingTaskService writingTaskService,
        WritingResultService writingResultService,
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
        this.writingTaskService = writingTaskService;
        this.writingResultService = writingResultService;
        this.objectMapper = objectMapper;
        this.callerAgentId = callerAgentId;
        this.callerWorkspaceId = callerWorkspaceId;
        this.callerUserId = callerUserId;
    }

    // ===== 核心工具：create_worker =====

    /**
     * 创建一个 Worker Agent（通用多智能体协作）。
     * 创建时附带 sessionId 和 sortOrder，直接绑定到 SwarmAgent。
     */
    @Tool(
        description = "创建 Worker Agent。Coordinator 用来动态创建子工作节点。" +
            " 可选传入 taskUuid/instruction 以在创建时立即派发任务（等效于后续再调用 delegate_task）。"
    )
    public String create_worker(
        @ToolParam(description = "Worker 角色名称，如 researcher/writer/analyst") String role,
        @ToolParam(description = "Worker 的能力边界和职责描述") String description,
        @ToolParam(description = "可选的 taskUuid，由 Coordinator 预先生成，用于后续 delegate_task 关联") String taskUuid,
        @ToolParam(description = "可选的初始化任务指令，创建时附带则等效于同时调用 delegate_task") String instruction,
        @ToolParam(description = "可选的 JSON 字符串，描述期望输出结构。没有可传 null") String expectedOutputSchemaJson
    ) {
        try {
            log.info(
                "[SwarmTools] create_worker invoked: callerAgentId={}, workspaceId={}, role={}, taskUuid={}",
                callerAgentId, callerWorkspaceId, role,
                taskUuid != null ? taskUuid : "(none)"
            );

            // Step 1: 解析 sessionId（复用调用者的 session）
            Long resolvedSessionId = resolveSessionIdForCaller();

            // Step 2: 创建 SwarmAgent（swarm 层实体）
            String normalizedRole = (role == null || role.isBlank()) ? "worker" : role.trim();
            WorkspaceDefaultsDTO defaults = workspaceService.createAgent(
                callerWorkspaceId, normalizedRole, callerAgentId, description
            );
            Long swarmAgentId = defaults.getAssistantAgentId();

            // Step 3: 设置 sessionId 和 sortOrder 到 SwarmAgent
            SwarmAgent newAgent = workspaceService.getAgent(swarmAgentId);
            newAgent.setSessionId(resolvedSessionId);
            newAgent.setSortOrder(0);
            swarmAgentRepository.update(newAgent);

            // Step 4: 可选——创建 task 并立即派发
            if (taskUuid != null && instruction != null && !instruction.isBlank()) {
                WritingTask task = writingTaskService.createTask(
                    resolvedSessionId, swarmAgentId,
                    "GENERAL", taskUuid, instruction, null,
                    parseJsonOrNull(expectedOutputSchemaJson),
                    0, callerAgentId
                );
                String message = buildTaskMessage(taskUuid, instruction, expectedOutputSchemaJson);
                sendToSwarmAgent(swarmAgentId, message);

                log.info(
                    "[SwarmTools] create_worker with task: swarmAgentId={}, taskUuid={}",
                    swarmAgentId, taskUuid
                );
                return objectMapper.writeValueAsString(Map.of(
                    "swarmAgentId", swarmAgentId,
                    "sessionId", resolvedSessionId,
                    "taskUuid", task.getTaskUuid(),
                    "status", "DISPATCHED"
                ));
            }

            log.info(
                "[SwarmTools] create_worker completed: swarmAgentId={}, sessionId={}",
                swarmAgentId, resolvedSessionId
            );
            return objectMapper.writeValueAsString(Map.of(
                "swarmAgentId", swarmAgentId,
                "sessionId", resolvedSessionId,
                "role", normalizedRole
            ));
        } catch (Exception e) {
            log.error("[SwarmTools] create_worker failed", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    // ===== 核心工具：delegate_task =====

    /**
     * 派发任务给指定的 Worker Agent（通用多智能体协作）。
     * 内部创建 WritingTask 并通过 send 消息传递给目标 Agent。
     */
    @Tool(
        description = "向指定的 Worker Agent 派发任务。Coordinator 使用。" +
            " 内部会创建 writing_task 并通过 send 消息派发给目标 Agent。"
    )
    public String delegate_task(
        @ToolParam(description = "目标 Worker 的 swarmAgentId") long targetAgentId,
        @ToolParam(description = "任务业务唯一标识（taskUuid），由 Coordinator 生成，用于结果回溯") String taskUuid,
        @ToolParam(description = "任务指令，包含目标、约束、期望输出格式") String instruction,
        @ToolParam(description = "可选的 JSON 字符串，描述期望输出结构。没有可传 null") String expectedOutputSchemaJson
    ) {
        try {
            log.info(
                "[SwarmTools] delegate_task invoked: callerAgentId={}, targetAgentId={}, taskUuid={}",
                callerAgentId, targetAgentId, taskUuid
            );
            Long resolvedSessionId = resolveSessionIdForCaller();

            // 获取目标 agent 的 sessionId（确保在同一 session）
            SwarmAgent targetAgent = swarmAgentRepository.findById(targetAgentId).orElse(null);
            if (targetAgent == null) {
                return "{\"error\": \"Target agent not found: " + targetAgentId + "\"}";
            }

            WritingTask task = writingTaskService.createTask(
                resolvedSessionId, targetAgentId,
                "GENERAL", taskUuid, instruction, null,
                parseJsonOrNull(expectedOutputSchemaJson),
                0, callerAgentId
            );

            String message = buildTaskMessage(taskUuid, instruction, expectedOutputSchemaJson);
            sendToSwarmAgent(targetAgentId, message);

            log.info(
                "[SwarmTools] delegate_task completed: taskId={}, taskUuid={}, swarmAgentId={}",
                task.getId(), task.getTaskUuid(), targetAgentId
            );
            return objectMapper.writeValueAsString(Map.of(
                "taskId", task.getId(),
                "taskUuid", task.getTaskUuid(),
                "swarmAgentId", targetAgentId,
                "sessionId", resolvedSessionId,
                "status", "DISPATCHED"
            ));
        } catch (Exception e) {
            log.error("[SwarmTools] delegate_task failed", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    // ===== 核心工具：submit_result =====

    /**
     * Worker Agent 提交任务结果（通用多智能体协作）。
     * 内部调用 writing_result_by_task_uuid，并自动将 task 状态标记为完成。
     */
    @Tool(
        description = "Worker 提交任务结果。内部调用 writing_result_by_task_uuid 并将任务标记为完成。" +
            " 推荐 Worker 优先使用此工具记录结果。"
    )
    public String submit_result(
        @ToolParam(description = "任务业务唯一标识（taskUuid），由 Coordinator 派发时提供") String taskUuid,
        @ToolParam(description = "结果类型，如 TEXT/OUTLINE/REVIEW/ANALYSIS") String resultType,
        @ToolParam(description = "结果摘要（简短描述）") String summary,
        @ToolParam(description = "结果正文") String content,
        @ToolParam(description = "可选的 JSON 字符串，记录结构化结果。没有可传 null") String metadataJson
    ) {
        try {
            log.info(
                "[SwarmTools] submit_result invoked: callerAgentId={}, taskUuid={}, resultType={}, summary={}",
                callerAgentId, taskUuid, resultType, summary
            );
            WritingResult result = writingResultService.recordTaskResultByUuid(
                taskUuid, resultType, summary, content, parseJsonOrNull(metadataJson)
            );
            log.info(
                "[SwarmTools] submit_result completed: resultId={}, taskUuid={}, sessionId={}",
                result.getId(), taskUuid, result.getSessionId()
            );
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("[SwarmTools] submit_result failed", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    // ===== 保留工具：executeWorkflow =====

    /**
     * 执行某个工作流 Agent，并等待执行完成后返回结果。
     */
    @Tool(
        description = "执行某个工作流 Agent，并等待执行完成后返回结果。适合主 AI 同步等待工作流输出。"
    )
    public String executeWorkflow(
        @ToolParam(description = "要执行的Agent ID") long agentId,
        @ToolParam(description = "可选的输入内容") String input
    ) {
        try {
            log.info(
                "[SwarmTools] executeWorkflow invoked: callerAgentId={}, workspaceId={}, targetAgentId={}",
                callerAgentId, callerWorkspaceId, agentId
            );
            Map<String, Object> inputs = new java.util.LinkedHashMap<>();
            if (input != null && !input.isBlank()) {
                inputs.put("input", input);
            }

            Map<String, Object> result = schedulerService.executeAndWait(
                agentId,
                callerUserId,
                inputs,
                com.zj.aiagent.domain.workflow.valobj.ExecutionMode.STANDARD,
                10 * 60 * 1000L
            );
            log.info(
                "[SwarmTools] executeWorkflow completed: callerAgentId={}, targetAgentId={}",
                callerAgentId, agentId
            );
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("[SwarmTools] executeWorkflow failed", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    // ===== 保留工具：send =====

    /**
     * 向指定Agent发送消息。
     */
    @Tool(
        description = "向指定Agent发送消息。用于委派任务给子Agent或回复其他Agent的消息。" +
            " 建议使用结构化消息格式：[PHASE: X] [GOAL: X] [CONSTRAINTS: X] [EXPECTED_OUTPUT: X]\\n<详细描述>"
    )
    public String send(
        @ToolParam(description = "目标Agent的ID") long agentId,
        @ToolParam(description = "消息内容") String message
    ) {
        try {
            log.info(
                "[SwarmTools] send invoked: callerAgentId={}, targetAgentId={}",
                callerAgentId, agentId
            );
            List<SwarmGroupDTO> groups = messageService.listGroups(callerWorkspaceId, callerAgentId);
            Long groupId = null;
            for (SwarmGroupDTO g : groups) {
                if (g.getMemberIds() != null && g.getMemberIds().contains(agentId)) {
                    groupId = g.getId();
                    break;
                }
            }

            if (groupId == null) {
                return "{\"error\": \"No group found with agent " + agentId + "\"}";
            }

            SendMessageRequest req = new SendMessageRequest();
            req.setSenderId(callerAgentId);
            req.setContentType("text");
            req.setContent(message);
            SwarmMessageDTO sent = messageService.sendMessage(groupId, req);

            log.info(
                "[SwarmTools] send completed: callerAgentId={}, targetAgentId={}, groupId={}, messageId={}",
                callerAgentId, agentId, groupId, sent.getId()
            );
            return objectMapper.writeValueAsString(sent);
        } catch (Exception e) {
            log.error("[SwarmTools] send failed", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    // ===== 保留工具：self =====

    /**
     * 返回自身信息。
     */
    @Tool(description = "返回自身信息，包括 agent_id、workspace_id、角色、状态等")
    public String self() {
        try {
            SwarmAgent agent = swarmAgentRepository.findById(callerAgentId).orElse(null);
            if (agent == null) return "{\"error\": \"Agent not found\"}";
            return objectMapper.writeValueAsString(
                SwarmAgentDTO.builder()
                    .id(agent.getId())
                    .workspaceId(agent.getWorkspaceId())
                    .role(agent.getRole())
                    .description(agent.getDescription())
                    .parentId(agent.getParentId())
                    .status(agent.getStatus() != null ? agent.getStatus().getCode() : "IDLE")
                    .build()
            );
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    // ===== 保留工具：listAgents =====

    /**
     * 列出当前 workspace 中所有 Agent。
     */
    @Tool(description = "列出当前workspace中所有Agent，包括它们的ID、角色、状态和父子关系")
    public String listAgents() {
        try {
            List<SwarmAgentDTO> agents = workspaceService.listAgents(callerWorkspaceId);
            log.info(
                "[SwarmTools] listAgents completed: callerAgentId={}, workspaceId={}, count={}",
                callerAgentId, callerWorkspaceId, agents.size()
            );
            return objectMapper.writeValueAsString(agents);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    // ===== 内部辅助方法 =====

    /**
     * 解析 JSON 字符串，返回 JsonNode 或 null。
     */
    private com.fasterxml.jackson.databind.JsonNode parseJsonOrNull(String raw) throws Exception {
        if (raw == null || raw.isBlank() || "null".equalsIgnoreCase(raw)) {
            return null;
        }
        return objectMapper.readTree(raw);
    }

    /**
     * 获取调用者对应的 WritingSession ID。
     * 正常流程：workspace 创建时已初始化 session，此处必定能查到。
     * 兜底逻辑：兼容历史数据或异常场景，防御性自动补建。
     */
    private Long resolveSessionIdForCaller() {
        List<com.zj.aiagent.domain.writing.entity.WritingSession> sessions =
            writingSessionService.listSessions(callerWorkspaceId);

        // 优先找 caller 创建的 session
        for (com.zj.aiagent.domain.writing.entity.WritingSession session : sessions) {
            if (callerAgentId.equals(session.getRootAgentId())) {
                return session.getId();
            }
        }

        // 否则返回最近的 session（兼容子 Agent 场景）
        if (!sessions.isEmpty()) {
            return sessions.stream()
                .max(Comparator.comparing(
                    com.zj.aiagent.domain.writing.entity.WritingSession::getCreatedAt,
                    Comparator.nullsLast(LocalDateTime::compareTo)
                ))
                .map(com.zj.aiagent.domain.writing.entity.WritingSession::getId)
                .orElseThrow(() -> new IllegalStateException("No writing session available"));
        }

        // 兜底：不应到达此处（workspace 创建时已初始化 session），但防御性补建
        log.warn("[SwarmTools] No session found for workspace={}, this should not happen. "
            + "Auto-creating fallback session.", callerWorkspaceId);
        List<SwarmGroupDTO> groups = messageService.listGroups(callerWorkspaceId, callerAgentId);
        Long groupId = groups.isEmpty() ? null : groups.get(0).getId();
        com.zj.aiagent.domain.writing.entity.WritingSession fallbackSession =
            writingSessionService.createSession(
                callerWorkspaceId, callerAgentId, groupId,
                "Fallback Session",
                "Auto-created due to missing workspace session",
                null
            );
        return fallbackSession.getId();
    }

    /**
     * 构造发送给 Worker 的任务消息。
     */
    private String buildTaskMessage(String taskUuid, String instruction, String expectedOutputSchemaJson) {
        StringBuilder sb = new StringBuilder();
        sb.append("[PHASE: EXECUTION]\n");
        sb.append("[GOAL: 完成分配的任务]\n");
        sb.append("[TASK_UUID: ").append(taskUuid).append("]\n");
        if (expectedOutputSchemaJson != null && !expectedOutputSchemaJson.isBlank()) {
            sb.append("[EXPECTED_OUTPUT: ").append(expectedOutputSchemaJson).append("]\n");
        }
        sb.append("[CONSTRAINTS: 完成后调用 submit_result 记录结果，再用 send 汇报]\n\n");
        sb.append(instruction);
        return sb.toString();
    }

    /**
     * 向指定 swarmAgentId 发送消息。
     */
    private void sendToSwarmAgent(long targetAgentId, String message) {
        try {
            List<SwarmGroupDTO> groups = messageService.listGroups(callerWorkspaceId, callerAgentId);
            Long groupId = null;
            for (SwarmGroupDTO g : groups) {
                if (g.getMemberIds() != null && g.getMemberIds().contains(targetAgentId)) {
                    groupId = g.getId();
                    break;
                }
            }
            if (groupId == null) {
                log.warn("[SwarmTools] No group found for agent {}, cannot send message", targetAgentId);
                return;
            }
            SendMessageRequest req = new SendMessageRequest();
            req.setSenderId(callerAgentId);
            req.setContentType("text");
            req.setContent(message);
            messageService.sendMessage(groupId, req);
        } catch (Exception e) {
            log.warn("[SwarmTools] Failed to send message to agent {}: {}", targetAgentId, e.getMessage());
        }
    }
}
