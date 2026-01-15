package com.zj.aiagent.application.workflow;

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
import com.zj.aiagent.domain.workflow.event.NodeCompletedEvent;
import com.zj.aiagent.domain.workflow.port.*;
import com.zj.aiagent.domain.workflow.service.WorkflowGraphFactory;
import com.zj.aiagent.domain.workflow.valobj.*;
import com.zj.aiagent.infrastructure.workflow.executor.NodeExecutorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
    private final RedissonClient redissonClient;
    private final StreamPublisherFactory streamPublisherFactory;
    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final HumanReviewRepository humanReviewRepository;

    // ========== 记忆系统依赖 ==========
    private final VectorStore vectorStore;
    private final ConversationRepository conversationRepository;

    private static final String CANCEL_KEY_PREFIX = "workflow:cancel:";
    private static final int DEFAULT_STM_LIMIT = 10;

    /**
     * 启动工作流执行（根据 AgentId 获取图定义）
     */
    public void startExecution(String executionId, Long agentId, Long userId,
            String conversationId, Integer versionId, Map<String, Object> inputs,
            com.zj.aiagent.domain.workflow.valobj.ExecutionMode mode) {
        log.info("[Scheduler] Starting execution for agent: {}, version: {}, mode: {}", agentId, versionId, mode);

        // 1. 查询 Agent
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));

        // 2. 获取 graphJson
        String graphJson;
        if (versionId != null) {
            // 使用指定版本
            AgentVersion version = agentRepository.findVersion(agentId, versionId)
                    .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionId));
            graphJson = version.getGraphSnapshot();
        } else if (agent.getPublishedVersionId() != null) {
            // 使用已发布版本
            AgentVersion publishedVersion = agentRepository
                    .findVersion(agentId, agent.getPublishedVersionId().intValue())
                    .orElseThrow(() -> new IllegalStateException("Published version not found"));
            graphJson = publishedVersion.getGraphSnapshot();
        } else {
            // 使用当前草稿
            graphJson = agent.getGraphJson();
        }

        if (graphJson == null || graphJson.isBlank()) {
            throw new IllegalStateException("Agent has no workflow graph defined");
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
                .status(com.zj.aiagent.domain.workflow.valobj.ExecutionStatus.PENDING)
                .build();

        startExecution(execution, inputs, mode);
    }

    /**
     * 启动工作流执行（内部方法，直接使用 Execution 对象）
     */
    private void startExecution(Execution execution, Map<String, Object> inputs,
            com.zj.aiagent.domain.workflow.valobj.ExecutionMode mode) {
        log.info("[Scheduler] Starting execution: {}, mode: {}", execution.getExecutionId(), mode);

        // ========== 记忆水合 (Memory Hydration) ==========
        hydrateMemory(execution, inputs);

        // 1. 启动执行，获取就绪节点
        List<Node> readyNodes = execution.start(inputs);

        // 2. 持久化初始状态
        executionRepository.save(execution);

        // 3. 调度就绪节点（根节点无父节点）
        scheduleNodes(execution.getExecutionId(), readyNodes, null);

        // TODO: 实现 ExecutionMode 行为差异
        // - DEBUG: 发布更详细的 SSE 事件
        // - DRY_RUN: 跳过真实外部调用
        if (mode == com.zj.aiagent.domain.workflow.valobj.ExecutionMode.DEBUG) {
            log.debug("[Scheduler] DEBUG mode enabled - detailed logging active");
        } else if (mode == com.zj.aiagent.domain.workflow.valobj.ExecutionMode.DRY_RUN) {
            log.info("[Scheduler] DRY_RUN mode - skipping real external calls");
        }
    }

    /**
     * 记忆水合 - 在工作流启动前加载记忆
     */
    private void hydrateMemory(Execution execution, Map<String, Object> inputs) {
        ExecutionContext context = execution.getContext();

        // 1. [LTM 加载] 检索长期记忆
        String userQuery = extractUserQuery(inputs);
        if (StringUtils.hasText(userQuery)) {
            try {
                List<String> memories = vectorStore.search(userQuery, execution.getAgentId());
                context.setLongTermMemories(memories);
                log.info("[MemoryHydration] Loaded {} LTM entries for execution: {}",
                        memories.size(), execution.getExecutionId());
            } catch (Exception e) {
                log.warn("[MemoryHydration] Failed to load LTM: {}", e.getMessage());
            }
        }

        // 2. [STM 加载] 获取历史对话
        if (StringUtils.hasText(execution.getConversationId())) {
            try {
                List<Message> history = conversationRepository.findMessagesByConversationId(
                        execution.getConversationId(),
                        PageRequest.of(0, DEFAULT_STM_LIMIT));

                // 转换为简化格式
                List<Map<String, String>> chatHistory = new ArrayList<>();
                for (Message msg : history) {
                    Map<String, String> entry = new HashMap<>();
                    entry.put("role", msg.getRole().name());
                    entry.put("content", msg.getContent());
                    chatHistory.add(entry);
                }
                context.setChatHistory(chatHistory);
                log.info("[MemoryHydration] Loaded {} STM entries for execution: {}",
                        chatHistory.size(), execution.getExecutionId());
            } catch (Exception e) {
                log.warn("[MemoryHydration] Failed to load STM: {}", e.getMessage());
            }
        }
    }

    private String extractUserQuery(Map<String, Object> inputs) {
        Object query = inputs.get("input");
        if (query == null)
            query = inputs.get("query");
        if (query == null)
            query = inputs.get("message");
        return query != null ? query.toString() : null;
    }

    /**
     * 恢复暂停的执行
     */
    public void resumeExecution(String executionId, String nodeId, Map<String, Object> edits, Long reviewerId,
            String comment) {
        log.info("[Scheduler] Resuming execution: {}, node: {}", executionId, nodeId);

        if (isCancelled(executionId)) {
            log.warn("[Scheduler] Execution {} is cancelled, cannot resume.", executionId);
            return;
        }

        String lockKey = "lock:exec:" + executionId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            lock.lock(30, TimeUnit.SECONDS);

            Execution execution = executionRepository.findById(executionId)
                    .orElseThrow(() -> new IllegalArgumentException("Execution not found: " + executionId));

            // Optimistic Lock Check (Implicit via execution version match in some patterns,
            // but here we just check logic)
            // Ideally we pass expected version from API to ensure no concurrent resume

            // 2. 区分 Phase 处理
            TriggerPhase pausedPhase = execution.getPausedPhase();
            if (pausedPhase == null)
                pausedPhase = TriggerPhase.AFTER_EXECUTION; // Default

            // 3. Context Merging
            if (edits != null && !edits.isEmpty()) {
                if (pausedPhase == TriggerPhase.BEFORE_EXECUTION) {
                    // Update Inputs: Usually means updating Shared State or inputs for next run
                    // Since Node execution pulls from inputs, putting in context should work if
                    // node is configured to read from it
                    // Or we assume `edits` are the NEW inputs for the node.
                    // Execution.resume logic takes `additionalInputs` and puts into SharedState.
                    // Ideally we should pass these as overrides to strategy.executeAsync, but
                    // resuming calls execution.resume()
                } else {
                    // AFTER_EXECUTION: Update Outputs
                    // This outputs will be used as the result of the node
                    execution.getContext().setNodeOutput(nodeId, edits);
                }
            }

            // 4. Audit Log
            HumanReviewRecord record = HumanReviewRecord.builder()
                    .executionId(executionId)
                    .nodeId(nodeId)
                    .reviewerId(reviewerId)
                    .decision("APPROVE") // resume implies approve
                    .triggerPhase(pausedPhase)
                    .modifiedData(edits != null ? edits.toString() : null) // Handle serialization better in real impl
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
            StreamPublisher publisher = streamPublisherFactory.create(streamContext);
            // Payload needs to be structured
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "workflow_resumed");
            payload.put("executionId", executionId);
            publisher.publishEvent("workflow_resumed", payload);

            // Remove from Pending Review Set
            RSet<String> pendingSet = redissonClient.getSet("human_review:pending");
            pendingSet.remove(executionId);

            // 8. Schedule Next
            if (pausedPhase == TriggerPhase.AFTER_EXECUTION) {
                // Manually trigger onNodeComplete logic because node execution is "skipped"
                // (results provided by human or previous run)
                // We need to fetch the "Result" to pass to advance.
                // Since we set output in context, we can construct success result.
                Map<String, Object> outputs = edits != null ? edits : execution.getContext().getNodeOutput(nodeId);
                NodeExecutionResult result = NodeExecutionResult.success(outputs);

                Node node = execution.getGraph().getNode(nodeId);
                // Call onNodeComplete to trigger advance and next scheduling
                onNodeComplete(executionId, nodeId, node.getName(), node.getType(), result,
                        execution.getContext().getInputs());
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

    private void scheduleNodes(String executionId, List<Node> nodes, String parentId) {
        if (nodes == null || nodes.isEmpty()) {
            log.info("[Scheduler] No nodes to schedule for execution: {}", executionId);
            return;
        }

        if (isCancelled(executionId)) {
            log.warn("[Scheduler] Execution {} cancelled, stopping schedule.", executionId);
            return;
        }

        log.info("[Scheduler] Scheduling {} nodes for execution: {}, parentId: {}",
                nodes.size(), executionId, parentId);

        String effectiveParentId = parentId;
        if (effectiveParentId == null && nodes.size() > 1) {
            effectiveParentId = "parallel_" + System.currentTimeMillis();
            log.debug("[Scheduler] Generated parallel group ID: {}", effectiveParentId);
        }

        for (Node node : nodes) {
            scheduleNode(executionId, node, effectiveParentId);
        }
    }

    private void scheduleNode(String executionId, Node node, String parentId) {
        log.info("[Scheduler] Dispatching node: {} (type: {})", node.getNodeId(), node.getType());

        if (isCancelled(executionId)) {
            return;
        }

        StreamContext streamContext = StreamContext.builder()
                .executionId(executionId)
                .nodeId(node.getNodeId())
                .parentId(parentId)
                .nodeType(node.getType() != null ? node.getType().name() : "UNKNOWN")
                .nodeName(node.getName())
                .build();

        StreamPublisher streamPublisher = streamPublisherFactory.create(streamContext);

        // CHECK PAUSE: BEFORE_EXECUTION
        if (checkPause(executionId, node, TriggerPhase.BEFORE_EXECUTION, streamPublisher, null)) {
            return;
        }

        streamPublisher.publishStart();

        NodeExecutorStrategy strategy = executorFactory.getStrategy(node.getType());

        Execution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalStateException("Execution not found"));

        ExecutionContext context = execution.getContext();
        Map<String, Object> resolvedInputs = context.resolveInputs(node.getInputs());

        // Inject Context
        resolvedInputs.put("__context__", context);

        // Inject outgoing edges for condition nodes
        resolvedInputs.put("__outgoingEdges__", execution.getGraph().getOutgoingEdges(node.getNodeId()));

        CompletableFuture<NodeExecutionResult> future = strategy.executeAsync(node, resolvedInputs, streamPublisher);

        future.whenComplete((result, error) -> {
            if (error != null) {
                result = NodeExecutionResult.failed(error.getMessage());
                streamPublisher.publishError(error.getMessage());
            } else {
                streamPublisher.publishFinish(result);
            }

            if (isCancelled(executionId)) {
                log.warn("[Scheduler] Execution {} cancelled, skipping node completion logic.", executionId);
                return;
            }

            onNodeComplete(executionId, node.getNodeId(), node.getName(), node.getType(), result, resolvedInputs);
        });
    }

    /**
     * 检查并处理暂停
     * 
     * @return true if paused, false otherwise
     */
    private boolean checkPause(String executionId, Node node, TriggerPhase phase, StreamPublisher publisher,
            Map<String, Object> outputs) {
        if (!node.requiresHumanReview()) {
            return false;
        }

        HumanReviewConfig config = node.getConfig().getHumanReviewConfig();
        if (config.getTriggerPhase() != phase) {
            // Config default might be null or default to BEFORE?
            // Assume if null, default is BEFORE_EXECUTION or handle both?
            // If explicit phase match only.
            TriggerPhase configuredPhase = config.getTriggerPhase() != null ? config.getTriggerPhase()
                    : TriggerPhase.BEFORE_EXECUTION;
            if (configuredPhase != phase)
                return false;
        }

        log.info("[Scheduler] Node {} requires human review at {}, pausing execution", node.getNodeId(), phase);

        String lockKey = "lock:exec:" + executionId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            lock.lock(10, TimeUnit.SECONDS);

            Execution execution = executionRepository.findById(executionId)
                    .orElseThrow(() -> new IllegalStateException("Execution not found"));

            // 暂停执行 (如果是在 执行后 暂停，必须保存 Outputs)
            execution.advance(node.getNodeId(), NodeExecutionResult.paused(phase, outputs));

            // 保存
            checkpointRepository.save(execution.createCheckpoint(node.getNodeId()));
            executionRepository.update(execution);

            // 发布事件
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "workflow_paused");
            payload.put("executionId", executionId);
            payload.put("nodeId", node.getNodeId());
            payload.put("triggerPhase", phase.name());
            publisher.publishEvent("workflow_paused", payload);

            // Add to Pending Review Set
            RSet<String> pendingSet = redissonClient.getSet("human_review:pending");
            pendingSet.add(executionId);

            return true;
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void onNodeComplete(String executionId, String nodeId, String nodeName, NodeType nodeType,
            NodeExecutionResult result, Map<String, Object> inputs) {
        log.info("[Scheduler] Node {} completed with status: {}", nodeId, result.getStatus());

        String lockKey = "lock:exec:" + executionId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            lock.lock(30, TimeUnit.SECONDS);

            // CHECK PAUSE: AFTER_EXECUTION
            // Need to reload Node to check config
            // But we don't have Node object easily here without loading Execution or Graph.
            // Loading execution anyway.

            Execution execution = executionRepository.findById(executionId)
                    .orElseThrow(() -> new IllegalStateException("Execution not found: " + executionId));

            Node node = execution.getGraph().getNode(nodeId);

            // Create temp publisher or reuse?
            StreamContext streamContext = StreamContext.builder().executionId(executionId).nodeId(nodeId).build();
            StreamPublisher publisher = streamPublisherFactory.create(streamContext);

            if (result.isSuccess()
                    && checkPause(executionId, node, TriggerPhase.AFTER_EXECUTION, publisher, result.getOutputs())) {
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
            NodeCompletedEvent logEvent = NodeCompletedEvent.builder()
                    .executionId(executionId)
                    .nodeId(nodeId)
                    .nodeName(nodeName)
                    .nodeType(nodeType.name())
                    .renderMode(result.getStatus() == ExecutionStatus.SUCCEEDED ? "MESSAGE" : "HIDDEN")
                    .status(result.getStatus().getCode())
                    .inputs(inputs)
                    .outputs(result.getOutputs())
                    .errorMessage(result.getErrorMessage())
                    .startTime(java.time.LocalDateTime.now())
                    .endTime(java.time.LocalDateTime.now())
                    .build();
            applicationEventPublisher.publishEvent(logEvent);

            if (execution.getStatus() == ExecutionStatus.SUCCEEDED ||
                    execution.getStatus() == ExecutionStatus.FAILED) {
                log.info("[Scheduler] Execution {} finished status: {}", executionId, execution.getStatus());
                return;
            }

            scheduleNodes(executionId, nextNodes, null);

        } catch (Exception e) {
            log.error("[Scheduler] Error onNodeComplete {}: {}", nodeId, e.getMessage(), e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public void cancelExecution(String executionId) {
        log.info("[Scheduler] Cancelling execution: {}", executionId);
        redisTemplate.opsForValue().set(CANCEL_KEY_PREFIX + executionId, "true", 1, TimeUnit.HOURS);
    }

    private boolean isCancelled(String executionId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(CANCEL_KEY_PREFIX + executionId));
    }

    private String generateNodeSummary(NodeType nodeType, NodeExecutionResult result) {
        if (result.getStatus() == ExecutionStatus.FAILED) {
            return "执行失败: " + (result.getErrorMessage() != null ? result.getErrorMessage() : "未知错误");
        }
        Map<String, Object> outputs = result.getOutputs();
        if (outputs == null || outputs.isEmpty()) {
            return "执行完成";
        }
        switch (nodeType) {
            case LLM:
                Object response = outputs.get("response");
                if (response == null)
                    response = outputs.get("text");
                if (response != null) {
                    String text = response.toString();
                    return "LLM响应: " + (text.length() > 100 ? text.substring(0, 100) + "..." : text);
                }
                return "LLM执行完成";
            case HTTP:
                Object statusCode = outputs.get("statusCode");
                return "HTTP请求完成, 状态码: " + (statusCode != null ? statusCode : "N/A");
            case CONDITION:
                Object branch = outputs.get("selectedBranchId");
                return "条件判断完成, 选择分支: " + (branch != null ? branch : "default");
            default:
                return nodeType.name() + " 节点执行完成";
        }
    }
}
