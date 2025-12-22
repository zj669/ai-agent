package com.zj.aiagent.domain.agent.dag.executor;

import com.alibaba.fastjson.JSON;
import com.zj.aiagent.domain.agent.dag.context.DagExecutionContext;
import com.zj.aiagent.domain.agent.dag.entity.DagGraph;
import com.zj.aiagent.domain.agent.dag.entity.DagExecutionInstance;
import com.zj.aiagent.domain.agent.dag.logging.DagLoggingService;
import com.zj.aiagent.domain.agent.dag.repository.IDagExecutionRepository;
import com.zj.aiagent.shared.design.dag.ConditionalDagNode;
import com.zj.aiagent.shared.design.dag.NodeRouteDecision;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DAG执行器
 * 负责执行完整的DAG工作流
 */
@Slf4j
@Service
public class DagExecutor {

    private final DagParallelScheduler scheduler;
    private final DagLoggingService loggingService;

    @Resource
    private IDagExecutionRepository executionRepository;

    public DagExecutor(
            DagLoggingService loggingService) {
        this.loggingService = loggingService;
        this.scheduler = new DagParallelScheduler(4, loggingService); // 最多4个并行任务
    }

    /**
     * 执行DAG
     */
    public DagExecutionResult execute(DagGraph dagGraph, DagExecutionContext context) {
        log.info("开始执行DAG: {}, executionId: {}", dagGraph.getDagId(), context.getExecutionId());

        // 记录DAG开始
        loggingService.logDagStart(context.getExecutionId(), context.getConversationId(), dagGraph.getDagId());

        long startTime = System.currentTimeMillis();

        try {
            // 1. 拓扑排序
            List<String> sortedNodes = DagTopologicalSorter.sort(dagGraph);

            // 2. 获取执行层级（用于并行执行）
            List<List<String>> executionLevels = DagTopologicalSorter.getExecutionLevels(dagGraph);

            // 2.5 推送 DAG 开始事件
            int totalNodes = dagGraph.getNodes().size();
            pushDagStartEvent(context, dagGraph.getDagId(), totalNodes);

            // 3. 创建执行实例(DAG聚合内部操作)
            DagExecutionInstance instance = createExecutionInstance(dagGraph, context);
            context.setInstanceId(instance.getId()); // 设置实例ID到上下文

            // 4. 将 DagGraph 存入 context，供 RouterNode 等节点使用
            context.setValue("__DAG_GRAPH__", dagGraph);

            // 5. 初始化启用的节点集合（用于路由过滤）
            Set<String> enabledNodes = new HashSet<>(dagGraph.getNodes().keySet());

            // 5.5 初始化进度跟踪
            int completedNodes = 0;

            // 6. 按层级执行
            for (int level = 0; level < executionLevels.size(); level++) {
                List<String> levelNodeIds = executionLevels.get(level);

                // 根据路由决策过滤节点
                List<String> filteredNodeIds = levelNodeIds.stream()
                        .filter(enabledNodes::contains)
                        .collect(Collectors.toList());

                if (filteredNodeIds.isEmpty()) {
                    log.info("第 {} 层所有节点已被路由过滤，跳过", level + 1);
                    continue;
                }

                log.info("执行第 {} 层，原始节点数: {}, 过滤后节点数: {}",
                        level + 1, levelNodeIds.size(), filteredNodeIds.size());

                // 获取本层的节点（需要类型转换，因为RouterNode返回ConditionalDagNode）
                List<Object> levelNodes = filteredNodeIds.stream()
                        .map(dagGraph::getNode)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                // 并行执行本层所有节点
                List<DagParallelScheduler.NodeExecutionResult> results = scheduler.executeParallel(
                        levelNodes, context, completedNodes, totalNodes);

                // 检查执行结果
                for (var result : results) {
                    if (!result.isSuccess()) {
                        log.error("节点执行失败: {}", result.getNodeId());

                        // 更新执行实例状态
                        updateExecutionInstance(instance, "FAILED", result.getNodeId(), context);

                        long totalDuration = System.currentTimeMillis() - startTime;
                        return DagExecutionResult.failed(
                                context.getExecutionId(),
                                context.getInstanceId(),
                                "Node execution failed: " + result.getNodeId(),
                                result.getException(),
                                totalDuration);
                    }

                    // 检查是否需要人工介入
                    if (result.getResult() != null && result.getResult().startsWith("WAITING_FOR_HUMAN")) {
                        log.info("节点等待人工介入: {}", result.getNodeId());

                        // 更新执行实例为暂停状态
                        updateExecutionInstance(instance, "PAUSED", result.getNodeId(), context);

                        long totalDuration = System.currentTimeMillis() - startTime;
                        return DagExecutionResult.paused(
                                context.getExecutionId(),
                                context.getInstanceId(),
                                result.getNodeId(),
                                totalDuration);
                    }

                    // 处理路由节点
                    Object nodeObj = dagGraph.getNode(result.getNodeId());
                    if (nodeObj instanceof ConditionalDagNode) {
                        handleConditionalNode((ConditionalDagNode<DagExecutionContext>) nodeObj,
                                context, dagGraph, enabledNodes);
                    }
                }

                // 更新执行实例当前节点
                if (!filteredNodeIds.isEmpty()) {
                    updateExecutionInstance(instance, "RUNNING", filteredNodeIds.get(0), context);
                }

                // 更新已完成节点数
                completedNodes += filteredNodeIds.size();
            }

            // 6. 执行完成
            updateExecutionInstance(instance, "COMPLETED", null, context);

            long totalDuration = System.currentTimeMillis() - startTime;

            // 记录DAG完成
            loggingService.logDagEnd(context.getExecutionId(), context.getConversationId(),
                    dagGraph.getDagId(), "SUCCESS", totalDuration);

            // 推送 DAG 完成事件
            pushDagCompleteEvent(context, dagGraph.getDagId(), "success", totalDuration);

            log.info("DAG执行完成，总耗时: {}ms", totalDuration);

            return DagExecutionResult.success(context.getExecutionId(), context.getInstanceId(), totalDuration);

        } catch (Exception e) {
            log.error("DAG执行异常", e);

            long totalDuration = System.currentTimeMillis() - startTime;

            // 记录DAG失败
            loggingService.logDagEnd(context.getExecutionId(), context.getConversationId(),
                    dagGraph != null ? dagGraph.getDagId() : "unknown", "FAILED", totalDuration);

            // 推送 DAG 失败事件
            if (dagGraph != null) {
                pushDagCompleteEvent(context, dagGraph.getDagId(), "failed", totalDuration);
            }

            return DagExecutionResult.failed(
                    context.getExecutionId(),
                    context.getInstanceId(),
                    "DAG execution error: " + e.getMessage(),
                    e,
                    totalDuration);
        }
    }

    /**
     * 处理条件节点（路由节点）
     * 根据路由决策禁用未被选中的节点
     */
    private void handleConditionalNode(ConditionalDagNode<DagExecutionContext> conditionalNode,
            DagExecutionContext context,
            DagGraph dagGraph,
            Set<String> enabledNodes) {

        String nodeId = conditionalNode.getNodeId();
        log.info("处理路由节点: {}", nodeId);

        // 从context获取路由决策结果
        Object routeDecisionObj = context.getNodeResult(nodeId);
        if (routeDecisionObj == null) {
            log.warn("路由节点 {} 没有决策结果", nodeId);
            return;
        }

        // 解析路由决策
        String selectedNodeId = null;
        if (routeDecisionObj instanceof String) {
            // 如果直接存储的是字符串（节点ID）
            selectedNodeId = (String) routeDecisionObj;
        } else if (routeDecisionObj instanceof NodeRouteDecision decision) {
            // 如果存储的是 NodeRouteDecision 对象

            if (decision.isStopExecution()) {
                log.info("路由决策: 停止执行");
                return;
            }

            // 获取选中的节点ID集合
            Set<String> nextNodeIds = decision.getNextNodeIds();
            if (nextNodeIds != null && !nextNodeIds.isEmpty()) {
                selectedNodeId = nextNodeIds.iterator().next(); // 取第一个
            }
        }

        if (selectedNodeId == null || selectedNodeId.isEmpty()) {
            log.warn("无法从路由决策中获取选中的节点ID");
            return;
        }

        log.info("路由决策: 选择节点 {}", selectedNodeId);

        // 获取所有候选节点
        Set<String> candidateNodes = conditionalNode.getCandidateNextNodes();
        if (candidateNodes == null || candidateNodes.isEmpty()) {
            log.warn("路由节点 {} 没有候选节点", nodeId);
            return;
        }

        // 禁用未被选中的候选节点
        for (String candidateNodeId : candidateNodes) {
            if (!candidateNodeId.equals(selectedNodeId)) {
                enabledNodes.remove(candidateNodeId);
                log.info("禁用未选中的节点: {}", candidateNodeId);

                // 递归禁用该节点的所有下游节点
                disableDownstreamNodes(candidateNodeId, dagGraph, enabledNodes);
            }
        }
    }

    /**
     * 递归禁用节点的所有下游节点
     */
    private void disableDownstreamNodes(String nodeId, DagGraph dagGraph, Set<String> enabledNodes) {
        // 遍历所有边，找到以nodeId为source的边
        for (var edge : dagGraph.getEdges()) {
            if (edge.getSource().equals(nodeId)) {
                String downstreamNodeId = edge.getTarget();
                if (enabledNodes.contains(downstreamNodeId)) {
                    enabledNodes.remove(downstreamNodeId);
                    log.info("递归禁用下游节点: {}", downstreamNodeId);
                    // 继续递归
                    disableDownstreamNodes(downstreamNodeId, dagGraph, enabledNodes);
                }
            }
        }
    }

    /**
     * 推送 DAG 开始事件
     */
    private void pushDagStartEvent(DagExecutionContext context, String dagId, int totalNodes) {
        org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter emitter = context.getEmitter();
        if (emitter == null) {
            return;
        }

        try {
            java.util.Map<String, Object> event = new java.util.HashMap<>();
            event.put("type", "dag_start");
            event.put("conversationId", context.getConversationId());
            event.put("agentId", String.valueOf(context.getAgentId()));
            event.put("dagId", dagId);
            event.put("totalNodes", totalNodes);
            event.put("timestamp", System.currentTimeMillis());

            String message = "data: " + com.alibaba.fastjson.JSON.toJSONString(event) + "\n\n";
            emitter.send(message);

            log.debug("推送 DAG 开始事件: dagId={}", dagId);
        } catch (Exception e) {
            log.warn("推送 DAG 开始事件失败: dagId={}", dagId, e);
        }
    }

    /**
     * 推送 DAG 完成事件
     */
    private void pushDagCompleteEvent(DagExecutionContext context, String dagId,
            String status, long durationMs) {
        org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter emitter = context.getEmitter();
        if (emitter == null) {
            return;
        }

        try {
            java.util.Map<String, Object> event = new java.util.HashMap<>();
            event.put("type", "dag_complete");
            event.put("conversationId", context.getConversationId());
            event.put("agentId", String.valueOf(context.getAgentId()));
            event.put("dagId", dagId);
            event.put("status", status); // "success" | "failed"
            event.put("durationMs", durationMs);
            event.put("timestamp", System.currentTimeMillis());

            String message = "data: " + com.alibaba.fastjson.JSON.toJSONString(event) + "\n\n";
            emitter.send(message);

            log.debug("推送 DAG 完成事件: dagId={}, status={}", dagId, status);
        } catch (Exception e) {
            log.warn("推送 DAG 完成事件失败: dagId={}", dagId, e);
        }
    }

    /**
     * 查找或创建DAG执行实例
     * 如果同一个conversationId已存在实例，则更新该实例；否则创建新实例
     */
    private DagExecutionInstance createExecutionInstance(DagGraph dagGraph, DagExecutionContext context) {
        // 先根据conversationId查找已有实例
        DagExecutionInstance instance = executionRepository.findByConversationId(context.getConversationId());

        if (instance != null) {
            // 如果已存在，则更新现有实例
            log.info("找到已存在的执行实例，将更新实例ID: {}, conversationId: {}",
                    instance.getId(), context.getConversationId());
            instance.setCurrentNodeId(dagGraph.getStartNodeId());
            instance.setStatus("RUNNING");
            instance.setUpdateTime(LocalDateTime.now());
            instance.setRuntimeContextJson(JSON.toJSONString(context.getAllNodeResults()));
            executionRepository.update(instance);
        } else {
            // 如果不存在，则创建新实例
            log.info("未找到已存在的执行实例，创建新实例。conversationId: {}", context.getConversationId());
            instance = DagExecutionInstance.builder()
                    .agentId(context.getAgentId())
                    .conversationId(context.getConversationId())
                    .currentNodeId(dagGraph.getStartNodeId())
                    .status("RUNNING")
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .runtimeContextJson(JSON.toJSONString(context.getAllNodeResults()))
                    .build();
            instance = executionRepository.save(instance);
        }

        return instance;
    }

    /**
     * 更新DAG执行实例
     */
    private void updateExecutionInstance(DagExecutionInstance instance, String status,
            String currentNodeId, DagExecutionContext context) {
        if (instance == null || instance.getId() == null) {
            return;
        }

        instance.setStatus(status);
        if (currentNodeId != null) {
            instance.setCurrentNodeId(currentNodeId);
        }
        instance.setRuntimeContextJson(JSON.toJSONString(context.getAllNodeResults()));
        instance.setUpdateTime(LocalDateTime.now());

        executionRepository.update(instance);
    }

    /**
     * 关闭执行器
     */
    public void shutdown() {
        scheduler.shutdown();
    }

    /**
     * 获取调度器（用于恢复执行）
     */
    public DagParallelScheduler getScheduler() {
        return scheduler;
    }

    /**
     * DAG执行结果
     */
    public static class DagExecutionResult {
        private final String executionId;
        private final Long instanceId; // 新增
        private final String status; // SUCCESS, FAILED, PAUSED
        private final String message;
        private final String pausedAtNodeId;
        private final Exception exception;
        private final long durationMs;

        private DagExecutionResult(String executionId, Long instanceId, String status, String message,
                String pausedAtNodeId, Exception exception, long durationMs) {
            this.executionId = executionId;
            this.instanceId = instanceId;
            this.status = status;
            this.message = message;
            this.pausedAtNodeId = pausedAtNodeId;
            this.exception = exception;
            this.durationMs = durationMs;
        }

        public static DagExecutionResult success(String executionId, Long instanceId, long durationMs) {
            return new DagExecutionResult(executionId, instanceId, "SUCCESS", "DAG executed successfully",
                    null, null, durationMs);
        }

        public static DagExecutionResult failed(String executionId, Long instanceId, String message,
                Exception exception, long durationMs) {
            return new DagExecutionResult(executionId, instanceId, "FAILED", message, null, exception, durationMs);
        }

        public static DagExecutionResult paused(String executionId, Long instanceId, String pausedAtNodeId,
                long durationMs) {
            return new DagExecutionResult(executionId, instanceId, "PAUSED", "Waiting for human intervention",
                    pausedAtNodeId, null, durationMs);
        }

        // Getters
        public String getExecutionId() {
            return executionId;
        }

        public Long getInstanceId() {
            return instanceId;
        }

        public String getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public String getPausedAtNodeId() {
            return pausedAtNodeId;
        }

        public Exception getException() {
            return exception;
        }

        public long getDurationMs() {
            return durationMs;
        }
    }
}
