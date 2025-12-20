package com.zj.aiagemt.service.dag.executor;

import com.zj.aiagemt.common.design.dag.ConditionalDagNode;
import com.zj.aiagemt.common.design.dag.DagNode;
import com.zj.aiagemt.common.design.dag.NodeRouteDecision;
import com.zj.aiagemt.model.entity.AiAgentInstance;
import com.zj.aiagemt.repository.base.AiAgentInstanceMapper;
import com.zj.aiagemt.service.dag.context.DagExecutionContext;
import com.zj.aiagemt.service.dag.exception.NodeConfigException;
import com.zj.aiagemt.service.dag.model.DagGraph;
import com.alibaba.fastjson2.JSON;
import com.zj.aiagemt.service.dag.logging.DagLoggingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DAG执行器
 * 负责执行完整的DAG工作流
 */
@Slf4j
@Service
public class DagExecutor {

    private final DagParallelScheduler scheduler;
    private final AiAgentInstanceMapper agentInstanceMapper;
    private final DagLoggingService loggingService;

    public DagExecutor(AiAgentInstanceMapper agentInstanceMapper,
            DagLoggingService loggingService) {
        this.agentInstanceMapper = agentInstanceMapper;
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

            // 3. 创建工作流实例
            AiAgentInstance agentInstance = createAgentInstance(dagGraph, context);

            // 4. 将 DagGraph 存入 context，供 RouterNode 等节点使用
            context.setValue("__DAG_GRAPH__", dagGraph);

            // 5. 初始化启用的节点集合（用于路由过滤）
            Set<String> enabledNodes = new HashSet<>(dagGraph.getNodes().keySet());

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
                List<DagParallelScheduler.NodeExecutionResult> results = scheduler.executeParallel(levelNodes, context);

                // 检查执行结果
                for (var result : results) {
                    if (!result.isSuccess()) {
                        log.error("节点执行失败: {}", result.getNodeId());

                        // 更新工作流实例状态
                        updateAgentInstance(agentInstance, "FAILED", result.getNodeId(), context);

                        long totalDuration = System.currentTimeMillis() - startTime;
                        return DagExecutionResult.failed(
                                context.getExecutionId(),
                                "Node execution failed: " + result.getNodeId(),
                                result.getException(),
                                totalDuration);
                    }

                    // 检查是否需要人工介入
                    if (result.getResult() != null && result.getResult().startsWith("WAITING_FOR_HUMAN")) {
                        log.info("节点等待人工介入: {}", result.getNodeId());

                        // 更新工作流实例为暂停状态
                        updateAgentInstance(agentInstance, "PAUSED", result.getNodeId(), context);

                        long totalDuration = System.currentTimeMillis() - startTime;
                        return DagExecutionResult.paused(
                                context.getExecutionId(),
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

                // 更新工作流实例当前节点
                if (!filteredNodeIds.isEmpty()) {
                    updateAgentInstance(agentInstance, "RUNNING", filteredNodeIds.get(0), context);
                }
            }

            // 6. 执行完成
            updateAgentInstance(agentInstance, "COMPLETED", null, context);

            long totalDuration = System.currentTimeMillis() - startTime;

            // 记录DAG完成
            loggingService.logDagEnd(context.getExecutionId(), context.getConversationId(),
                    dagGraph.getDagId(), "SUCCESS", totalDuration);

            log.info("DAG执行完成，总耗时: {}ms", totalDuration);

            return DagExecutionResult.success(context.getExecutionId(), totalDuration);

        } catch (Exception e) {
            log.error("DAG执行异常", e);

            long totalDuration = System.currentTimeMillis() - startTime;

            // 记录DAG失败
            loggingService.logDagEnd(context.getExecutionId(), context.getConversationId(),
                    dagGraph != null ? dagGraph.getDagId() : "unknown", "FAILED", totalDuration);

            return DagExecutionResult.failed(
                    context.getExecutionId(),
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
        } else if (routeDecisionObj instanceof com.zj.aiagemt.common.design.dag.NodeRouteDecision) {
            // 如果存储的是 NodeRouteDecision 对象
            com.zj.aiagemt.common.design.dag.NodeRouteDecision decision = (com.zj.aiagemt.common.design.dag.NodeRouteDecision) routeDecisionObj;

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
     * 创建工作流实例
     */
    private AiAgentInstance createAgentInstance(DagGraph dagGraph, DagExecutionContext context) {
        AiAgentInstance instance = new AiAgentInstance();
        // TODO
        instance.setAgentId(1L);
        instance.setConversationId(context.getConversationId());
        instance.setCurrentNodeId(dagGraph.getStartNodeId());
        instance.setStatus("RUNNING");
        instance.setCreateTime(LocalDateTime.now());
        instance.setUpdateTime(LocalDateTime.now());
        instance.setRuntimeContextJson(JSON.toJSONString(context));
        // 保存到数据库
        try {
            agentInstanceMapper.insert(instance);
            log.info("创建智能体实例: id={}, conversationId={}", instance.getId(), instance.getConversationId());
        } catch (Exception e) {
            log.error("创建工作流实例失败", e);
        }

        return instance;
    }

    /**
     * 更新工作流实例
     */
    private void updateAgentInstance(AiAgentInstance instance, String status,
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

        try {
            agentInstanceMapper.updateById(instance);
            log.debug("更新智能体实例: status={}, currentNodeId={}", status, currentNodeId);
        } catch (Exception e) {
            log.error("更新工作流实例失败", e);
        }
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
        private final String status; // SUCCESS, FAILED, PAUSED
        private final String message;
        private final String pausedAtNodeId;
        private final Exception exception;
        private final long durationMs;

        private DagExecutionResult(String executionId, String status, String message,
                String pausedAtNodeId, Exception exception, long durationMs) {
            this.executionId = executionId;
            this.status = status;
            this.message = message;
            this.pausedAtNodeId = pausedAtNodeId;
            this.exception = exception;
            this.durationMs = durationMs;
        }

        public static DagExecutionResult success(String executionId, long durationMs) {
            return new DagExecutionResult(executionId, "SUCCESS", "DAG executed successfully",
                    null, null, durationMs);
        }

        public static DagExecutionResult failed(String executionId, String message,
                Exception exception, long durationMs) {
            return new DagExecutionResult(executionId, "FAILED", message, null, exception, durationMs);
        }

        public static DagExecutionResult paused(String executionId, String pausedAtNodeId, long durationMs) {
            return new DagExecutionResult(executionId, "PAUSED", "Waiting for human intervention",
                    pausedAtNodeId, null, durationMs);
        }

        // Getters
        public String getExecutionId() {
            return executionId;
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
