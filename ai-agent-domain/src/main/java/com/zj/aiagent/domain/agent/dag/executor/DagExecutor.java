package com.zj.aiagent.domain.agent.dag.executor;

import com.alibaba.fastjson.JSON;
import com.zj.aiagent.domain.agent.dag.context.DagExecutionContext;
import com.zj.aiagent.domain.agent.dag.entity.DagExecutionInstance;
import com.zj.aiagent.domain.agent.dag.entity.DagGraph;
import com.zj.aiagent.domain.agent.dag.logging.DagLoggingService;
import com.zj.aiagent.domain.agent.dag.node.AbstractConfigurableNode;
import com.zj.aiagent.domain.agent.dag.repository.IDagExecutionRepository;
import com.zj.aiagent.domain.agent.dag.repository.IHumanInterventionRepository;
import com.zj.aiagent.shared.design.dag.NodeExecutionResult;
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

    /**
     * 调度策略枚举
     */
    public enum SchedulingStrategy {
        /**
         * 事件驱动调度（推荐）：基于依赖计数触发的细粒度并行
         */
        EVENT_DRIVEN,
        /**
         * 层级并行调度（传统）：按层级分组的粗粒度并行
         */
        LEVEL_BASED
    }

    private final DagParallelScheduler levelBasedScheduler;
    private final DagEventDrivenScheduler eventDrivenScheduler;
    private final DagLoggingService loggingService;

    @Resource
    private IDagExecutionRepository executionRepository;

    @Resource
    private IHumanInterventionRepository humanInterventionRepository;

    /**
     * 当前使用的调度策略（可通过配置文件或环境变量修改）
     */
    private SchedulingStrategy schedulingStrategy = SchedulingStrategy.EVENT_DRIVEN;

    public DagExecutor(DagLoggingService loggingService) {
        this.loggingService = loggingService;
        this.levelBasedScheduler = new DagParallelScheduler(4); // 最多4个并行任务
        this.eventDrivenScheduler = new DagEventDrivenScheduler(4); // 最多4个并行任务
    }

    /**
     * 设置调度策略
     */
    public void setSchedulingStrategy(SchedulingStrategy strategy) {
        this.schedulingStrategy = strategy;
        log.info("调度策略设置为: {}", strategy);
    }

    /**
     * 执行DAG（根据策略选择调度器）
     */
    public DagExecutionResult execute(DagGraph dagGraph, DagExecutionContext context) {
        log.info("开始执行DAG: {}, executionId: {}, 策略: {}",
                dagGraph.getDagId(), context.getExecutionId(), schedulingStrategy);

        // 根据策略选择调度器
        return switch (schedulingStrategy) {
            case EVENT_DRIVEN -> executeWithEventDriven(dagGraph, context);
            case LEVEL_BASED -> executeWithLevelBased(dagGraph, context);
        };
    }

    /**
     * 使用事件驱动调度器执行DAG
     */
    private DagExecutionResult executeWithEventDriven(DagGraph dagGraph, DagExecutionContext context) {
        log.info("使用事件驱动调度器执行DAG: {}", dagGraph.getDagId());

        // 记录DAG开始
        loggingService.logDagStart(context.getExecutionId(), context.getConversationId(), dagGraph.getDagId());

        long startTime = System.currentTimeMillis();

        try {
            // 1. 拓扑排序（检测循环依赖）
            DagTopologicalSorter.sort(dagGraph);

            // 2. 推送 DAG 开始事件
            int totalNodes = dagGraph.getNodes().size();
            pushDagStartEvent(context, dagGraph.getDagId(), totalNodes);

            // 3. 创建执行实例
            DagExecutionInstance instance = createExecutionInstance(dagGraph, context);
            context.setInstanceId(instance.getId());

            // 4. 将 DagGraph 存入上下文
            context.setDagGraph(dagGraph);

            // 5. 使用事件驱动调度器执行
            DagEventDrivenScheduler.SchedulerExecutionResult result = eventDrivenScheduler.execute(dagGraph, context);

            // 6. 处理结果
            long totalDuration = System.currentTimeMillis() - startTime;

            if ("FAILED".equals(result.getStatus())) {
                updateExecutionInstance(instance, "FAILED", null, context);
                loggingService.logDagEnd(context.getExecutionId(), context.getConversationId(),
                        dagGraph.getDagId(), "FAILED", totalDuration);
                pushDagCompleteEvent(context, dagGraph.getDagId(), "failed", totalDuration);

                return DagExecutionResult.failed(
                        context.getExecutionId(),
                        context.getInstanceId(),
                        result.getMessage(),
                        result.getException(),
                        totalDuration);
            }

            if ("PAUSED".equals(result.getStatus())) {
                updateExecutionInstance(instance, "PAUSED", result.getPausedNodeId(), context);
                return DagExecutionResult.paused(
                        context.getExecutionId(),
                        context.getInstanceId(),
                        result.getPausedNodeId(),
                        totalDuration);
            }

            // 成功
            updateExecutionInstance(instance, "COMPLETED", null, context);
            loggingService.logDagEnd(context.getExecutionId(), context.getConversationId(),
                    dagGraph.getDagId(), "SUCCESS", totalDuration);
            pushDagCompleteEvent(context, dagGraph.getDagId(), "success", totalDuration);

            log.info("DAG执行完成（事件驱动），总耗时: {}ms", totalDuration);
            return DagExecutionResult.success(context.getExecutionId(), context.getInstanceId(), totalDuration);

        } catch (Exception e) {
            log.error("DAG执行异常（事件驱动）", e);

            long totalDuration = System.currentTimeMillis() - startTime;
            loggingService.logDagEnd(context.getExecutionId(), context.getConversationId(),
                    dagGraph != null ? dagGraph.getDagId() : "unknown", "FAILED", totalDuration);

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
     * 使用层级并行调度器执行DAG（传统方式）
     */
    private DagExecutionResult executeWithLevelBased(DagGraph dagGraph, DagExecutionContext context) {
        log.info("使用层级并行调度器执行DAG: {}", dagGraph.getDagId());

        // 记录DAG开始
        loggingService.logDagStart(context.getExecutionId(), context.getConversationId(), dagGraph.getDagId());

        long startTime = System.currentTimeMillis();

        try {
            // 1. 拓扑排序
            DagTopologicalSorter.sort(dagGraph);

            // 2. 获取执行层级（用于并行执行）
            List<List<String>> executionLevels = DagTopologicalSorter.getExecutionLevels(dagGraph);

            // 2.5 推送 DAG 开始事件
            int totalNodes = dagGraph.getNodes().size();
            pushDagStartEvent(context, dagGraph.getDagId(), totalNodes);

            // 3. 创建执行实例(DAG聚合内部操作)
            DagExecutionInstance instance = createExecutionInstance(dagGraph, context);
            context.setInstanceId(instance.getId()); // 设置实例ID到上下文

            // 4. 将 DagGraph 存入领域对象，供 RouterNode 等节点使用
            context.setDagGraph(dagGraph);

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

                // 获取本层的节点（统一使用 AbstractConfigurableNode 类型）
                List<AbstractConfigurableNode> levelNodes = filteredNodeIds.stream()
                        .map(dagGraph::getNode)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                // 并行执行本层所有节点
                List<DagParallelScheduler.NodeExecutionResult> results = levelBasedScheduler.executeParallel(
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

                    // 处理路由节点（使用 isRouterNode() 方法代替 instanceof）
                    AbstractConfigurableNode node = dagGraph.getNode(result.getNodeId());
                    if (node != null && node.isRouterNode()) {
                        handleRouterNode(node, context, dagGraph, enabledNodes);
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
     * 处理路由节点
     * 根据路由决策禁用未被选中的节点
     */
    private void handleRouterNode(AbstractConfigurableNode routerNode,
            DagExecutionContext context,
            DagGraph dagGraph,
            Set<String> enabledNodes) {

        String nodeId = routerNode.getNodeId();
        log.info("处理路由节点: {}", nodeId);

        // 从 context 获取路由决策结果
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
        } else if (routeDecisionObj instanceof NodeExecutionResult execResult) {
            // 如果存储的是 NodeExecutionResult 对象
            if (execResult.isRoutingDecision()) {
                NodeRouteDecision decision = execResult.getRouteDecision();
                if (decision.isStopExecution()) {
                    log.info("路由决策: 停止执行");
                    return;
                }
                Set<String> nextNodeIds = decision.getNextNodeIds();
                if (nextNodeIds != null && !nextNodeIds.isEmpty()) {
                    selectedNodeId = nextNodeIds.iterator().next();
                }
            }
        } else if (routeDecisionObj instanceof NodeRouteDecision decision) {
            // 如果存储的是 NodeRouteDecision 对象
            if (decision.isStopExecution()) {
                log.info("路由决策: 停止执行");
                return;
            }
            Set<String> nextNodeIds = decision.getNextNodeIds();
            if (nextNodeIds != null && !nextNodeIds.isEmpty()) {
                selectedNodeId = nextNodeIds.iterator().next();
            }
        }

        if (selectedNodeId == null || selectedNodeId.isEmpty()) {
            log.warn("无法从路由决策中获取选中的节点ID");
            return;
        }

        log.info("路由决策: 选择节点 {}", selectedNodeId);

        // 获取所有候选节点
        Set<String> candidateNodes = routerNode.getCandidateNextNodes();
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
        levelBasedScheduler.shutdown();
        eventDrivenScheduler.shutdown();
    }

    /**
     * 获取层级调度器（用于恢复执行）
     */
    public DagParallelScheduler getLevelBasedScheduler() {
        return levelBasedScheduler;
    }

    /**
     * 获取事件驱动调度器
     */
    public DagEventDrivenScheduler getEventDrivenScheduler() {
        return eventDrivenScheduler;
    }

    /**
     * 从暂停状态恢复执行
     *
     * @param dagGraph DAG 图
     * @param context  执行上下文（已包含审核结果）
     * @return 执行结果
     */
    public DagExecutionResult resumeFromPause(DagGraph dagGraph, DagExecutionContext context) {
        log.info("从暂停状态恢复执行: conversationId={}", context.getConversationId());

        long startTime = System.currentTimeMillis();

        try {
            // 1. 从数据库加载执行实例
            DagExecutionInstance instance = executionRepository.findByConversationId(context.getConversationId());
            if (instance == null) {
                throw new RuntimeException("未找到执行实例: " + context.getConversationId());
            }

            // 检查是否确实处于暂停状态
            if (!"PAUSED".equals(instance.getStatus())) {
                log.warn("执行实例状态不是 PAUSED，当前状态: {}", instance.getStatus());
            }

            // 设置 instanceId 到上下文
            context.setInstanceId(instance.getId());

            // 2. 恢复运行时上下文（已执行节点的结果）
            if (instance.getRuntimeContextJson() != null && !instance.getRuntimeContextJson().isEmpty()) {
                try {
                    java.util.Map<String, Object> savedContext = com.alibaba.fastjson.JSON
                            .parseObject(instance.getRuntimeContextJson(), java.util.Map.class);

                    for (java.util.Map.Entry<String, Object> entry : savedContext.entrySet()) {
                        context.setNodeResult(entry.getKey(), entry.getValue());
                    }

                    log.info("已恢复 {} 个节点的执行结果", savedContext.size());
                } catch (Exception e) {
                    log.warn("恢复上下文失败，继续执行", e);
                }
            }

            // 3. 检查审核结果
            if (!context.getHumanInterventionData().isApproved()) {
                // 拒绝 - 终止整个 DAG 执行
                log.warn("人工审核被拒绝，终止 DAG 执行");

                updateExecutionInstance(instance, "FAILED", instance.getCurrentNodeId(), context);

                // 推送终止事件
                pushDagCompleteEvent(context, dagGraph.getDagId(), "rejected",
                        System.currentTimeMillis() - startTime);

                long totalDuration = System.currentTimeMillis() - startTime;
                return DagExecutionResult.failed(
                        context.getExecutionId(),
                        instance.getId(),
                        "人工审核被拒绝",
                        null,
                        totalDuration);
            }

            // 4. 批准 - 继续执行
            log.info("人工审核已批准，继续执行 DAG");

            // 将 DagGraph 存入上下文
            context.setDagGraph(dagGraph);

            // 推送 DAG 恢复事件
            pushDagResumeEvent(context, dagGraph.getDagId(), instance.getCurrentNodeId());

            // 5. 使用当前策略继续执行
            DagExecutionResult result = switch (schedulingStrategy) {
                case EVENT_DRIVEN -> resumeWithEventDriven(dagGraph, context, instance);
                case LEVEL_BASED -> resumeWithLevelBased(dagGraph, context, instance);
            };

            // 6. 恢复执行完成后清理 Redis 快照
            try {
                humanInterventionRepository.deleteContextSnapshot(context.getConversationId());
                log.info("已清理 Redis 快照: conversationId={}", context.getConversationId());
            } catch (Exception e) {
                log.warn("清理 Redis 快照失败，不影响结果", e);
            }

            return result;

        } catch (Exception e) {
            log.error("恢复执行异常", e);

            long totalDuration = System.currentTimeMillis() - startTime;
            pushDagCompleteEvent(context, dagGraph.getDagId(), "failed", totalDuration);

            return DagExecutionResult.failed(
                    context.getExecutionId(),
                    context.getInstanceId(),
                    "恢复执行失败: " + e.getMessage(),
                    e,
                    totalDuration);
        }
    }

    /**
     * 使用事件驱动调度器恢复执行
     */
    private DagExecutionResult resumeWithEventDriven(
            DagGraph dagGraph,
            DagExecutionContext context,
            DagExecutionInstance instance) {

        long startTime = System.currentTimeMillis();

        try {
            // 使用事件驱动调度器执行（会自动跳过已执行节点）
            DagEventDrivenScheduler.SchedulerExecutionResult result = eventDrivenScheduler.execute(dagGraph, context);

            long totalDuration = System.currentTimeMillis() - startTime;

            if ("FAILED".equals(result.getStatus())) {
                updateExecutionInstance(instance, "FAILED", null, context);
                loggingService.logDagEnd(context.getExecutionId(), context.getConversationId(),
                        dagGraph.getDagId(), "FAILED", totalDuration);
                pushDagCompleteEvent(context, dagGraph.getDagId(), "failed", totalDuration);

                return DagExecutionResult.failed(
                        context.getExecutionId(),
                        context.getInstanceId(),
                        result.getMessage(),
                        result.getException(),
                        totalDuration);
            }

            if ("PAUSED".equals(result.getStatus())) {
                updateExecutionInstance(instance, "PAUSED", result.getPausedNodeId(), context);
                return DagExecutionResult.paused(
                        context.getExecutionId(),
                        context.getInstanceId(),
                        result.getPausedNodeId(),
                        totalDuration);
            }

            // 成功
            updateExecutionInstance(instance, "COMPLETED", null, context);
            loggingService.logDagEnd(context.getExecutionId(), context.getConversationId(),
                    dagGraph.getDagId(), "SUCCESS", totalDuration);
            pushDagCompleteEvent(context, dagGraph.getDagId(), "success", totalDuration);

            log.info("DAG 恢复执行完成（事件驱动），总耗时: {}ms", totalDuration);
            return DagExecutionResult.success(context.getExecutionId(), context.getInstanceId(), totalDuration);

        } catch (Exception e) {
            log.error("事件驱动恢复执行异常", e);
            throw e;
        }
    }

    /**
     * 使用层级调度器恢复执行
     */
    private DagExecutionResult resumeWithLevelBased(
            DagGraph dagGraph,
            DagExecutionContext context,
            DagExecutionInstance instance) {

        // 层级调度器恢复执行的实现（与原 executeWithLevelBased 类似，但跳过已执行节点）
        log.info("使用层级调度器恢复执行");

        // 简化实现：直接调用完整执行，依赖节点检查逻辑跳过已执行节点
        return executeWithLevelBased(dagGraph, context);
    }

    /**
     * 推送 DAG 恢复事件
     */
    private void pushDagResumeEvent(DagExecutionContext context, String dagId, String fromNodeId) {
        org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter emitter = context.getEmitter();
        if (emitter == null) {
            return;
        }

        try {
            java.util.Map<String, Object> event = new java.util.HashMap<>();
            event.put("type", "dag_resume");
            event.put("conversationId", context.getConversationId());
            event.put("agentId", String.valueOf(context.getAgentId()));
            event.put("dagId", dagId);
            event.put("fromNodeId", fromNodeId);
            event.put("timestamp", System.currentTimeMillis());

            String message = "data: " + com.alibaba.fastjson.JSON.toJSONString(event) + "\n\n";
            emitter.send(message);

            log.debug("推送 DAG 恢复事件: dagId={}, fromNodeId={}", dagId, fromNodeId);
        } catch (Exception e) {
            log.warn("推送 DAG 恢复事件失败: dagId={}", dagId, e);
        }
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
