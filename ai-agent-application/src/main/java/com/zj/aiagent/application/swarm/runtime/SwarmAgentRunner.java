package com.zj.aiagent.application.swarm.runtime;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.application.swarm.SwarmContextAnalyzer;
import com.zj.aiagent.application.swarm.SwarmMessageService;
import com.zj.aiagent.application.swarm.dto.SendMessageRequest;
import com.zj.aiagent.application.swarm.prompt.SwarmPromptService;
import com.zj.aiagent.application.swarm.tool.SwarmToolFilter;
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
import com.zj.aiagent.domain.swarm.valobj.SwarmRole;
import com.zj.aiagent.domain.swarm.valobj.SwarmTaskContext;
import com.zj.aiagent.domain.swarm.valobj.TaskNotificationEvent;
import com.zj.aiagent.domain.writing.entity.WritingResult;
import com.zj.aiagent.domain.writing.entity.WritingSession;
import com.zj.aiagent.domain.writing.entity.WritingTask;
import com.zj.aiagent.infrastructure.mcp.adapter.McpToolCallbackAdapter;
import com.zj.aiagent.infrastructure.swarm.llm.SwarmLlmCaller;
import com.zj.aiagent.infrastructure.swarm.llm.SwarmLlmResponse;
import com.zj.aiagent.infrastructure.swarm.sse.SwarmAgentEventBus;
import com.zj.aiagent.infrastructure.swarm.sse.SwarmUIEventBus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

/**
 * 单个 Agent 的运行循环（虚拟线程）
 */
@Slf4j
public class SwarmAgentRunner implements Runnable {

    /**
     * Coordinator Phase — 参照 Claude-Code 的阶段性任务分解
     */
    public enum Phase {
        RESEARCH("Research", "调研阶段：收集信息、分析问题"),
        SYNTHESIS("Synthesis", "整合阶段：汇总发现、撰写规格"),
        IMPLEMENTATION("Implementation", "实施阶段：执行变更、实现功能"),
        VERIFICATION("Verification", "验证阶段：测试验证、确认正确性");

        private final String label;
        private final String description;

        Phase(String label, String description) {
            this.label = label;
            this.description = description;
        }

        public String getLabel() { return label; }
        public String getDescription() { return description; }

        /**
         * 获取下一个 Phase。
         * RESEARCH → SYNTHESIS → IMPLEMENTATION → VERIFICATION → VERIFICATION（终态）
         */
        public Phase next() {
            return switch (this) {
                case RESEARCH -> SYNTHESIS;
                case SYNTHESIS -> IMPLEMENTATION;
                case IMPLEMENTATION -> VERIFICATION;
                case VERIFICATION -> VERIFICATION;
            };
        }

        /**
         * 判断是否可以流转到目标 Phase（只能向前流转）
         */
        public boolean canTransitionTo(Phase target) {
            return target.ordinal() > this.ordinal();
        }

        /**
         * 转换为 SwarmTaskContext.Phase（domain 层定义，避免循环依赖）
         */
        public SwarmTaskContext.Phase toContextPhase() {
            return SwarmTaskContext.Phase.valueOf(this.name());
        }

        /**
         * 从 SwarmTaskContext.Phase 转换
         */
        public static Phase fromContextPhase(SwarmTaskContext.Phase ctxPhase) {
            return Phase.valueOf(ctxPhase.name());
        }
    }

    /**
     * 根据 parentId 解析 SwarmRole。
     * - parentId == null → 直接服务用户的协调者 = COORDINATOR
     * - parentId != null → 被派发的执行者 = WORKER
     */
    private static SwarmRole resolveSwarmRole(Long parentId) {
        if (parentId != null) {
            return SwarmRole.WORKER;
        }
        return SwarmRole.COORDINATOR;
    }

    private final SwarmAgent agent;
    private final SwarmDomainService domainService;
    private final SwarmAgentRepository agentRepository;
    private final SwarmGroupRepository groupRepository;
    private final SwarmMessageRepository messageRepository;
    private final SwarmMessageService messageService;
    private final SwarmLlmCaller llmCaller;
    private final SwarmTools swarmTools;
    /** SwarmTools 内置工具（@Tool 方法，一次构建） */
    private final ToolCallback[] swarmToolCallbacks;
    /** MCP 工具适配器（动态查询连接池，实时反映已连接服务器的工具） */
    private final McpToolCallbackAdapter mcpToolCallbackAdapter;
    private final Map<String, ToolCallback> toolCallbackMap;
    private final ObjectMapper objectMapper;
    private final SwarmAgentEventBus agentEventBus;
    private final SwarmUIEventBus uiEventBus;
    private final SwarmToolFilter toolFilter;
    private final SwarmPromptService promptService;
    private final SwarmContextAnalyzer contextAnalyzer;
    private final WritingSessionService writingSessionService;
    private final WritingTaskService writingTaskService;
    private final WritingResultService writingResultService;
    private final int maxRoundsPerTurn;
    private final LlmProviderConfig llmConfig;
    /** 根据 parentId 计算的 SwarmRole */
    private final SwarmRole swarmRole;
    /** 当前 Phase（Coordinator/Worker 模式使用，初始为 RESEARCH 动态流转） */
    private volatile Phase currentPhase = Phase.RESEARCH;
    /** 任务上下文追踪（exploredFiles / exploredModules / findings） */
    private volatile SwarmTaskContext taskContext;
    /** 本次执行统计 */
    private volatile long turnStartTime;
    private volatile int turnToolCallCount = 0;
    private volatile int turnTokenCount = 0;

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
        ObjectMapper objectMapper,
        SwarmAgentEventBus agentEventBus,
        SwarmUIEventBus uiEventBus,
        WritingSessionService writingSessionService,
        WritingTaskService writingTaskService,
        WritingResultService writingResultService,
        int maxRoundsPerTurn,
        LlmProviderConfig llmConfig,
        SwarmTools swarmTools,
        McpToolCallbackAdapter mcpToolCallbackAdapter,
        SwarmToolFilter toolFilter,
        SwarmPromptService promptService,
        SwarmContextAnalyzer contextAnalyzer
    ) {
        this.agent = agent;
        this.domainService = domainService;
        this.agentRepository = agentRepository;
        this.groupRepository = groupRepository;
        this.messageRepository = messageRepository;
        this.messageService = messageService;
        this.llmCaller = llmCaller;
        this.swarmTools = swarmTools;
        this.mcpToolCallbackAdapter = mcpToolCallbackAdapter;
        this.objectMapper = objectMapper;
        this.agentEventBus = agentEventBus;
        this.uiEventBus = uiEventBus;
        this.toolFilter = toolFilter;
        this.promptService = promptService;
        this.contextAnalyzer = contextAnalyzer;
        this.writingSessionService = writingSessionService;
        this.writingTaskService = writingTaskService;
        this.writingResultService = writingResultService;
        this.maxRoundsPerTurn = maxRoundsPerTurn;
        this.llmConfig = llmConfig;
        this.swarmRole = resolveSwarmRole(agent.getParentId());
        this.currentWakeSignal = new CompletableFuture<>();

        // 初始化任务上下文（从持久化中恢复或创建新的）
        if (contextAnalyzer != null) {
            SwarmTaskContext saved = contextAnalyzer.loadContext(agent.getId());
            this.taskContext = saved != null ? saved : SwarmTaskContext.create(agent.getId());
        } else {
            this.taskContext = SwarmTaskContext.create(agent.getId());
        }

        // 构建 SwarmTools 内置工具索引（静态部分）
        this.swarmToolCallbacks = swarmTools != null
                ? ToolCallbacks.from(swarmTools)
                : new ToolCallback[0];

        // 构建 name -> ToolCallback 索引（首次用 SwarmTools，buildAllToolCallbacks 动态补充 MCP）
        this.toolCallbackMap = new HashMap<>();
        for (ToolCallback cb : swarmToolCallbacks) {
            this.toolCallbackMap.put(cb.getToolDefinition().name(), cb);
        }
        log.info(
            "[Swarm] ToolCallback pool assembled: agent={}, swarmTools={}, toolNames={}",
            agent.getId(),
            this.swarmToolCallbacks.length,
            this.toolCallbackMap.keySet()
        );
    }

    /**
     * 动态构建完整的工具回调列表
     * <p>
     * SwarmTools 内置工具（静态）+ MCP 已连接服务器的工具（实时）。
     * 每次 LLM 调用前重新合并，确保：
     * 1. Agent 启动时 MCP 服务器未连接 → 只有 SwarmTools
     * 2. 运行期间服务器连接 → MCP 工具立即生效
     */
    private ToolCallback[] buildAllToolCallbacks() {
        List<ToolCallback> mcpCallbacks = mcpToolCallbackAdapter != null
                ? mcpToolCallbackAdapter.getAllMcpToolCallbacks()
                : List.of();
        return ArrayUtils.addAll(swarmToolCallbacks, mcpCallbacks.toArray(new ToolCallback[0]));
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

    // ── Phase 动态追踪 ───────────────────────────────────────────

    /**
     * 转换到新的 Phase。
     * 只能向前流转：RESEARCH → SYNTHESIS → IMPLEMENTATION → VERIFICATION
     *
     * @param newPhase 目标 Phase
     * @return 是否成功转换（如果已在目标 Phase 或目标 Phase 更早，则返回 false）
     */
    private boolean transitionPhase(Phase newPhase) {
        Phase oldPhase = this.currentPhase;
        if (oldPhase == newPhase) {
            log.debug(
                "[Swarm] Phase unchanged: agent={}, phase={}",
                agent.getId(),
                oldPhase
            );
            return false;
        }
        if (!oldPhase.canTransitionTo(newPhase)) {
            log.warn(
                "[Swarm] Invalid phase transition (cannot go backwards): agent={}, from={}, to={}",
                agent.getId(),
                oldPhase,
                newPhase
            );
            return false;
        }
        this.currentPhase = newPhase;
        // 转换 Runner.Phase → TaskContext.Phase
        SwarmTaskContext.Phase ctxPhase = SwarmTaskContext.Phase.valueOf(newPhase.name());
        this.taskContext = this.taskContext.withPhase(ctxPhase);

        log.info(
            "[Swarm] Phase transitioned: agent={}, role={}, from={} → to={}, description={}",
            agent.getId(),
            swarmRole,
            oldPhase,
            newPhase,
            newPhase.getDescription()
        );

        // 广播 Phase 转换事件
        emitUIEvent(
            "ui.agent.phase_changed",
            String.format(
                "{\"agentId\":%d,\"phase\":\"%s\",\"label\":\"%s\",\"description\":\"%s\"}",
                agent.getId(),
                newPhase.name(),
                newPhase.getLabel(),
                newPhase.getDescription()
            )
        );
        return true;
    }

    /**
     * 根据消息内容推断 Phase 转换。
     * 分析收到的消息内容，决定是否需要转换 Phase。
     *
     * <p>转换规则：
     * <ul>
     *   <li>RESEARCH → SYNTHESIS: 收到 Worker 调研结果 / 用户要求综合分析</li>
     *   <li>SYNTHESIS → IMPLEMENTATION: Coordinator 决定开始实现 / 用户确认计划</li>
     *   <li>IMPLEMENTATION → VERIFICATION: 所有实现任务完成</li>
     * </ul>
     */
    private void inferPhaseTransition(Map<Long, List<SwarmMessage>> messagesByGroup) {
        if (swarmRole != SwarmRole.COORDINATOR) {
            return;
        }

        for (List<SwarmMessage> messages : messagesByGroup.values()) {
            for (SwarmMessage msg : messages) {
                String content = msg.getContent();
                if (content == null || content.isBlank()) {
                    continue;
                }

                // RESEARCH → SYNTHESIS: 收到包含"调研完成"、"research"、"调研结果"等关键词的消息
                if (currentPhase == Phase.RESEARCH) {
                    String lower = content.toLowerCase();
                    if (lower.contains("调研完成") || lower.contains("research")
                        || lower.contains("调研结果") || lower.contains("findings")
                        || lower.contains("synthesis") || lower.contains("综合")
                        || lower.contains("分析完成")) {
                        transitionPhase(Phase.SYNTHESIS);
                        return;
                    }
                }

                // SYNTHESIS → IMPLEMENTATION: 收到包含"开始实现"、"implement"、"实施"等关键词
                if (currentPhase == Phase.SYNTHESIS) {
                    String lower = content.toLowerCase();
                    if (lower.contains("开始实现") || lower.contains("implement")
                        || lower.contains("开始执行") || lower.contains("开始干活")
                        || lower.contains("implementation") || lower.contains("实施")
                        || lower.contains("计划确认") || lower.contains("plan confirmed")) {
                        transitionPhase(Phase.IMPLEMENTATION);
                        return;
                    }
                }

                // IMPLEMENTATION → VERIFICATION: 收到包含"实现完成"、"验证"、"verify"、"测试"等关键词
                if (currentPhase == Phase.IMPLEMENTATION) {
                    String lower = content.toLowerCase();
                    if (lower.contains("实现完成") || lower.contains("verify")
                        || lower.contains("验证") || lower.contains("测试")
                        || lower.contains("implementation done") || lower.contains("完成")
                        || lower.contains("done")) {
                        // 检查是否所有 Worker 任务都完成了
                        if (checkAllWorkerTasksCompleted()) {
                            transitionPhase(Phase.VERIFICATION);
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * 检查所有 Worker 任务是否都已完成
     */
    private boolean checkAllWorkerTasksCompleted() {
        try {
            var sessions = writingSessionService.listSessions(agent.getWorkspaceId());
            for (var session : sessions) {
                if (!agent.getId().equals(session.getRootAgentId())) {
                    continue;
                }
                var tasks = writingTaskService.listBySession(session.getId());
                if (tasks.isEmpty()) {
                    continue;
                }
                // 只要有任何一个 PENDING 或 RUNNING 任务，就认为还没完成
                boolean hasActive = tasks.stream()
                    .anyMatch(t -> "PENDING".equals(t.getStatus()) || "RUNNING".equals(t.getStatus()));
                if (hasActive) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.warn(
                "[Swarm] Failed to check worker tasks completion: agent={}",
                agent.getId(),
                e
            );
            return false;
        }
    }

    // ── 上下文追踪 ───────────────────────────────────────────────

    /**
     * 从工具调用参数中提取探索过的文件路径。
     * 识别模式：read_file, write_file, edit_file, glob 等文件操作工具。
     */
    private Set<String> extractExploredFiles(String toolName, String toolArgs) {
        Set<String> files = new HashSet<>();
        if (toolName == null || toolArgs == null || toolArgs.isBlank()) {
            return files;
        }

        try {
            JsonNode args = SwarmToolArgumentUtils.tryParseArgumentsObject(toolArgs, objectMapper);
            if (args == null) {
                return files;
            }

            // 匹配已知文件操作工具的参数字段
            String[] fileFields = {"path", "file", "filePath", "file_path", "filepath",
                                   "files", "paths", "target", "source", "destination"};
            for (String field : fileFields) {
                if (args.has(field)) {
                    JsonNode node = args.get(field);
                    if (node.isTextual()) {
                        addFileIfRelevant(files, node.asText());
                    } else if (node.isArray()) {
                        for (JsonNode item : node) {
                            if (item.isTextual()) {
                                addFileIfRelevant(files, item.asText());
                            }
                        }
                    }
                }
            }

            // 特殊工具名识别
            if (toolName.contains("read") || toolName.contains("write")
                || toolName.contains("edit") || toolName.contains("file")
                || toolName.contains("glob") || toolName.contains("grep")) {
                // 尝试从整个 args 中提取文件路径（简单的启发式匹配）
                extractFilePathsFromText(files, toolArgs);
            }
        } catch (Exception e) {
            log.debug(
                "[Swarm] Failed to extract files from tool call: tool={}",
                toolName,
                e
            );
        }
        return files;
    }

    /**
     * 添加文件路径（只保留合理的文件路径）
     */
    private void addFileIfRelevant(Set<String> files, String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        path = path.trim();
        // 过滤掉明显不是文件路径的值
        if (path.length() > 512 || path.contains("://") || path.startsWith("--")
            || path.startsWith("\"") || path.startsWith("'")
            || Character.isUpperCase(path.charAt(0)) && !path.contains("/")
            && !path.contains("\\") && path.contains(" ")) {
            // 可能是命令参数，不是文件路径
            return;
        }
        files.add(path);
    }

    /**
     * 从文本中提取文件路径（启发式）
     */
    private void extractFilePathsFromText(Set<String> files, String text) {
        // 匹配常见的代码文件扩展名
        Pattern filePattern = Pattern.compile(
            "[a-zA-Z0-9_\\-./\\\\]+" +
            "\\.(java|kt|scala|js|ts|tsx|jsx|py|go|rs|cpp|c|h|hpp|sql|xml|json|yaml|yml|md|gradle|pom|properties)"
        );
        var matcher = filePattern.matcher(text);
        while (matcher.find()) {
            addFileIfRelevant(files, matcher.group());
        }
    }

    /**
     * 从工具调用结果中提取探索过的模块
     */
    private Set<String> extractExploredModules(String toolName, String toolResult) {
        Set<String> modules = new HashSet<>();
        if (toolName == null || toolResult == null || toolResult.isBlank()) {
            return modules;
        }

        // 简单的启发式：从结果中提取模块名（通常是包路径或目录名）
        // 例如: com.zj.aiagent.domain 或 src/main/java/com/zj/aiagent
        Pattern modulePattern = Pattern.compile(
            "(?:com|org|io|dev|ai)\\.[a-zA-Z0-9_]+" +
            "(?:\\.[a-zA-Z0-9_]+)*"
        );
        var matcher = modulePattern.matcher(toolResult);
        while (matcher.find()) {
            String match = matcher.group();
            // 只取前3层作为模块名
            String[] parts = match.split("\\.");
            if (parts.length >= 2) {
                modules.add(parts[parts.length - 1]);
            }
        }
        return modules;
    }

    /**
     * 从 LLM 响应中提取发现的摘要
     */
    private String extractFindingsFromResponse(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        // 尝试从响应中提取 finding 标记
        // 格式1: <finding>...</finding>
        // 格式2: "finding": "..."
        Pattern findingPattern = Pattern.compile(
            "<finding>(.*?)</finding>",
            Pattern.DOTALL
        );
        var matcher = findingPattern.matcher(content);
        if (matcher.find()) {
            String finding = matcher.group(1).trim();
            return finding.length() <= 500 ? finding : finding.substring(0, 500);
        }
        return null;
    }

    /**
     * 收集本次 ReAct 循环产生的上下文（exploredFiles, exploredModules, findings）
     */
    private void collectContext(
        String lastToolName,
        String lastToolArgs,
        String lastToolResult,
        String lastLlmContent
    ) {
        // 提取文件路径
        Set<String> files = extractExploredFiles(lastToolName, lastToolArgs);
        if (!files.isEmpty()) {
            taskContext = taskContext.addExploredFiles(files);
            log.info(
                "[Swarm] Context updated (files): agent={}, added={}, total={}",
                agent.getId(),
                files.size(),
                taskContext.getExploredFiles().size()
            );
        }

        // 提取模块
        Set<String> modules = extractExploredModules(lastToolName, lastToolResult);
        for (String module : modules) {
            taskContext = taskContext.addExploredModule(module);
        }

        // 提取发现
        String finding = extractFindingsFromResponse(lastLlmContent);
        if (finding != null) {
            taskContext = taskContext.addFinding(finding);
            log.info(
                "[Swarm] Context updated (finding): agent={}, total={}",
                agent.getId(),
                taskContext.getFindings().size()
            );
        }
    }

    /**
     * 持久化上下文到存储
     */
    private void persistContext() {
        if (contextAnalyzer != null && taskContext != null) {
            contextAnalyzer.saveContext(agent.getId(), taskContext);
            log.debug(
                "[Swarm] Context persisted: agent={}, files={}, modules={}, findings={}, phase={}",
                agent.getId(),
                taskContext.getExploredFiles().size(),
                taskContext.getExploredModules().size(),
                taskContext.getFindings().size(),
                taskContext.getCurrentPhase()
            );
        }
    }

    /**
     * 广播当前上下文给所有订阅者
     */
    private void broadcastContext() {
        if (taskContext == null) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(
                Map.of(
                    "agentId", agent.getId(),
                    "phase", currentPhase.name(),
                    "phaseLabel", currentPhase.getLabel(),
                    "exploredFiles", new ArrayList<>(taskContext.getExploredFiles()),
                    "exploredModules", new ArrayList<>(taskContext.getExploredModules()),
                    "findings", new ArrayList<>(taskContext.getFindings()),
                    "timestamp", Instant.now().toEpochMilli()
                )
            );
            emitUIEvent("ui.agent.context_updated", payload);
        } catch (Exception e) {
            log.warn(
                "[Swarm] Failed to broadcast context: agent={}",
                agent.getId(),
                e
            );
        }
    }

    /**
     * 执行一轮推理
     */
    private void processTurn() {
        // 重置本次执行统计
        turnStartTime = System.currentTimeMillis();
        turnToolCallCount = 0;
        turnTokenCount = 0;

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

            // Phase 动态推断：根据消息内容决定是否需要转换 Phase
            inferPhaseTransition(unreadByGroup);

            // 简化：所有消息统一处理
            log.info(
                "[Swarm] Turn started: agent={}, workspace={}, role={}, swarmRole={}, phase={}, unreadGroups={}, unreadMessages={}",
                agent.getId(),
                agent.getWorkspaceId(),
                agent.getRole(),
                swarmRole,
                currentPhase,
                unreadByGroup.size(),
                countMessages(unreadByGroup)
            );

            // 处理所有消息
            if (!unreadByGroup.isEmpty()) {
                if (processAgentMessages(unreadByGroup)) {
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
            // 持久化本次 turn 收集的上下文
            persistContext();
            // 广播当前上下文
            broadcastContext();

            if (swarmRole == SwarmRole.COORDINATOR && finalStatus != SwarmAgentStatus.WAITING) {
                emitWaitingDone(null);
            }
            agentRepository.updateStatus(agent.getId(), finalStatus.getCode());
            emitEvent("agent.done", null, "");
            // Worker 任务完成时，向 Coordinator 发送 task-notification
            emitTaskNotificationOnCompletion(finalStatus);
        }
    }

    /**
     * 任务完成时发射 task-notification 事件
     * - Worker（有 parentId）：向父 Agent（Coordinator）发送通知
     * - Coordinator（无 parentId）：向前端 UI 发送通知
     */
    private void emitTaskNotificationOnCompletion(SwarmAgentStatus finalStatus) {
        if (agent.isWorker()) {
            // Worker 向父 Agent（Coordinator）发送 task-notification
            Long parentId = agent.getParentId();
            if (parentId != null) {
                long durationMs = System.currentTimeMillis() - turnStartTime;
                String status = switch (finalStatus) {
                    case IDLE, WAITING -> "completed";
                    case BUSY -> "running";
                    case STOPPED -> "killed";
                    default -> "failed";
                };
                String result = buildTurnResultSummary();
                String summary = buildTurnSummary(result);

                TaskNotificationEvent.Usage usage =
                    TaskNotificationEvent.Usage.builder()
                        .toolUses(turnToolCallCount)
                        .totalTokens(turnTokenCount)
                        .durationMs(durationMs)
                        .build();

                TaskNotificationEvent notification =
                    TaskNotificationEvent.builder()
                        .agentId(agent.getId())
                        .status(status)
                        .summary(summary)
                        .result(result)
                        .phase(currentPhase.getLabel())
                        .usage(usage)
                        .build();

                agentEventBus.emitTaskNotification(parentId, notification);
                log.info(
                    "[Swarm] Task notification sent: worker={}, parent={}, status={}, duration={}ms",
                    agent.getId(), parentId, status, durationMs
                );
            }
        } else if (agent.isCoordinator(agentRepository.hasChildren(agent.getId()))) {
            // Coordinator 完成一轮时，向 UI 发送通知（通过 uiEventBus）
            long durationMs = System.currentTimeMillis() - turnStartTime;
            try {
                String payload = objectMapper.writeValueAsString(
                    Map.of(
                        "type", "task-notification",
                        "agentId", agent.getId(),
                        "status", "coordinator_turn_complete",
                        "phase", currentPhase.getLabel(),
                        "toolCallCount", turnToolCallCount,
                        "durationMs", durationMs,
                        "timestamp", System.currentTimeMillis()
                    )
                );
                emitUIEvent("ui.agent.task-notification", payload);
            } catch (Exception e) {
                log.warn("[Swarm] Failed to emit coordinator task notification UI event", e);
            }
        }
    }

    private String buildTurnSummary(String result) {
        if (result == null || result.isBlank()) {
            return "任务执行完成";
        }
        String trimmed = result.replaceAll("\\s+", " ").trim();
        return trimmed.length() <= 200
            ? trimmed
            : trimmed.substring(0, 200) + "...";
    }

    private String buildTurnResultSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Agent ").append(agent.getId())
          .append(" (").append(agent.getRole()).append(") ")
          .append("执行完成。");
        if (turnToolCallCount > 0) {
            sb.append(" 工具调用次数: ").append(turnToolCallCount);
        }
        return sb.toString();
    }

    /**
     * 处理来自其他 agent 的消息：agent 间通信模式。
     * 同时处理来自用户和来自子 agent 的消息。
     *
     * 实现标准的 ReAct 推理循环：
     * 1. 调用 LLM → 2. 如果有 tool_calls → 3. 执行工具 → 4. 构建 ToolResponseMessage 追加到上下文 → 回到 1
     * 循环直到：LLM 不再发起 tool_calls / 进入 waiting 状态 / 达到 maxRoundsPerTurn 上限
     */
    private boolean processAgentMessages(
        Map<Long, List<SwarmMessage>> agentMsgsByGroup
    ) {
        // 独立构建 LLM 消息列表
        List<Message> messages = buildMessages(agentMsgsByGroup);

        Long primaryGroupId = agentMsgsByGroup.keySet().iterator().next();

        // Coordinator（parentId == null）：有派发的子 agent，流向 P2P 主群
        // Worker（parentId != null）：流向自身的 task 群
        boolean isCoordinator = agent.getParentId() == null;

        if (!isCoordinator) {
            markWritingAssignmentRunningIfNeeded();
        }
        boolean waitingForChildAgents = false;
        Long streamGroupId = isCoordinator
            ? findPrimaryGroup()
            : primaryGroupId;

        log.info(
            "[Swarm] Processing agent messages: agent={}, workspace={}, coordinator={}, groups={}, totalMessages={}, primaryGroup={}, streamGroup={}",
            agent.getId(),
            agent.getWorkspaceId(),
            isCoordinator,
            agentMsgsByGroup.keySet(),
            countMessages(agentMsgsByGroup),
            primaryGroupId,
            streamGroupId
        );

        // ── ReAct Loop ──────────────────────────────────────────────
        int loopRound = 0;
        SwarmLlmResponse response = null;
        // 追踪最后一个工具调用，用于上下文收集（跨循环累积）
        String lastToolName = null;
        String lastToolArgs = null;
        String lastToolResult = null;

        while (loopRound < maxRoundsPerTurn) {
            loopRound++;

            // 1. 调用 LLM
            emitStreamStart(streamGroupId);
            response = llmCaller.callStreamWithTools(
                messages,
                buildAllToolCallbacks(),
                chunk -> emitContentChunk(streamGroupId, chunk, true),
                llmConfig
            );
            emitStreamDone(streamGroupId);

            log.info(
                "[Swarm] LLM response ready (agent): agent={}, coordinator={}, round={}/{}, contentPreview={}, toolCallCount={}",
                agent.getId(),
                isCoordinator,
                loopRound,
                maxRoundsPerTurn,
                preview(response.getContent()),
                response.hasToolCalls() ? response.getToolCalls().size() : 0
            );

            // 2. 过滤出可执行的 tool calls
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

            // 3. 如果没有 tool calls，本轮推理结束
            if (executableToolCalls.isEmpty()) {
                break;
            }

            // 4. 将 LLM 的 AssistantMessage（含 tool_calls）追加到上下文
            AssistantMessage assistantMsg = new AssistantMessage(
                response.getContent() != null ? response.getContent() : "",
                Map.of(),
                executableToolCalls
            );
            messages.add(assistantMsg);

            // 5. 执行工具并收集结果
            List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();

            for (AssistantMessage.ToolCall toolCall : executableToolCalls) {
                String toolName = toolCall.name();
                String toolArgs = toolCall.arguments();
                lastToolName = toolName;
                lastToolArgs = toolArgs;
                String toolCallEventId =
                    agent.getId() + "-" + System.nanoTime() + "-" + toolName;

                Long sendTargetAgentId = "send".equals(toolName)
                    ? extractSendTargetAgentId(toolArgs)
                    : null;

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

                String result = executeToolCall(toolName, toolArgs);
                lastToolResult = result;
                turnToolCallCount++;
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

                // 收集 ToolResponse 用于下一轮 LLM 调用
                toolResponses.add(new ToolResponseMessage.ToolResponse(
                    toolCall.id(),
                    toolName,
                    result != null ? result : ""
                ));
            }

            // 6. 将 ToolResponseMessage 追加到上下文
            messages.add(new ToolResponseMessage(toolResponses));

            // 7. 如果已进入 waiting 状态（派发了子任务），不再继续循环
            if (waitingForChildAgents) {
                log.info(
                    "[Swarm] Coordinator entering waiting state after tool dispatch, breaking ReAct loop: agent={}, round={}",
                    agent.getId(),
                    loopRound
                );
                break;
            }

            log.info(
                "[Swarm] ReAct loop continues: agent={}, round={}/{}, feeding {} tool results back to LLM",
                agent.getId(),
                loopRound,
                maxRoundsPerTurn,
                toolResponses.size()
            );
        }

        if (loopRound >= maxRoundsPerTurn) {
            log.warn(
                "[Swarm] ReAct loop reached maxRoundsPerTurn limit: agent={}, maxRounds={}",
                agent.getId(),
                maxRoundsPerTurn
            );
        }
        // ── End ReAct Loop ──────────────────────────────────────────

        // 收集本次 ReAct 循环产生的上下文（从最后工具调用结果中提取文件/模块/发现）
        collectContext(lastToolName, lastToolArgs, lastToolResult, response != null ? response.getContent() : null);

        // Coordinator：有文字内容就自动投递到 P2P 主群
        if (isCoordinator && response != null) {
            String content = response.getContent();
            if (content != null && !content.isBlank()) {
                Long p2pGroupId = findPrimaryGroup();
                if (p2pGroupId != null) {
                    SwarmMessage msg = SwarmMessage.builder()
                        .workspaceId(agent.getWorkspaceId())
                        .groupId(p2pGroupId)
                        .senderId(agent.getId())
                        .contentType("text")
                        .content(content)
                        .sendTime(LocalDateTime.now())
                        .build();
                    messageRepository.save(msg);
                    log.info(
                        "[Swarm] Coordinator auto-delivered reply to P2P group: agent={}, p2pGroupId={}, messageId={}, preview={}",
                        agent.getId(),
                        p2pGroupId,
                        msg.getId(),
                        preview(content)
                    );
                    emitUIEvent(
                        "ui.message.created",
                        "{\"groupId\":" +
                            p2pGroupId +
                            ",\"messageId\":" +
                            msg.getId() +
                            ",\"senderId\":" +
                            agent.getId() +
                            "}"
                    );
                }
            }
        } else if (
            response != null &&
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
        // Coordinator（parentId == null）向子 Agent 派发任务后应进入等待
        return (
            swarmRole == SwarmRole.COORDINATOR &&
            targetAgentId != null
        );
    }

    private void emitWaitingDone(Long targetAgentId) {
        Long p2pGroupId = findPrimaryGroup();
        if (p2pGroupId == null) {
            return;
        }
        String targetAgentJson =
            targetAgentId == null ? "null" : String.valueOf(targetAgentId);
        emitUIEvent(
            "ui.agent.waiting.done",
            "{\"agentId\":" +
                agent.getId() +
                ",\"groupId\":" +
                p2pGroupId +
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
        if (swarmRole != SwarmRole.COORDINATOR) {
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
                agentRepository.updateStatus(agent.getId(), "DONE");
                latestResult = writingResultService.createResult(
                    assignment.session().getId(),
                    assignment.task().getId(),
                    assignment.task().getSwarmAgentId(),
                    normalizeResultType(assignment.task().getTaskType()),
                    summary,
                    content,
                    null
                );
                log.info(
                    "[Swarm] Implicit writing_result persisted: agent={}, sessionId={}, taskId={}, swarmAgentId={}, resultId={}",
                    agent.getId(),
                    assignment.session().getId(),
                    assignment.task().getId(),
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
        Long sessionId = agent.getSessionId();
        if (sessionId == null) {
            return Optional.empty();
        }
        WritingSession session = writingSessionService.getSession(sessionId);
        WritingTask task = writingTaskService
            .listBySwarmAgentId(agent.getId())
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
            return Optional.empty();
        }
        return Optional.of(new ResolvedWritingAssignment(session, task));
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

            if (!"RUNNING".equals(task.getStatus())) {
                writingTaskService.markRunning(task.getId());
            }
            if (!"RUNNING".equals(agent.getStatus().getCode())) {
                agentRepository.updateStatus(agent.getId(), "RUNNING");
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
     * 查找 Agent 的主群组 ID。
     * - Coordinator（parentId == null）：优先找 name="P2P" 的群；否则找第一个包含此 Agent 的群
     * - Worker（parentId != null）：找第一个包含此 Agent 的群（通常是 task 群）
     */
    private Long findPrimaryGroup() {
        try {
            var groups = groupRepository.findByWorkspaceId(
                agent.getWorkspaceId()
            );
            // 优先找 P2P 群
            for (var group : groups) {
                if ("P2P".equals(group.getName())) {
                    List<Long> memberIds = groupRepository.findMemberIds(
                        group.getId()
                    );
                    if (memberIds.contains(agent.getId())) {
                        return group.getId();
                    }
                }
            }
            // 降级：找第一个包含此 Agent 的群
            for (var group : groups) {
                List<Long> memberIds = groupRepository.findMemberIds(
                    group.getId()
                );
                if (memberIds.contains(agent.getId())) {
                    return group.getId();
                }
            }
        } catch (Exception e) {
            log.warn(
                "[Swarm] Failed to find primary group for agent {}",
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

        // System prompt: 通过 SwarmPromptService 按角色动态组合
        String systemPrompt;
        if (swarmRole == SwarmRole.WORKER) {
            systemPrompt = promptService.getWorkerPrompt(
                agent, currentPhase.name()
            );
        } else {
            // COORDINATOR
            systemPrompt = promptService.getCoordinatorPrompt(agent);
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
     * 通过 ToolCallback 统一池执行工具调用（参照 Claude-Code assembleToolPool 模式）
     * 内置 @Tool 方法 + MCP 适配工具均通过同一 Map 分派，无需手动 switch。
     */
    private String executeToolCall(String toolName, String toolArgs) {
        try {
            log.info(
                "[Swarm] Executing tool call: agent={}, workspace={}, role={}, swarmRole={}, tool={}, argsPreview={}",
                agent.getId(),
                agent.getWorkspaceId(),
                agent.getRole(),
                swarmRole,
                toolName,
                preview(toolArgs)
            );
            if (!toolFilter.isAllowed(swarmRole, toolName)) {
                log.warn(
                    "[Swarm] Agent {} (role={}) attempted disallowed tool: {}",
                    agent.getId(),
                    swarmRole,
                    toolName
                );
                return errorJson(
                    "你是 " + swarmRole.getDesc() + " Agent，不允许使用 " +
                        toolName + " 工具。"
                );
            }

            // 从动态完整列表查找（MCP 工具在运行期间可能随时加入连接池）
            Map<String, ToolCallback> allCallbacks = new HashMap<>();
            for (ToolCallback cb : buildAllToolCallbacks()) {
                allCallbacks.put(cb.getToolDefinition().name(), cb);
            }
            ToolCallback callback = allCallbacks.get(toolName);
            if (callback == null) {
                log.warn(
                    "[Swarm] Unknown tool: agent={}, tool={}, availableTools={}",
                    agent.getId(),
                    toolName,
                    allCallbacks.keySet()
                );
                return errorJson("Unknown tool: " + toolName);
            }

            String result = callback.call(toolArgs);

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
