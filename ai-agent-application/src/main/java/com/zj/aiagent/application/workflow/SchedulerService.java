package com.zj.aiagent.application.workflow;

import com.zj.aiagent.domain.chat.valobj.RenderConfig;
import com.zj.aiagent.domain.chat.valobj.SseEventPayload;
import com.zj.aiagent.domain.workflow.entity.Execution;
import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.event.NodeCompletedEvent;
import com.zj.aiagent.domain.workflow.port.CheckpointRepository;
import com.zj.aiagent.domain.workflow.port.ExecutionRepository;
import com.zj.aiagent.domain.workflow.port.NodeExecutorStrategy;
import com.zj.aiagent.domain.workflow.valobj.ExecutionContext;
import com.zj.aiagent.domain.workflow.valobj.ExecutionStatus;
import com.zj.aiagent.domain.workflow.valobj.NodeExecutionResult;
import com.zj.aiagent.domain.workflow.valobj.NodeType;
import com.zj.aiagent.infrastructure.workflow.event.RedisSsePublisher;
import com.zj.aiagent.infrastructure.workflow.executor.NodeExecutorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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
    private final RedissonClient redissonClient;
    private final RedisSsePublisher ssePublisher;
    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;

    private static final String CANCEL_KEY_PREFIX = "workflow:cancel:";

    /**
     * 启动工作流执行
     */
    public void startExecution(Execution execution, Map<String, Object> inputs) {
        log.info("[Scheduler] Starting execution: {}", execution.getExecutionId());

        // 1. 启动执行，获取就绪节点
        List<Node> readyNodes = execution.start(inputs);

        // 2. 持久化初始状态
        executionRepository.save(execution);

        // 3. 调度就绪节点
        scheduleNodes(execution.getExecutionId(), readyNodes);
    }

    /**
     * 恢复暂停的执行
     */
    public void resumeExecution(String executionId, String nodeId, Map<String, Object> additionalInputs) {
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

            List<Node> readyNodes = execution.resume(nodeId, additionalInputs);
            executionRepository.update(execution);

            scheduleNodes(executionId, readyNodes);

        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 调度多个节点（并行触发）
     */
    private void scheduleNodes(String executionId, List<Node> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            log.info("[Scheduler] No nodes to schedule for execution: {}", executionId);
            return;
        }

        if (isCancelled(executionId)) {
            log.warn("[Scheduler] Execution {} cancelled, stopping schedule.", executionId);
            return;
        }

        log.info("[Scheduler] Scheduling {} nodes for execution: {}", nodes.size(), executionId);

        for (Node node : nodes) {
            scheduleNode(executionId, node);
        }
    }

    /**
     * 调度单个节点
     */
    private void scheduleNode(String executionId, Node node) {
        log.info("[Scheduler] Dispatching node: {} (type: {})", node.getNodeId(), node.getType());

        // 0. Double check cancellation
        if (isCancelled(executionId)) {
            return;
        }

        // Publish Node Started Event (SSE)
        publishSseEvent(executionId, node, "node_started", "Node Started", false);

        // 1. 检查人工审核
        if (node.requiresHumanReview()) {
            handleHumanReview(executionId, node);
            return;
        }

        // 2. 获取策略
        NodeExecutorStrategy strategy = executorFactory.getStrategy(node.getType());

        // 3. 加载执行上下文，解析输入
        Execution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalStateException("Execution not found"));

        ExecutionContext context = execution.getContext();
        Map<String, Object> resolvedInputs = context.resolveInputs(node.getInputs());

        // 4. 异步执行
        CompletableFuture<NodeExecutionResult> future = strategy.executeAsync(node, resolvedInputs);

        // 5. 注册回调
        future.whenComplete((result, error) -> {
            if (error != null) {
                result = NodeExecutionResult.failed(error.getMessage());
            }
            if (isCancelled(executionId)) {
                log.warn("[Scheduler] Execution {} cancelled, skipping node completion logic.", executionId);
                return;
            }
            // Pass simple values or Enum? onNodeComplete expects strings for metadata
            // usually, or Enums.
            // Let's pass Enum for type safety within service.
            onNodeComplete(executionId, node.getNodeId(), node.getName(), node.getType(), result, resolvedInputs);
        });
    }

    /**
     * 处理人工审核节点
     */
    private void handleHumanReview(String executionId, Node node) {
        log.info("[Scheduler] Node {} requires human review, pausing execution", node.getNodeId());

        String lockKey = "lock:exec:" + executionId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            lock.lock(10, TimeUnit.SECONDS);

            Execution execution = executionRepository.findById(executionId)
                    .orElseThrow(() -> new IllegalStateException("Execution not found"));

            // 暂停执行
            execution.advance(node.getNodeId(), NodeExecutionResult.paused());

            // 保存检查点
            checkpointRepository.save(execution.createCheckpoint(node.getNodeId()));
            executionRepository.update(execution);

            // 发布事件（供 SSE 推送）
            publishSseEvent(executionId, node, "node_paused", "Waiting for Human Review", false);

        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 节点完成回调（核心）
     * 使用分布式锁串行化状态更新
     */
    private void onNodeComplete(String executionId, String nodeId, String nodeName, NodeType nodeType,
            NodeExecutionResult result, Map<String, Object> inputs) {
        log.info("[Scheduler] Node {} completed with status: {}", nodeId, result.getStatus());

        String lockKey = "lock:exec:" + executionId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 1. 加锁（防止并发更新冲突）
            lock.lock(30, TimeUnit.SECONDS);

            // 2. 加载聚合根
            Execution execution = executionRepository.findById(executionId)
                    .orElseThrow(() -> new IllegalStateException("Execution not found: " + executionId));

            // 3. 推进执行（包含状态更新、输出写入、分支剪枝）
            List<Node> nextNodes = execution.advance(nodeId, result);

            // 4. 保存检查点
            checkpointRepository.save(execution.createCheckpoint(nodeId));

            // 5. 持久化（乐观锁）
            executionRepository.update(execution);

            // 6. 发布 Domain Event (Async Audit)
            // Note: ExecutionStatus now has getCode()
            NodeCompletedEvent logEvent = NodeCompletedEvent.builder()
                    .executionId(executionId)
                    .nodeId(nodeId)
                    .nodeName(nodeName)
                    .nodeType(nodeType.name()) // Enum name for String field
                    .renderMode(result.getStatus() == ExecutionStatus.SUCCEEDED ? "MESSAGE" : "HIDDEN")
                    .status(result.getStatus().getCode()) // Use getCode()
                    .inputs(inputs)
                    .outputs(result.getOutputs())
                    .errorMessage(result.getErrorMessage())
                    .startTime(java.time.LocalDateTime.now())
                    .endTime(java.time.LocalDateTime.now())
                    .build();
            applicationEventPublisher.publishEvent(logEvent);

            // 7. 发布 SSE Event
            Node node = new Node();
            node.setNodeId(nodeId);
            node.setType(nodeType); // Pass Enum
            publishSseEvent(executionId, node, "node_completed", "Node Completed", false);

            // 8. 检查执行是否结束
            if (execution.getStatus() == ExecutionStatus.SUCCEEDED ||
                    execution.getStatus() == ExecutionStatus.FAILED) {
                log.info("[Scheduler] Execution {} finished with status: {}", executionId, execution.getStatus());

                publishSseEvent(executionId, node, "execution_completed", "Execution Finished", false);
                return;
            }

            // 9. 调度下一批节点
            scheduleNodes(executionId, nextNodes);

        } catch (Exception e) {
            log.error("[Scheduler] Error processing node completion for {}: {}", nodeId, e.getMessage(), e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 取消执行
     */
    public void cancelExecution(String executionId) {
        log.info("[Scheduler] Cancelling execution: {}", executionId);
        redisTemplate.opsForValue().set(CANCEL_KEY_PREFIX + executionId, "true", 1, TimeUnit.HOURS);
    }

    private boolean isCancelled(String executionId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(CANCEL_KEY_PREFIX + executionId));
    }

    private void publishSseEvent(String executionId, Node node, String type, String content, boolean isThought) {
        String nodeTypeStr = node.getType() != null ? node.getType().name() : "UNKNOWN";
        SseEventPayload payload = SseEventPayload.builder()
                .executionId(executionId)
                .nodeId(node.getNodeId())
                .nodeType(nodeTypeStr)
                .timestamp(System.currentTimeMillis())
                .isThought(isThought)
                .content(content) // Initial content or status message
                .renderConfig(RenderConfig.builder()
                        .mode("MESSAGE") // Default
                        .title(content)
                        .build())
                .build();
        ssePublisher.publish(payload);
    }
}
