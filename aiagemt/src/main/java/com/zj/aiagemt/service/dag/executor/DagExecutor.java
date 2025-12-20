package com.zj.aiagemt.service.dag.executor;

import com.zj.aiagemt.common.design.dag.ConditionalDagNode;
import com.zj.aiagemt.common.design.dag.DagNode;
import com.zj.aiagemt.common.design.dag.NodeRouteDecision;
import com.zj.aiagemt.model.entity.AiWorkflowInstance;
import com.zj.aiagemt.repository.base.AiWorkflowInstanceMapper;
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
    private final AiWorkflowInstanceMapper workflowInstanceMapper;
    private final DagLoggingService loggingService;

    public DagExecutor(AiWorkflowInstanceMapper workflowInstanceMapper,
            DagLoggingService loggingService) {
        this.workflowInstanceMapper = workflowInstanceMapper;
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
            AiWorkflowInstance workflowInstance = createWorkflowInstance(dagGraph, context);

            // 4. 按层级执行
            for (int level = 0; level < executionLevels.size(); level++) {
                List<String> levelNodeIds = executionLevels.get(level);
                log.info("执行第 {} 层，节点数: {}", level + 1, levelNodeIds.size());

                // 获取本层的节点（需要类型转换，因为RouterNode返回ConditionalDagNode）
                List<Object> levelNodes = levelNodeIds.stream()
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
                        updateWorkflowInstance(workflowInstance, "FAILED", result.getNodeId(), context);

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
                        updateWorkflowInstance(workflowInstance, "PAUSED", result.getNodeId(), context);

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
                                context, dagGraph, executionLevels, level);
                    }
                }

                // 更新工作流实例当前节点
                if (!levelNodeIds.isEmpty()) {
                    updateWorkflowInstance(workflowInstance, "RUNNING", levelNodeIds.get(0), context);
                }
            }

            // 5. 执行完成
            updateWorkflowInstance(workflowInstance, "COMPLETED", null, context);

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
     */
    private void handleConditionalNode(ConditionalDagNode<DagExecutionContext> conditionalNode,
            DagExecutionContext context,
            DagGraph dagGraph,
            List<List<String>> executionLevels,
            int currentLevel) {
        // 路由节点的决策已经在execute中完成并存储到context
        // 这里可以根据需要进行额外处理
        log.debug("处理条件节点: {}", conditionalNode.getNodeId());
    }

    /**
     * 创建工作流实例
     */
    private AiWorkflowInstance createWorkflowInstance(DagGraph dagGraph, DagExecutionContext context) {
        AiWorkflowInstance instance = new AiWorkflowInstance();
        // TODO
        instance.setAgentId(1L);
        instance.setConversationId(context.getConversationId());
        instance.setCurrentNodeId(dagGraph.getStartNodeId());
        instance.setStatus("RUNNING");
        instance.setCreateTime(LocalDateTime.now());
        instance.setUpdateTime(LocalDateTime.now());
        instance.setVersionId(1L);
        instance.setRuntimeContextJson(JSON.toJSONString(context));
        // 保存到数据库
        try {
            workflowInstanceMapper.insert(instance);
            log.info("创建工作流实例: id={}, conversationId={}", instance.getId(), instance.getConversationId());
        } catch (Exception e) {
            log.error("创建工作流实例失败", e);
        }

        return instance;
    }

    /**
     * 更新工作流实例
     */
    private void updateWorkflowInstance(AiWorkflowInstance instance, String status,
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
            workflowInstanceMapper.updateById(instance);
            log.debug("更新工作流实例: status={}, currentNodeId={}", status, currentNodeId);
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
