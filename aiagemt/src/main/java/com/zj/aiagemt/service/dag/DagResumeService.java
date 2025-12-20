package com.zj.aiagemt.service.dag;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.zj.aiagemt.model.entity.AiWorkflowInstance;
import com.zj.aiagemt.repository.base.AiWorkflowInstanceMapper;
import com.zj.aiagemt.service.dag.context.DagExecutionContext;
import com.zj.aiagemt.service.dag.executor.DagExecutor;
import com.zj.aiagemt.service.dag.executor.DagParallelScheduler;
import com.zj.aiagemt.service.dag.loader.DagLoaderService;
import com.zj.aiagemt.service.dag.model.DagGraph;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DAG恢复服务
 * 处理人工审核后的DAG恢复执行
 */
@Slf4j
@Service
public class DagResumeService {

    private final AiWorkflowInstanceMapper workflowInstanceMapper;
    private final DagLoaderService dagLoaderService;
    private final DagExecutor dagExecutor;

    public DagResumeService(AiWorkflowInstanceMapper workflowInstanceMapper,
            DagLoaderService dagLoaderService,
            DagExecutor dagExecutor) {
        this.workflowInstanceMapper = workflowInstanceMapper;
        this.dagLoaderService = dagLoaderService;
        this.dagExecutor = dagExecutor;
    }

    /**
     * 恢复DAG执行
     * 
     * @param conversationId       会话ID
     * @param approved             是否批准
     * @param contextModifications 上下文修改
     * @param comments             审核评论
     * @return 执行结果
     */
    public DagExecutor.DagExecutionResult resumeExecution(
            String conversationId,
            Boolean approved,
            Map<String, Object> contextModifications,
            String comments) {

        log.info("恢复DAG执行: conversationId={}, approved={}", conversationId, approved);

        // 1. 查询工作流实例
        AiWorkflowInstance instance = workflowInstanceMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiWorkflowInstance>()
                        .eq(AiWorkflowInstance::getConversationId, conversationId)
                        .eq(AiWorkflowInstance::getStatus, "PAUSED")
                        .orderByDesc(AiWorkflowInstance::getCreateTime)
                        .last("LIMIT 1"));

        if (instance == null) {
            throw new RuntimeException("No paused workflow found for conversationId: " + conversationId);
        }

        // 2. 恢复上下文
        DagExecutionContext context = restoreContext(instance, conversationId);

        // 3. 应用人工审核结果
        applyHumanReview(context, approved, contextModifications, comments);

        // 4. 更新工作流实例状态
        instance.setStatus("RUNNING");
        instance.setUpdateTime(LocalDateTime.now());
        workflowInstanceMapper.updateById(instance);

        // 5. 重新加载DAG
        DagGraph dagGraph = dagLoaderService.loadDagByVersionId(instance.getVersionId());

        // 6. 继续执行DAG（从当前节点的下一个节点开始）
        String pausedNodeId = instance.getCurrentNodeId();
        log.info("继续执行DAG，从节点 {} 之后开始", pausedNodeId);

        DagExecutor.DagExecutionResult result;

        // 如果用户不批准，直接返回失败
        if (Boolean.FALSE.equals(approved)) {
            log.info("用户拒绝继续执行，DAG终止");
            instance.setStatus("REJECTED");
            instance.setUpdateTime(LocalDateTime.now());
            workflowInstanceMapper.updateById(instance);

            result = DagExecutor.DagExecutionResult.failed(
                    context.getExecutionId(),
                    "User rejected at node: " + pausedNodeId + ". Comments: " + comments,
                    null,
                    0L);
        } else {
            // 用户批准，继续执行
            try {
                result = resumeExecutionFromNode(dagGraph, context, pausedNodeId, instance);
            } catch (Exception e) {
                log.error("恢复执行失败", e);
                instance.setStatus("FAILED");
                instance.setUpdateTime(LocalDateTime.now());
                workflowInstanceMapper.updateById(instance);

                result = DagExecutor.DagExecutionResult.failed(
                        context.getExecutionId(),
                        "Resume execution failed: " + e.getMessage(),
                        e,
                        0L);
            }
        }

        log.info("DAG恢复执行完成: status={}", result.getStatus());

        return result;
    }

    /**
     * 从指定节点之后继续执行DAG
     */
    private DagExecutor.DagExecutionResult resumeExecutionFromNode(
            DagGraph dagGraph,
            DagExecutionContext context,
            String pausedNodeId,
            AiWorkflowInstance instance) {

        long startTime = System.currentTimeMillis();

        try {
            // 1. 获取所有执行层级
            List<List<String>> allLevels = com.zj.aiagemt.service.dag.executor.DagTopologicalSorter
                    .getExecutionLevels(dagGraph);

            // 2. 找到暂停节点所在的层级
            int pausedLevel = -1;
            for (int i = 0; i < allLevels.size(); i++) {
                if (allLevels.get(i).contains(pausedNodeId)) {
                    pausedLevel = i;
                    break;
                }
            }

            if (pausedLevel == -1) {
                throw new RuntimeException("Paused node not found in execution levels: " + pausedNodeId);
            }

            log.info("暂停节点 {} 位于第 {} 层", pausedNodeId, pausedLevel + 1);

            // 3. 从暂停节点的下一层开始执行
            for (int level = pausedLevel + 1; level < allLevels.size(); level++) {
                List<String> levelNodeIds = allLevels.get(level);
                log.info("执行第 {} 层，节点数: {}", level + 1, levelNodeIds.size());

                // 获取本层的节点对象
                List<Object> levelNodes = levelNodeIds.stream()
                        .map(dagGraph::getNode)
                        .filter(java.util.Objects::nonNull)
                        .collect(java.util.stream.Collectors.toList());

                // 并行执行本层所有节点
                List<com.zj.aiagemt.service.dag.executor.DagParallelScheduler.NodeExecutionResult> results = dagExecutor
                        .getScheduler().executeParallel(levelNodes, context);

                // 检查执行结果
                for (var result : results) {
                    if (!result.isSuccess()) {
                        log.error("节点执行失败: {}", result.getNodeId());

                        // 更新工作流实例状态
                        instance.setStatus("FAILED");
                        instance.setCurrentNodeId(result.getNodeId());
                        instance.setRuntimeContextJson(
                                com.alibaba.fastjson2.JSON.toJSONString(context.getAllNodeResults()));
                        instance.setUpdateTime(java.time.LocalDateTime.now());
                        workflowInstanceMapper.updateById(instance);

                        long totalDuration = System.currentTimeMillis() - startTime;
                        return DagExecutor.DagExecutionResult.failed(
                                context.getExecutionId(),
                                "Node execution failed: " + result.getNodeId(),
                                result.getException(),
                                totalDuration);
                    }

                    // 检查是否需要再次人工介入
                    if (result.getResult() != null && result.getResult().startsWith("WAITING_FOR_HUMAN")) {
                        log.info("节点再次等待人工介入: {}", result.getNodeId());

                        // 更新工作流实例为暂停状态
                        instance.setStatus("PAUSED");
                        instance.setCurrentNodeId(result.getNodeId());
                        instance.setRuntimeContextJson(
                                com.alibaba.fastjson2.JSON.toJSONString(context.getAllNodeResults()));
                        instance.setUpdateTime(java.time.LocalDateTime.now());
                        workflowInstanceMapper.updateById(instance);

                        long totalDuration = System.currentTimeMillis() - startTime;
                        return DagExecutor.DagExecutionResult.paused(
                                context.getExecutionId(),
                                result.getNodeId(),
                                totalDuration);
                    }
                }

                // 更新工作流实例当前节点
                if (!levelNodeIds.isEmpty()) {
                    instance.setCurrentNodeId(levelNodeIds.get(0));
                    instance.setRuntimeContextJson(
                            com.alibaba.fastjson2.JSON.toJSONString(context.getAllNodeResults()));
                    instance.setUpdateTime(java.time.LocalDateTime.now());
                    workflowInstanceMapper.updateById(instance);
                }
            }

            // 执行完成
            instance.setStatus("COMPLETED");
            instance.setRuntimeContextJson(com.alibaba.fastjson2.JSON.toJSONString(context.getAllNodeResults()));
            instance.setUpdateTime(java.time.LocalDateTime.now());
            workflowInstanceMapper.updateById(instance);

            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("DAG恢复执行完成，总耗时: {}ms", totalDuration);

            return DagExecutor.DagExecutionResult.success(context.getExecutionId(), totalDuration);

        } catch (Exception e) {
            log.error("从节点 {} 恢复执行时发生异常", pausedNodeId, e);
            throw new RuntimeException("Resume execution from node failed: " + e.getMessage(), e);
        }
    }

    /**
     * 恢复执行上下文
     */
    private DagExecutionContext restoreContext(AiWorkflowInstance instance, String conversationId) {
        DagExecutionContext context = new DagExecutionContext(conversationId);

        // 恢复运行时上下文
        if (instance.getRuntimeContextJson() != null && !instance.getRuntimeContextJson().isEmpty()) {
            try {
                Map<String, Object> nodeResults = JSON.parseObject(
                        instance.getRuntimeContextJson(),
                        new TypeReference<Map<String, Object>>() {
                        });

                // 恢复节点结果
                for (Map.Entry<String, Object> entry : nodeResults.entrySet()) {
                    context.setNodeResult(entry.getKey(), entry.getValue());
                }

                log.info("恢复上下文成功，节点结果数: {}", nodeResults.size());
            } catch (Exception e) {
                log.error("恢复上下文失败", e);
            }
        }

        return context;
    }

    /**
     * 应用人工审核结果
     */
    private void applyHumanReview(DagExecutionContext context,
            Boolean approved,
            Map<String, Object> contextModifications,
            String comments) {
        // 设置审核结果
        context.setValue("human_approved", approved);
        context.setValue("human_comments", comments != null ? comments : "");

        // 应用上下文修改
        if (contextModifications != null && !contextModifications.isEmpty()) {
            for (Map.Entry<String, Object> entry : contextModifications.entrySet()) {
                context.setValue(entry.getKey(), entry.getValue());
                log.info("应用上下文修改: {} = {}", entry.getKey(), entry.getValue());
            }
        }

        log.info("人工审核结果已应用: approved={}, modifications={}",
                approved, contextModifications != null ? contextModifications.size() : 0);
    }
}
