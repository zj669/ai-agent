package com.zj.aiagent.application.workflow;

import com.zj.aiagent.application.chat.ChatApplicationService;
import com.zj.aiagent.domain.agent.entity.Agent;
import com.zj.aiagent.domain.agent.entity.AgentVersion;
import com.zj.aiagent.domain.agent.repository.AgentRepository;
import com.zj.aiagent.domain.chat.entity.Message;
import com.zj.aiagent.domain.chat.port.ConversationRepository;
import com.zj.aiagent.domain.memory.port.VectorStore;
import com.zj.aiagent.domain.workflow.config.HumanReviewConfig;
import com.zj.aiagent.domain.workflow.entity.Execution;
import com.zj.aiagent.domain.workflow.entity.HumanReviewRecord;
import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.entity.WorkflowGraph;
import com.zj.aiagent.domain.workflow.entity.WorkflowNodeExecutionLog;
import com.zj.aiagent.domain.workflow.event.NodeCompletedEvent;
import com.zj.aiagent.domain.workflow.port.*;
import com.zj.aiagent.domain.workflow.service.WorkflowGraphFactory;
import com.zj.aiagent.domain.workflow.valobj.*;
import com.zj.aiagent.infrastructure.redis.IRedisService;
import com.zj.aiagent.infrastructure.workflow.executor.NodeExecutorFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 工作流调度应用服务
 * 负责 DAG 遍历、节点调度和状态管理
 *
 * 位于 Application 层，协调 Domain 和 Infrastructure
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final NodeExecutorFactory executorFactory;
    private final ExecutionRepository executionRepository;
    private final CheckpointRepository checkpointRepository;
    private final AgentRepository agentRepository;
    private final WorkflowGraphFactory workflowGraphFactory;
    private final IRedisService redisService;
    private final WorkflowCancellationPort cancellationPort;
    private final HumanReviewQueuePort humanReviewQueuePort;
    private final StreamPublisherFactory streamPublisherFactory;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final HumanReviewRepository humanReviewRepository;

    // ========== 记忆系统依赖 ==========
    private final VectorStore vectorStore;
    private final ConversationRepository conversationRepository;

    // ========== 聊天服务依赖 ==========
    private final ChatApplicationService chatApplicationService;

    // ========== 工作流日志依赖 ==========
    private final WorkflowNodeExecutionLogRepository workflowNodeExecutionLogRepository;

    // ========== 表达式解析依赖 ==========
    private final ExpressionResolverPort expressionResolver;

    private static final int DEFAULT_STM_LIMIT = 10;

    /**
     * 启动工作流执行（根据 AgentId 获取图定义）
     */
    public void startExecution(
        String executionId,
        Long agentId,
        Long userId,
        String conversationId,
        Integer versionId,
        Map<String, Object> inputs,
        com.zj.aiagent.domain.workflow.valobj.ExecutionMode mode
    ) {
        log.info(
            "[Scheduler] Starting execution for agent: {}, version: {}, mode: {}",
            agentId,
            versionId,
            mode
        );

        // 1. 查询 Agent
        Agent agent = agentRepository
            .findById(agentId)
            .orElseThrow(() ->
                new IllegalArgumentException("Agent not found: " + agentId)
            );

        // 2. 获取 graphJson
        String graphJson;
        if (versionId != null) {
            // 使用指定版本
            AgentVersion version = agentRepository
                .findVersion(agentId, versionId)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Version not found: " + versionId
                    )
                );
            graphJson = version.getGraphSnapshot();
        } else if (agent.getPublishedVersionId() != null) {
            // 使用已发布版本
            AgentVersion publishedVersion = agentRepository
                .findVersion(agentId, agent.getPublishedVersionId().intValue())
                .orElseThrow(() ->
                    new IllegalStateException("Published version not found")
                );
            graphJson = publishedVersion.getGraphSnapshot();
        } else {
            // 使用当前草稿
            graphJson = agent.getGraphJson();
        }

        if (graphJson == null || graphJson.isBlank()) {
            throw new IllegalStateException(
                "Agent has no workflow graph defined"
            );
        }

        // 3. 解析为领域对象
        WorkflowGraph graph = workflowGraphFactory.fromJson(graphJson);

        // 4. 构建 Execution 并启动
        Execution execution = Execution.builder()
            .executionId(executionId)
            .agentId(agentId)
            .userId(userId)
            .conversationId(conversationId)
            .graph(graph)
            .status(
                com.zj.aiagent.domain.workflow.valobj.ExecutionStatus.PENDING
            )
            .build();

        startExecution(execution, inputs, mode);
    }

    /**
     * 启动工作流并同步等待结果
     * 适用于 Swarm/Tool 调用场景：主 AI 可以阻塞等待工作流执行完成，再基于结果继续回复用户。
     */
    public Map<String, Object> executeAndWait(
        Long agentId,
        Long userId,
        Map<String, Object> inputs,
        com.zj.aiagent.domain.workflow.valobj.ExecutionMode mode,
        long timeoutMillis
    ) {
        String executionId = java.util.UUID.randomUUID().toString();
        Map<String, Object> safeInputs =
            inputs == null ? new HashMap<>() : new HashMap<>(inputs);

        startExecution(
            executionId,
            agentId,
            userId,
            null,
            null,
            safeInputs,
            mode != null
                ? mode
                : com.zj.aiagent.domain.workflow.valobj.ExecutionMode.STANDARD
        );

        long deadline =
            System.currentTimeMillis() + Math.max(timeoutMillis, 1000L);

        while (System.currentTimeMillis() <= deadline) {
            Execution execution = executionRepository
                .findById(executionId)
                .orElseThrow(() ->
                    new IllegalStateException(
                        "工作流执行记录不存在: " + executionId
                    )
                );

            ExecutionStatus status = execution.getStatus();
            if (status == ExecutionStatus.SUCCEEDED) {
                String output = extractFinalResponseFromExecution(execution);
                if (!StringUtils.hasText(output) || "执行完成".equals(output)) {
                    output = extractFinalResponseFromLogs(executionId);
                }

                Map<String, Object> result = new HashMap<>();
                result.put("executionId", executionId);
                result.put("status", status.name());
                result.put("output", output);
                return result;
            }

            if (
                status == ExecutionStatus.FAILED ||
                status == ExecutionStatus.CANCELLED ||
                status == ExecutionStatus.PAUSED ||
                status == ExecutionStatus.PAUSED_FOR_REVIEW
            ) {
                Map<String, Object> result = new HashMap<>();
                result.put("executionId", executionId);
                result.put("status", status.name());
                result.put(
                    "output",
                    buildExecutionStatusMessage(execution, status)
                );
                return result;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                    "等待工作流执行结果时被中断: " + executionId,
                    e
                );
            }
        }

        throw new IllegalStateException(
            "工作流执行超时，超过 " + timeoutMillis + "ms: " + executionId
        );
    }

    /**
     * 启动工作流执行（内部方法，直接使用 Execution 对象）
     */
    private void startExecution(
        Execution execution,
        Map<String, Object> inputs,
        com.zj.aiagent.domain.workflow.valobj.ExecutionMode mode
    ) {
        log.info(
            "[Scheduler] Starting execution: {}, mode: {}",
            execution.getExecutionId(),
            mode
        );

        // ========== 新增: 保存用户消息和初始化 Assistant 消息 ==========
        if (StringUtils.hasText(execution.getConversationId())) {
            String userInput = extractUserQuery(inputs);
            if (StringUtils.hasText(userInput)) {
                try {
                    // 1. 保存用户消息
                    Message userMessage =
                        chatApplicationService.appendUserMessage(
                            execution.getConversationId(),
                            userInput,
                            Map.of("executionId", execution.getExecutionId())
                        );
                    log.info(
                        "[Scheduler] Saved user message: {}",
                        userMessage.getId()
                    );

                    // 2. 初始化 Assistant 消息 (PENDING 状态)
                    String assistantMessageId =
                        chatApplicationService.initAssistantMessage(
                            execution.getConversationId(),
                            execution.getExecutionId()
                        );

                    // 3. 保存 messageId 到 execution（用于后续更新）
                    execution.setAssistantMessageId(assistantMessageId);

                    log.info(
                        "[Scheduler] Initialized assistant message: {}",
                        assistantMessageId
                    );
                } catch (Exception e) {
                    log.error(
                        "[Scheduler] Failed to save messages: {}",
                        e.getMessage(),
                        e
                    );
                    // 不阻塞 workflow 执行
                }
            }
        }

        // ========== 记忆水合 (Memory Hydration) ==========
        hydrateMemory(execution, inputs);

        // 1. 启动执行，获取就绪节点
        List<Node> readyNodes = execution.start(inputs);

        // 2. 持久化初始状态
        executionRepository.save(execution);

        // 3. 调度就绪节点（根节点无父节点）
        scheduleNodes(execution.getExecutionId(), readyNodes, null);
    }

    /**
     * 记忆水合 - 在工作流启动前加载记忆
     */
    private void hydrateMemory(
        Execution execution,
        Map<String, Object> inputs
    ) {
        ExecutionContext context = execution.getContext();

        // 1. [LTM 加载] 检索长期记忆
        String userQuery = extractUserQuery(inputs);
        if (StringUtils.hasText(userQuery)) {
            try {
                List<String> memories = vectorStore.search(
                    userQuery,
                    execution.getAgentId()
                );
                context.setLongTermMemories(memories);
                log.info(
                    "[MemoryHydration] Loaded {} LTM entries for execution: {}",
                    memories.size(),
                    execution.getExecutionId()
                );
            } catch (Exception e) {
                log.warn(
                    "[MemoryHydration] Failed to load LTM (workflow will continue without memory): {}",
                    e.getMessage()
                );
                if (e.getMessage() != null && e.getMessage().contains("404")) {
                    log.warn(
                        "[MemoryHydration] Embedding API returned 404 - please check embedding model configuration (baseUrl/model name)"
                    );
                }
            }
        }

        // 2. [STM 加载] 获取历史对话
        if (StringUtils.hasText(execution.getConversationId())) {
            try {
                List<Message> history =
                    conversationRepository.findMessagesByConversationId(
                        execution.getConversationId(),
                        PageRequest.of(0, DEFAULT_STM_LIMIT)
                    );

                // 转换为简化格式
                List<Map<String, String>> chatHistory = new ArrayList<>();
                for (Message msg : history) {
                    Map<String, String> entry = new HashMap<>();
                    entry.put("role", msg.getRole().name());
                    entry.put("content", msg.getContent());
                    chatHistory.add(entry);
                }
                context.setChatHistory(chatHistory);
                log.info(
                    "[MemoryHydration] Loaded {} STM entries for execution: {}",
                    chatHistory.size(),
                    execution.getExecutionId()
                );
            } catch (Exception e) {
                log.warn(
                    "[MemoryHydration] Failed to load STM: {}",
                    e.getMessage()
                );
            }
        }
    }

    private String extractUserQuery(Map<String, Object> inputs) {
        Object query = inputs.get("input");
        if (query == null) query = inputs.get("query");
        if (query == null) query = inputs.get("message");
        return query != null ? query.toString() : null;
    }

    /**
     * 恢复暂停的执行
     */
    public void resumeExecution(
        String executionId,
        String nodeId,
        Integer expectedVersion,
        Map<String, Object> edits,
        Long reviewerId,
        String comment,
        Map<String, Map<String, Object>> nodeEdits
    ) {
        log.info(
            "[Scheduler] Resuming execution: {}, node: {}",
            executionId,
            nodeId
        );

        if (isCancelled(executionId)) {
            log.warn(
                "[Scheduler] Execution {} is cancelled, cannot resume.",
                executionId
            );
            return;
        }

        String lockKey = "lock:exec:" + executionId;
        RLock lock = redisService.getLock(lockKey);

        try {
            lock.lock(30, TimeUnit.SECONDS);

            Execution execution = executionRepository
                .findById(executionId)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Execution not found: " + executionId
                    )
                );

            // paused node consistency guard
            String pausedNodeId = execution.getPausedNodeId();
            if (!StringUtils.hasText(pausedNodeId)) {
                throw new IllegalStateException(
                    "Execution is not paused at a concrete node"
                );
            }
            if (!StringUtils.hasText(nodeId) || !pausedNodeId.equals(nodeId)) {
                throw new IllegalArgumentException(
                    "Resume nodeId mismatch, expected pausedNodeId=" +
                        pausedNodeId +
                        ", actual=" +
                        nodeId
                );
            }

            if (
                expectedVersion != null &&
                !expectedVersion.equals(execution.getVersion())
            ) {
                throw new OptimisticLockingFailureException(
                    "Execution version conflict, expected=" +
                        expectedVersion +
                        ", actual=" +
                        execution.getVersion()
                );
            }

            // 2. 区分 Phase 处理
            TriggerPhase pausedPhase = execution.getPausedPhase();
            if (pausedPhase == null) pausedPhase = TriggerPhase.AFTER_EXECUTION; // Default

            // 3. Context Merging — 当前节点 edits
            if (edits != null && !edits.isEmpty()) {
                if (pausedPhase == TriggerPhase.BEFORE_EXECUTION) {
                    // BEFORE: edits 作为 additionalInputs，后续 resume 会放入 SharedState
                } else {
                    // AFTER: 更新当前节点的输出
                    execution.getContext().setNodeOutput(nodeId, edits);
                }
            }

            // 3b. 多节点 edits — 更新上游节点的输出
            if (nodeEdits != null && !nodeEdits.isEmpty()) {
                for (Map.Entry<
                    String,
                    Map<String, Object>
                > entry : nodeEdits.entrySet()) {
                    String editNodeId = entry.getKey();
                    Map<String, Object> editData = entry.getValue();
                    if (editData != null && !editData.isEmpty()) {
                        execution
                            .getContext()
                            .setNodeOutput(editNodeId, editData);
                        log.info(
                            "[Scheduler] Applied nodeEdits for node: {}",
                            editNodeId
                        );
                    }
                }
            }

            // 4. Audit Log
            HumanReviewRecord record = HumanReviewRecord.builder()
                .executionId(executionId)
                .nodeId(nodeId)
                .reviewerId(reviewerId)
                .decision("APPROVE") // resume implies approve
                .triggerPhase(pausedPhase)
                .modifiedData(edits != null ? serializeToJson(edits) : null)
                .comment(comment)
                .reviewedAt(java.time.LocalDateTime.now())
                .build();
            humanReviewRepository.save(record);

            // 5. Resume in Domain
            // Note: pass edits as additionalInputs to resume, which puts them in shared
            // state
            List<Node> readyNodes = execution.resume(nodeId, edits);

            // 6. Persistence
            executionRepository.update(execution);

            // 7. Publish Event
            StreamContext streamContext = StreamContext.builder()
                .executionId(executionId)
                .nodeId(nodeId)
                .build();
            StreamPublisher publisher = streamPublisherFactory.create(
                streamContext
            );
            // Payload needs to be structured
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "workflow_resumed");
            payload.put("executionId", executionId);
            publisher.publishEvent("workflow_resumed", payload);

            // 从待审核队列移除
            humanReviewQueuePort.removeFromPendingQueue(executionId);

            // 8. Schedule Next
            if (pausedPhase == TriggerPhase.AFTER_EXECUTION) {
                // Manually trigger onNodeComplete logic because node execution is "skipped"
                // (results provided by human or previous run)
                // We need to fetch the "Result" to pass to advance.
                // Since we set output in context, we can construct success result.
                Map<String, Object> outputs =
                    edits != null
                        ? edits
                        : execution.getContext().getNodeOutput(nodeId);
                NodeExecutionResult result = NodeExecutionResult.success(
                    outputs
                );

                Node node = execution.getGraph().getNode(nodeId);
                // Call onNodeComplete to trigger advance and next scheduling
                onNodeComplete(
                    executionId,
                    nodeId,
                    node.getName(),
                    node.getType(),
                    result,
                    execution.getContext().getInputs()
                );
            } else {
                // BEFORE_EXECUTION: readyNodes contains the node itself to be re-run
                scheduleNodes(executionId, readyNodes, null);
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 拒绝暂停中的执行
     */
    public void rejectExecution(
        String executionId,
        String nodeId,
        Integer expectedVersion,
        Long reviewerId,
        String reason
    ) {
        log.info(
            "[Scheduler] Rejecting execution: {}, node: {}",
            executionId,
            nodeId
        );

        if (isCancelled(executionId)) {
            log.warn(
                "[Scheduler] Execution {} is cancelled, cannot reject.",
                executionId
            );
            return;
        }

        String lockKey = "lock:exec:" + executionId;
        RLock lock = redisService.getLock(lockKey);

        try {
            lock.lock(30, TimeUnit.SECONDS);

            Execution execution = executionRepository
                .findById(executionId)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Execution not found: " + executionId
                    )
                );

            String pausedNodeId = execution.getPausedNodeId();
            if (!StringUtils.hasText(pausedNodeId)) {
                throw new IllegalStateException(
                    "Execution is not paused at a concrete node"
                );
            }
            if (!StringUtils.hasText(nodeId) || !pausedNodeId.equals(nodeId)) {
                throw new IllegalArgumentException(
                    "Reject nodeId mismatch, expected pausedNodeId=" +
                        pausedNodeId +
                        ", actual=" +
                        nodeId
                );
            }

            if (
                expectedVersion != null &&
                !expectedVersion.equals(execution.getVersion())
            ) {
                throw new OptimisticLockingFailureException(
                    "Execution version conflict, expected=" +
                        expectedVersion +
                        ", actual=" +
                        execution.getVersion()
                );
            }

            TriggerPhase pausedPhase = execution.getPausedPhase();
            if (pausedPhase == null) {
                pausedPhase = TriggerPhase.AFTER_EXECUTION;
            }

            Map<String, Object> originalData =
                execution.getContext() != null
                    ? execution.getContext().getNodeOutput(nodeId)
                    : null;

            HumanReviewRecord record = HumanReviewRecord.builder()
                .executionId(executionId)
                .nodeId(nodeId)
                .reviewerId(reviewerId)
                .decision("REJECT")
                .triggerPhase(pausedPhase)
                .originalData(
                    originalData != null && !originalData.isEmpty()
                        ? serializeToJson(originalData)
                        : null
                )
                .comment(reason)
                .reviewedAt(java.time.LocalDateTime.now())
                .build();
            humanReviewRepository.save(record);

            execution.reject(nodeId);
            executionRepository.update(execution);

            StreamContext streamContext = StreamContext.builder()
                .executionId(executionId)
                .nodeId(nodeId)
                .build();
            StreamPublisher publisher = streamPublisherFactory.create(
                streamContext
            );
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "workflow_rejected");
            payload.put("executionId", executionId);
            payload.put("nodeId", nodeId);
            payload.put("reason", reason);
            publisher.publishEvent("workflow_rejected", payload);

            try {
                publisher.publishFinish(
                    NodeExecutionResult.failed(
                        reason != null ? reason : "Human review rejected"
                    )
                );
            } catch (Exception ex) {
                log.warn(
                    "[Scheduler] Failed to publish reject finish event for {}: {}",
                    executionId,
                    ex.getMessage()
                );
            }

            humanReviewQueuePort.removeFromPendingQueue(executionId);

            String assistantMessageId = execution.getAssistantMessageId();
            if (StringUtils.hasText(assistantMessageId)) {
                String rejectContent = buildRejectSummary(
                    execution,
                    nodeId,
                    reason
                );
                List<
                    com.zj.aiagent.domain.chat.valobj.ThoughtStep
                > thoughtSteps = buildThoughtSteps(executionId);
                chatApplicationService.finalizeMessage(
                    assistantMessageId,
                    rejectContent,
                    thoughtSteps,
                    com.zj.aiagent.domain.chat.valobj.MessageStatus.FAILED
                );
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void scheduleNodes(
        String executionId,
        List<Node> nodes,
        String parentId
    ) {
        if (nodes == null || nodes.isEmpty()) {
            log.info(
                "[Scheduler] No nodes to schedule for execution: {}",
                executionId
            );
            return;
        }

        if (isCancelled(executionId)) {
            log.warn(
                "[Scheduler] Execution {} cancelled, stopping schedule.",
                executionId
            );
            return;
        }

        if (isExecutionPaused(executionId)) {
            log.info(
                "[Scheduler] Execution {} paused, skip scheduling nodes",
                executionId
            );
            return;
        }

        log.info(
            "[Scheduler] Scheduling {} nodes for execution: {}, parentId: {}",
            nodes.size(),
            executionId,
            parentId
        );

        String effectiveParentId = parentId;
        if (effectiveParentId == null && nodes.size() > 1) {
            effectiveParentId = "parallel_" + System.currentTimeMillis();
            log.debug(
                "[Scheduler] Generated parallel group ID: {}",
                effectiveParentId
            );
        }

        for (Node node : nodes) {
            scheduleNode(executionId, node, effectiveParentId);
        }
    }

    private void scheduleNode(String executionId, Node node, String parentId) {
        log.info(
            "[Scheduler] Dispatching node: {} (type: {})",
            node.getNodeId(),
            node.getType()
        );

        if (isCancelled(executionId)) {
            return;
        }

        // 判断是否为最终输出节点
        // 1. END 节点始终是最终输出节点
        // 2. 其他节点默认不是最终输出节点(输出显示在思维链中)
        boolean isFinalOutputNode = "END".equals(node.getNodeId());

        StreamContext streamContext = StreamContext.builder()
            .executionId(executionId)
            .nodeId(node.getNodeId())
            .parentId(parentId)
            .nodeType(
                node.getType() != null ? node.getType().name() : "UNKNOWN"
            )
            .nodeName(node.getName())
            .isFinalOutputNode(isFinalOutputNode)
            .build();

        StreamPublisher streamPublisher = streamPublisherFactory.create(
            streamContext
        );

        // CHECK PAUSE: BEFORE_EXECUTION
        if (
            checkPause(
                executionId,
                node,
                TriggerPhase.BEFORE_EXECUTION,
                streamPublisher,
                null
            )
        ) {
            return;
        }

        streamPublisher.publishStart();

        NodeExecutorStrategy strategy = executorFactory.getStrategy(
            node.getType()
        );

        Execution execution = executionRepository
            .findById(executionId)
            .orElseThrow(() ->
                new IllegalStateException("Execution not found")
            );

        ExecutionContext context = execution.getContext();
        Map<String, Object> resolvedInputs = expressionResolver.resolveInputs(
            node.getInputs(),
            context
        );

        // Inject Context
        resolvedInputs.put("__context__", context);

        // Inject agentId for knowledge retrieval
        resolvedInputs.put("__agentId__", execution.getAgentId());

        // Inject outgoing edges for condition nodes
        resolvedInputs.put(
            "__outgoingEdges__",
            execution.getGraph().getOutgoingEdges(node.getNodeId())
        );

        CompletableFuture<NodeExecutionResult> future = strategy.executeAsync(
            node,
            resolvedInputs,
            streamPublisher
        );

        future.whenComplete((result, error) -> {
            if (error != null) {
                result = NodeExecutionResult.failed(error.getMessage());
                streamPublisher.publishError(error.getMessage());
            } else {
                streamPublisher.publishFinish(result);
            }

            if (isCancelled(executionId)) {
                log.warn(
                    "[Scheduler] Execution {} cancelled, skipping node completion logic.",
                    executionId
                );
                return;
            }

            // pause gate: avoid in-flight callback continuing workflow after pause
            try {
                Execution latestExecution = executionRepository
                    .findById(executionId)
                    .orElseThrow(() ->
                        new IllegalStateException(
                            "Execution not found: " + executionId
                        )
                    );
                if (
                    latestExecution.getStatus() == ExecutionStatus.PAUSED ||
                    latestExecution.getStatus() ==
                    ExecutionStatus.PAUSED_FOR_REVIEW
                ) {
                    log.info(
                        "[Scheduler] Execution {} is paused, ignore in-flight callback for node {}",
                        executionId,
                        node.getNodeId()
                    );
                    return;
                }
            } catch (Exception e) {
                log.warn(
                    "[Scheduler] Failed to evaluate pause gate for execution {}: {}",
                    executionId,
                    e.getMessage()
                );
            }

            onNodeComplete(
                executionId,
                node.getNodeId(),
                node.getName(),
                node.getType(),
                result,
                resolvedInputs
            );
        });
    }

    /**
     * 检查并处理暂停
     *
     * @return true if paused, false otherwise
     */
    private boolean checkPause(
        String executionId,
        Node node,
        TriggerPhase phase,
        StreamPublisher publisher,
        Map<String, Object> outputs
    ) {
        if (!node.requiresHumanReview()) {
            return false;
        }

        // 已通过审核的节点不再暂停
        Execution exec = executionRepository.findById(executionId).orElse(null);
        if (exec != null && exec.isNodeReviewed(node.getNodeId())) {
            log.info(
                "[Scheduler] Node {} already reviewed, skipping pause",
                node.getNodeId()
            );
            return false;
        }

        HumanReviewConfig config = node.getConfig().getHumanReviewConfig();
        TriggerPhase configuredPhase =
            config.getTriggerPhase() != null
                ? config.getTriggerPhase()
                : TriggerPhase.BEFORE_EXECUTION;
        if (configuredPhase != phase) {
            return false;
        }

        log.info(
            "[Scheduler] Node {} requires human review at {}, pausing execution",
            node.getNodeId(),
            phase
        );

        String lockKey = "lock:exec:" + executionId;
        RLock lock = redisService.getLock(lockKey);

        try {
            if (!lock.tryLock(30, TimeUnit.SECONDS)) {
                log.warn(
                    "[Scheduler] Failed to acquire lock for checkPause, executionId={}",
                    executionId
                );
                throw new IllegalStateException(
                    "Failed to acquire lock for execution: " + executionId
                );
            }

            Execution execution = executionRepository
                .findById(executionId)
                .orElseThrow(() ->
                    new IllegalStateException("Execution not found")
                );

            // 暂停执行 (如果是在 执行后 暂停，必须保存 Outputs)
            execution.advance(
                node.getNodeId(),
                NodeExecutionResult.paused(phase, outputs)
            );

            // 保存
            checkpointRepository.save(
                execution.createCheckpoint(node.getNodeId())
            );
            executionRepository.update(execution);

            // 发布事件
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "workflow_paused");
            payload.put("executionId", executionId);
            payload.put("nodeId", node.getNodeId());
            payload.put("triggerPhase", phase.name());
            publisher.publishEvent("workflow_paused", payload);

            // 添加到待审核队列
            humanReviewQueuePort.addToPendingQueue(executionId);

            // 暂停时更新 assistant 消息，避免切换会话后内容为空显示 "..."
            String assistantMessageId = execution.getAssistantMessageId();
            if (StringUtils.hasText(assistantMessageId)) {
                String pauseContent = buildPauseSummary(execution, node, phase);
                List<
                    com.zj.aiagent.domain.chat.valobj.ThoughtStep
                > thoughtSteps = buildThoughtSteps(executionId);
                chatApplicationService.finalizeMessage(
                    assistantMessageId,
                    pauseContent,
                    thoughtSteps,
                    com.zj.aiagent.domain.chat.valobj.MessageStatus.COMPLETED
                );
            }

            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String buildPauseSummary(
        Execution execution,
        Node node,
        TriggerPhase phase
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("⏸️ 工作流已在节点「").append(node.getName()).append("」");
        sb.append(phase == TriggerPhase.BEFORE_EXECUTION ? "执行前" : "执行后");
        sb.append("暂停，等待人工审核。\n\n");

        ExecutionContext ctx = execution.getContext();
        if (ctx != null) {
            for (Map.Entry<String, ExecutionStatus> entry : execution
                .getNodeStatuses()
                .entrySet()) {
                if (entry.getValue() == ExecutionStatus.SUCCEEDED) {
                    Map<String, Object> output = ctx.getNodeOutput(
                        entry.getKey()
                    );
                    if (output != null && !output.isEmpty()) {
                        Node n = execution
                            .getGraph()
                            .getNodes()
                            .get(entry.getKey());
                        String name = n != null ? n.getName() : entry.getKey();
                        sb.append("**").append(name).append("**: ");
                        String val = output
                            .values()
                            .iterator()
                            .next()
                            .toString();
                        sb.append(
                            val.length() > 200
                                ? val.substring(0, 200) + "..."
                                : val
                        );
                        sb.append("\n\n");
                    }
                }
            }
        }
        return sb.toString();
    }

    private String buildRejectSummary(
        Execution execution,
        String nodeId,
        String reason
    ) {
        Node node =
            execution.getGraph() != null
                ? execution.getGraph().getNode(nodeId)
                : null;
        String nodeName = node != null ? node.getName() : nodeId;

        StringBuilder sb = new StringBuilder();
        sb
            .append("❌ 工作流已在节点「")
            .append(nodeName)
            .append("」被人工审核拒绝，执行已终止。");

        if (StringUtils.hasText(reason)) {
            sb.append("\n\n拒绝原因：").append(reason.trim());
        }

        return sb.toString();
    }

    private void onNodeComplete(
        String executionId,
        String nodeId,
        String nodeName,
        NodeType nodeType,
        NodeExecutionResult result,
        Map<String, Object> inputs
    ) {
        log.info(
            "[Scheduler] Node {} completed with status: {}",
            nodeId,
            result.getStatus()
        );

        String lockKey = "lock:exec:" + executionId;
        RLock lock = redisService.getLock(lockKey);

        try {
            lock.lock(30, TimeUnit.SECONDS);

            // CHECK PAUSE: AFTER_EXECUTION
            // Need to reload Node to check config
            // But we don't have Node object easily here without loading Execution or Graph.
            // Loading execution anyway.

            Execution execution = executionRepository
                .findById(executionId)
                .orElseThrow(() ->
                    new IllegalStateException(
                        "Execution not found: " + executionId
                    )
                );

            Node node = execution.getGraph().getNode(nodeId);

            // Create temp publisher or reuse?
            StreamContext streamContext = StreamContext.builder()
                .executionId(executionId)
                .nodeId(nodeId)
                .build();
            StreamPublisher publisher = streamPublisherFactory.create(
                streamContext
            );

            if (
                result.isSuccess() &&
                checkPause(
                    executionId,
                    node,
                    TriggerPhase.AFTER_EXECUTION,
                    publisher,
                    result.getOutputs()
                )
            ) {
                return;
            }

            // 3. 推进执行
            List<Node> nextNodes = execution.advance(nodeId, result);

            // 3.1 [Awareness]
            String summary = generateNodeSummary(nodeType, result);
            execution.getContext().appendLog(nodeId, nodeName, summary);

            // 4. Save Checkpoint
            checkpointRepository.save(execution.createCheckpoint(nodeId));

            // 5. Update DB
            executionRepository.update(execution);

            // 6. Publish Event
            // 判断 renderMode: 只有最终输出节点(END)才是 MESSAGE,其他都是 THOUGHT
            String renderMode;
            if ("END".equals(nodeId)) {
                renderMode = "MESSAGE"; // 最终输出节点
            } else if (result.getStatus() == ExecutionStatus.SUCCEEDED) {
                renderMode = "THOUGHT"; // 成功的中间节点,显示在思维链中
            } else {
                renderMode = "HIDDEN"; // 失败的节点,隐藏
            }

            NodeCompletedEvent logEvent = NodeCompletedEvent.builder()
                .executionId(executionId)
                .nodeId(nodeId)
                .nodeName(nodeName)
                .nodeType(nodeType.name())
                .renderMode(renderMode)
                .status(result.getStatus().getCode())
                .inputs(inputs)
                .outputs(result.getOutputs())
                .errorMessage(result.getErrorMessage())
                .startTime(java.time.LocalDateTime.now())
                .endTime(java.time.LocalDateTime.now())
                .build();
            applicationEventPublisher.publishEvent(logEvent);

            // ========== 新增: 检查是否完成 ==========
            if (
                execution.getStatus() == ExecutionStatus.SUCCEEDED ||
                execution.getStatus() == ExecutionStatus.FAILED
            ) {
                log.info(
                    "[Scheduler] Execution {} finished with status: {}",
                    executionId,
                    execution.getStatus()
                );

                // 保存最终消息
                onExecutionComplete(execution);

                // 发送 execution-level FINISH 事件，通知前端 SSE 流可以关闭
                try {
                    NodeExecutionResult finishResult =
                        NodeExecutionResult.success(
                            java.util.Map.of(
                                "status",
                                execution.getStatus().name()
                            )
                        );
                    publisher.publishFinish(finishResult);
                } catch (Exception ex) {
                    log.warn(
                        "[Scheduler] Failed to publish execution finish event: {}",
                        ex.getMessage()
                    );
                }
                return;
            }

            scheduleNodes(executionId, nextNodes, null);
        } catch (Exception e) {
            log.error(
                "[Scheduler] Error onNodeComplete {}: {}",
                nodeId,
                e.getMessage(),
                e
            );
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public void cancelExecution(String executionId) {
        log.info("[Scheduler] Cancelling execution: {}", executionId);
        cancellationPort.markAsCancelled(executionId);
    }

    public void pauseExecution(String executionId) {
        log.info("[Scheduler] Pausing execution: {}", executionId);

        String lockKey = "lock:exec:" + executionId;
        RLock lock = redisService.getLock(lockKey);

        try {
            if (!lock.tryLock(30, TimeUnit.SECONDS)) {
                log.warn(
                    "[Scheduler] Failed to acquire lock for pauseExecution, executionId={}",
                    executionId
                );
                throw new IllegalStateException(
                    "Failed to acquire lock for execution: " + executionId
                );
            }
            Execution execution = executionRepository
                .findById(executionId)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Execution not found: " + executionId
                    )
                );

            if (
                execution.getStatus() == ExecutionStatus.PAUSED ||
                execution.getStatus() == ExecutionStatus.PAUSED_FOR_REVIEW ||
                execution.getStatus() == ExecutionStatus.SUCCEEDED ||
                execution.getStatus() == ExecutionStatus.FAILED ||
                execution.getStatus() == ExecutionStatus.CANCELLED
            ) {
                log.info(
                    "[Scheduler] Execution {} status is {}, skip pause",
                    executionId,
                    execution.getStatus()
                );
                return;
            }

            execution.setStatus(ExecutionStatus.PAUSED);
            execution.setPausedNodeId("__MANUAL_PAUSE__");
            execution.setPausedPhase(null);
            execution.setUpdatedAt(java.time.LocalDateTime.now());
            execution.setVersion(execution.getVersion() + 1);

            checkpointRepository.save(
                execution.createCheckpoint("__MANUAL_PAUSE__")
            );
            executionRepository.update(execution);

            StreamContext streamContext = StreamContext.builder()
                .executionId(executionId)
                .nodeId("__MANUAL_PAUSE__")
                .build();
            StreamPublisher publisher = streamPublisherFactory.create(
                streamContext
            );
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "workflow_paused");
            payload.put("executionId", executionId);
            payload.put("nodeId", "__MANUAL_PAUSE__");
            payload.put("triggerPhase", "MANUAL");
            publisher.publishEvent("workflow_paused", payload);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private boolean isCancelled(String executionId) {
        return cancellationPort.isCancelled(executionId);
    }

    private boolean isExecutionPaused(String executionId) {
        try {
            return executionRepository
                .findById(executionId)
                .map(
                    execution ->
                        execution.getStatus() == ExecutionStatus.PAUSED ||
                        execution.getStatus() ==
                        ExecutionStatus.PAUSED_FOR_REVIEW
                )
                .orElse(false);
        } catch (Exception e) {
            log.warn(
                "[Scheduler] Failed to check paused status for {}: {}",
                executionId,
                e.getMessage()
            );
            return false;
        }
    }

    private String generateNodeSummary(
        NodeType nodeType,
        NodeExecutionResult result
    ) {
        if (result.getStatus() == ExecutionStatus.FAILED) {
            return (
                "执行失败: " +
                (result.getErrorMessage() != null
                    ? result.getErrorMessage()
                    : "未知错误")
            );
        }
        Map<String, Object> outputs = result.getOutputs();
        if (outputs == null || outputs.isEmpty()) {
            return "执行完成";
        }
        switch (nodeType) {
            case LLM:
                Object response = outputs.get("response");
                if (response == null) response = outputs.get("text");
                if (response != null) {
                    String text = response.toString();
                    return (
                        "LLM响应: " +
                        (text.length() > 100
                            ? text.substring(0, 100) + "..."
                            : text)
                    );
                }
                return "LLM执行完成";
            case HTTP:
                Object statusCode = outputs.get("statusCode");
                return (
                    "HTTP请求完成, 状态码: " +
                    (statusCode != null ? statusCode : "N/A")
                );
            case CONDITION:
                Object branch = outputs.get("selectedBranchId");
                return (
                    "条件判断完成, 选择分支: " +
                    (branch != null ? branch : "default")
                );
            default:
                return nodeType.name() + " 节点执行完成";
        }
    }

    /**
     * 工作流执行完成回调
     * 提取最终响应并更新 Assistant 消息
     */
    private void onExecutionComplete(Execution execution) {
        String executionId = execution.getExecutionId();
        String assistantMessageId = execution.getAssistantMessageId();

        // 检查是否有关联的消息
        if (!StringUtils.hasText(assistantMessageId)) {
            log.debug(
                "[Scheduler] No assistant message associated with execution: {}",
                executionId
            );
            return;
        }

        try {
            if (execution.getStatus() == ExecutionStatus.SUCCEEDED) {
                // 方案1: 直接从 Execution 上下文获取 END 节点的输出
                String finalResponse = extractFinalResponseFromExecution(
                    execution
                );

                // 方案2: 异步延迟查询数据库（给异步保存留出时间）
                // 如果方案1失败，使用方案2作为后备
                if (finalResponse == null || finalResponse.equals("执行完成")) {
                    log.warn(
                        "[Scheduler] Failed to extract response from execution context, will retry from database"
                    );
                    // 延迟100ms后查询数据库
                    CompletableFuture.runAsync(() -> {
                        try {
                            Thread.sleep(100);
                            String response = extractFinalResponseFromLogs(
                                executionId
                            );
                            List<
                                com.zj.aiagent.domain.chat.valobj.ThoughtStep
                            > thoughtSteps = buildThoughtSteps(executionId);

                            chatApplicationService.finalizeMessage(
                                assistantMessageId,
                                response,
                                thoughtSteps,
                                com.zj.aiagent.domain.chat.valobj.MessageStatus.COMPLETED
                            );
                            log.info(
                                "[Scheduler] Updated assistant message {} with final response (delayed) for execution: {}",
                                assistantMessageId,
                                executionId
                            );
                        } catch (Exception e) {
                            log.error(
                                "[Scheduler] Failed to update message in delayed task: {}",
                                e.getMessage(),
                                e
                            );
                        }
                    });
                    return;
                }

                // 构建思维链（从执行日志中提取，也需要延迟）
                CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(100);
                        List<
                            com.zj.aiagent.domain.chat.valobj.ThoughtStep
                        > thoughtSteps = buildThoughtSteps(executionId);

                        chatApplicationService.finalizeMessage(
                            assistantMessageId,
                            finalResponse,
                            thoughtSteps,
                            com.zj.aiagent.domain.chat.valobj.MessageStatus.COMPLETED
                        );
                        log.info(
                            "[Scheduler] Updated assistant message {} with final response for execution: {}",
                            assistantMessageId,
                            executionId
                        );
                    } catch (Exception e) {
                        log.error(
                            "[Scheduler] Failed to update message with thought steps: {}",
                            e.getMessage(),
                            e
                        );
                    }
                });
            } else if (execution.getStatus() == ExecutionStatus.FAILED) {
                // 更新消息为失败状态
                String errorMessage = "执行失败";

                // 尝试从上下文获取错误信息
                ExecutionContext context = execution.getContext();
                if (context != null) {
                    String logContent = context.getExecutionLogContent();
                    if (
                        StringUtils.hasText(logContent) &&
                        logContent.contains("失败")
                    ) {
                        // 提取最后一行日志作为错误信息
                        String[] lines = logContent.split("\n");
                        if (lines.length > 0) {
                            errorMessage = lines[lines.length - 1];
                        }
                    }
                }

                chatApplicationService.finalizeMessage(
                    assistantMessageId,
                    errorMessage,
                    null,
                    com.zj.aiagent.domain.chat.valobj.MessageStatus.FAILED
                );

                log.warn(
                    "[Scheduler] Updated assistant message {} with error for execution: {}",
                    assistantMessageId,
                    executionId
                );
            }
        } catch (Exception e) {
            log.error(
                "[Scheduler] Failed to update assistant message for execution {}: {}",
                executionId,
                e.getMessage(),
                e
            );
            // 不抛出异常，避免影响工作流的正常完成
        }
    }

    /**
     * 从 Execution 上下文直接提取最终响应
     * 优先级: END 节点输出（通过 sourceRef 引用上游） > LLM 节点输出 > fallback
     */
    private String extractFinalResponseFromExecution(Execution execution) {
        try {
            ExecutionContext context = execution.getContext();
            if (context == null) {
                log.warn(
                    "[Scheduler] Execution context is null for: {}",
                    execution.getExecutionId()
                );
                return "执行完成";
            }

            // 1. 优先从 END 节点输出获取（按节点类型查找，避免硬编码 nodeId 大小写问题）
            String endNodeId = null;
            for (Map.Entry<String, ExecutionStatus> entry : execution
                .getNodeStatuses()
                .entrySet()) {
                String nid = entry.getKey();
                Node n = execution.getGraph().getNode(nid);
                if (n != null && n.getType() == NodeType.END) {
                    endNodeId = nid;
                    break;
                }
            }

            if (StringUtils.hasText(endNodeId)) {
                Map<String, Object> endNodeOutput = context.getNodeOutput(
                    endNodeId
                );
                if (endNodeOutput != null && !endNodeOutput.isEmpty()) {
                    for (Map.Entry<
                        String,
                        Object
                    > outputEntry : endNodeOutput.entrySet()) {
                        String key = outputEntry.getKey();
                        if (key.startsWith("__")) continue;
                        Object value = outputEntry.getValue();
                        if (value != null && !value.toString().isEmpty()) {
                            log.info(
                                "[Scheduler] Extracted response from END node output, nodeId={}, key={}, length={}",
                                endNodeId,
                                key,
                                value.toString().length()
                            );
                            return value.toString();
                        }
                    }
                }
            }

            // 2. Fallback: 从 LLM 节点输出中提取
            for (Map.Entry<String, ExecutionStatus> entry : execution
                .getNodeStatuses()
                .entrySet()) {
                String nid = entry.getKey();
                Node node = execution.getGraph().getNode(nid);
                if (node != null && node.getType() == NodeType.LLM) {
                    Map<String, Object> llmOutput = context.getNodeOutput(nid);
                    if (llmOutput != null) {
                        Object response = llmOutput.get("response");
                        if (response == null) response = llmOutput.get("text");
                        if (
                            response != null && !response.toString().isEmpty()
                        ) {
                            log.info(
                                "[Scheduler] Extracted response from LLM node {}: length={}",
                                nid,
                                response.toString().length()
                            );
                            return response.toString();
                        }
                    }
                }
            }

            log.warn(
                "[Scheduler] No valid response found in execution context"
            );
            return "执行完成";
        } catch (Exception e) {
            log.error(
                "[Scheduler] Error extracting response from execution: {}",
                e.getMessage(),
                e
            );
            return "执行完成";
        }
    }

    private String buildExecutionStatusMessage(
        Execution execution,
        ExecutionStatus status
    ) {
        if (
            status == ExecutionStatus.PAUSED ||
            status == ExecutionStatus.PAUSED_FOR_REVIEW
        ) {
            return "工作流已暂停，正在等待人工审核或外部干预。";
        }

        if (status == ExecutionStatus.CANCELLED) {
            return "工作流已取消。";
        }

        ExecutionContext context = execution.getContext();
        if (context != null) {
            String logContent = context.getExecutionLogContent();
            if (StringUtils.hasText(logContent)) {
                String[] lines = logContent.split("\n");
                for (int i = lines.length - 1; i >= 0; i--) {
                    String line = lines[i] != null ? lines[i].trim() : "";
                    if (!line.isEmpty()) {
                        return line;
                    }
                }
            }
        }

        return "工作流执行结束，状态为 " + status.name();
    }

    /**
     * 构建思维链步骤
     * 从工作流节点执行日志中提取，转换为 ThoughtStep 格式
     */
    private List<
        com.zj.aiagent.domain.chat.valobj.ThoughtStep
    > buildThoughtSteps(String executionId) {
        List<com.zj.aiagent.domain.chat.valobj.ThoughtStep> steps =
            new ArrayList<>();

        try {
            List<WorkflowNodeExecutionLog> logs =
                workflowNodeExecutionLogRepository.findByExecutionIdOrderByEndTime(
                    executionId
                );

            for (WorkflowNodeExecutionLog log : logs) {
                // 只包含 MESSAGE 或 THOUGHT 渲染模式的节点
                if (
                    "MESSAGE".equals(log.getRenderMode()) ||
                    "THOUGHT".equals(log.getRenderMode())
                ) {
                    // 计算执行时长
                    Long durationMs = null;
                    if (
                        log.getStartTime() != null && log.getEndTime() != null
                    ) {
                        durationMs = java.time.Duration.between(
                            log.getStartTime(),
                            log.getEndTime()
                        ).toMillis();
                    }

                    // 构建内容摘要
                    String content = buildStepContent(log);

                    // 映射状态
                    String status = mapExecutionStatus(log.getStatus());

                    com.zj.aiagent.domain.chat.valobj.ThoughtStep step =
                        com.zj.aiagent.domain.chat.valobj.ThoughtStep.builder()
                            .stepId(log.getNodeId())
                            .title(log.getNodeName())
                            .content(content)
                            .durationMs(durationMs)
                            .status(status)
                            .type("log")
                            .build();
                    steps.add(step);
                }
            }

            log.debug(
                "[Scheduler] Built {} thought steps for execution: {}",
                steps.size(),
                executionId
            );
        } catch (Exception e) {
            log.warn(
                "[Scheduler] Failed to build thought steps for execution {}: {}",
                executionId,
                e.getMessage()
            );
        }

        return steps;
    }

    /**
     * 构建步骤内容摘要
     */
    private String buildStepContent(WorkflowNodeExecutionLog log) {
        if (log.getErrorMessage() != null) {
            return "错误: " + log.getErrorMessage();
        }

        Map<String, Object> outputs = log.getOutputs();
        if (outputs != null && !outputs.isEmpty()) {
            // 尝试提取响应内容
            Object response = outputs.get("response");
            if (response == null) response = outputs.get("text");
            if (response == null) response = outputs.get("output");

            if (response != null) {
                String text = response.toString();
                // 限制长度
                return text.length() > 200
                    ? text.substring(0, 200) + "..."
                    : text;
            }
        }

        return log.getNodeType() + " 节点执行完成";
    }

    /**
     * 映射执行状态
     * 0:Running -> RUNNING
     * 1:Success -> SUCCESS
     * 2:Failed -> FAILED
     */
    private String mapExecutionStatus(Integer status) {
        if (status == null) return "UNKNOWN";
        switch (status) {
            case 0:
                return "PENDING";
            case 1:
                return "RUNNING";
            case 2:
                return "SUCCESS";
            case 3:
                return "FAILED";
            case 4:
                return "SKIPPED";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * 从执行日志中提取最终响应
     *
     * 策略:
     * 1. 优先查询 END 节点的输出
     * 2. 如果 END 节点没有输出，查询最后执行的节点
     * 3. 提取 response、text、output 或 result 字段
     * 4. 返回默认值 "执行完成" 如果没有找到
     */
    private String extractFinalResponseFromLogs(String executionId) {
        try {
            log.info(
                "[Scheduler] Extracting final response for execution: {}",
                executionId
            );

            // 1. 优先查询 END 节点的输出
            WorkflowNodeExecutionLog endNodeLog =
                workflowNodeExecutionLogRepository.findByExecutionIdAndNodeId(
                    executionId,
                    "END"
                );

            if (endNodeLog != null) {
                log.info(
                    "[Scheduler] Found END node log for execution: {}, outputs: {}",
                    executionId,
                    endNodeLog.getOutputs()
                );

                if (endNodeLog.getOutputs() != null) {
                    Map<String, Object> outputs = endNodeLog.getOutputs();

                    // 尝试提取响应字段
                    Object response = outputs.get("response");
                    if (response == null) response = outputs.get("text");
                    if (response == null) response = outputs.get("output");
                    if (response == null) response = outputs.get("result");

                    if (response != null) {
                        log.info(
                            "[Scheduler] Extracted final response from END node: {}",
                            response
                                .toString()
                                .substring(
                                    0,
                                    Math.min(100, response.toString().length())
                                )
                        );
                        return response.toString();
                    } else {
                        log.warn(
                            "[Scheduler] END node outputs exist but no response field found. Available keys: {}",
                            outputs.keySet()
                        );
                    }
                } else {
                    log.warn(
                        "[Scheduler] END node log found but outputs is null for execution: {}",
                        executionId
                    );
                }
            } else {
                log.warn(
                    "[Scheduler] No END node log found for execution: {}",
                    executionId
                );
            }

            // 2. 如果 END 节点没有输出，查询最后执行的节点
            List<WorkflowNodeExecutionLog> allLogs =
                workflowNodeExecutionLogRepository.findByExecutionIdOrderByEndTime(
                    executionId
                );

            log.info(
                "[Scheduler] Found {} total logs for execution: {}",
                allLogs.size(),
                executionId
            );

            if (!allLogs.isEmpty()) {
                WorkflowNodeExecutionLog lastLog = allLogs.get(
                    allLogs.size() - 1
                );
                log.info(
                    "[Scheduler] Last executed node: {}, type: {}, outputs: {}",
                    lastLog.getNodeId(),
                    lastLog.getNodeType(),
                    lastLog.getOutputs()
                );

                Map<String, Object> outputs = lastLog.getOutputs();

                if (outputs != null && !outputs.isEmpty()) {
                    // 尝试提取响应
                    Object response = outputs.get("response");
                    if (response == null) response = outputs.get("text");
                    if (response == null) response = outputs.get("output");
                    if (response == null) response = outputs.get("result");

                    if (response != null) {
                        log.info(
                            "[Scheduler] Extracted final response from last node {}: {}",
                            lastLog.getNodeId(),
                            response
                                .toString()
                                .substring(
                                    0,
                                    Math.min(100, response.toString().length())
                                )
                        );
                        return response.toString();
                    } else {
                        log.warn(
                            "[Scheduler] Last node outputs exist but no response field found. Available keys: {}",
                            outputs.keySet()
                        );
                    }
                } else {
                    log.warn(
                        "[Scheduler] Last node outputs is null or empty for execution: {}",
                        executionId
                    );
                }
            } else {
                log.error(
                    "[Scheduler] No execution logs found at all for execution: {}",
                    executionId
                );
            }

            log.warn(
                "[Scheduler] No final response found for execution: {}, returning default message",
                executionId
            );
            return "执行完成";
        } catch (Exception e) {
            log.error(
                "[Scheduler] Error extracting final response for execution {}: {}",
                executionId,
                e.getMessage(),
                e
            );
            return "执行完成";
        }
    }

    private String serializeToJson(Map<String, Object> map) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
                map
            );
        } catch (Exception e) {
            log.warn(
                "[Scheduler] Failed to serialize map to JSON: {}",
                e.getMessage()
            );
            return "{}";
        }
    }
}
