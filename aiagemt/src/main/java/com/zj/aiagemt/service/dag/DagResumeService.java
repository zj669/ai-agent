package com.zj.aiagemt.service.dag;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.zj.aiagemt.model.entity.AiWorkflowInstance;
import com.zj.aiagemt.repository.base.AiWorkflowInstanceMapper;
import com.zj.aiagemt.service.dag.context.DagExecutionContext;
import com.zj.aiagemt.service.dag.executor.DagExecutor;
import com.zj.aiagemt.service.dag.loader.DagLoaderService;
import com.zj.aiagemt.service.dag.model.DagGraph;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
        log.info("继续执行DAG，从节点 {} 之后开始", instance.getCurrentNodeId());

        // 注意：这里简化处理，实际应该从暂停的节点之后继续
        // 完整实现需要在DagExecutor中支持从特定节点恢复
        DagExecutor.DagExecutionResult result = dagExecutor.execute(dagGraph, context);

        log.info("DAG恢复执行完成: status={}", result.getStatus());

        return result;
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
