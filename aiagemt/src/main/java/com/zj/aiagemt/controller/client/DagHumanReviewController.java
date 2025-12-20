package com.zj.aiagemt.controller.client;

import com.zj.aiagemt.model.common.Response;
import com.zj.aiagemt.model.dto.HumanReviewSubmitRequest;
import com.zj.aiagemt.model.dto.HumanReviewTaskResponse;
import com.zj.aiagemt.model.entity.AiAgentInstance;
import com.zj.aiagemt.repository.base.AiAgentInstanceMapper;
import com.zj.aiagemt.service.dag.DagResumeService;
import com.zj.aiagemt.service.dag.executor.DagExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * DAG人工审核控制器
 */
@Slf4j
@RestController
@RequestMapping("/client/dag/human")
@Tag(name = "DAG人工审核", description = "DAG工作流人工审核相关接口")
public class DagHumanReviewController {

    private final AiAgentInstanceMapper agentInstanceMapper;
    private final DagResumeService dagResumeService;

    public DagHumanReviewController(AiAgentInstanceMapper agentInstanceMapper,
            DagResumeService dagResumeService) {
        this.agentInstanceMapper = agentInstanceMapper;
        this.dagResumeService = dagResumeService;
    }

    /**
     * 查询待处理的人工审核任务
     */
    @GetMapping("/tasks/{conversationId}")
    @Operation(summary = "查询待处理的人工审核任务")
    public Response<HumanReviewTaskResponse> getHumanTask(@PathVariable String conversationId) {
        try {
            log.info("查询人工审核任务: conversationId={}", conversationId);

            // 查询暂停的工作流实例
            AiAgentInstance instance = agentInstanceMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiAgentInstance>()
                            .eq(AiAgentInstance::getConversationId, conversationId)
                            .eq(AiAgentInstance::getStatus, "PAUSED")
                            .orderByDesc(AiAgentInstance::getCreateTime)
                            .last("LIMIT 1"));

            if (instance == null) {
                return Response.fail("未找到待处理的人工审核任务");
            }

            // 构建响应
            HumanReviewTaskResponse response = HumanReviewTaskResponse.builder()
                    .conversationId(conversationId)
                    .nodeId(instance.getCurrentNodeId())
                    .checkMessage("请审核任务执行结果")
                    .createTime(instance.getCreateTime().toInstant(java.time.ZoneOffset.of("+8")).toEpochMilli())
                    .status("WAITING")
                    .build();

            return Response.success(response);

        } catch (Exception e) {
            log.error("查询人工审核任务失败", e);
            return Response.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 提交人工审核结果
     */
    @PostMapping("/tasks/{conversationId}/submit")
    @Operation(summary = "提交人工审核结果")
    public Response<String> submitHumanReview(
            @PathVariable String conversationId,
            @RequestBody HumanReviewSubmitRequest request) {
        try {
            log.info("提交人工审核结果: conversationId={}, approved={}",
                    conversationId, request.getApproved());

            // 验证请求
            if (request.getApproved() == null) {
                return Response.fail("approved字段不能为空");
            }

            // 触发DAG恢复执行
            DagExecutor.DagExecutionResult result = dagResumeService.resumeExecution(
                    conversationId,
                    request.getApproved(),
                    request.getContextModifications(),
                    request.getComments());

            // 返回执行结果
            String message = String.format("审核结果已提交，DAG执行状态: %s, 耗时: %dms",
                    result.getStatus(), result.getDurationMs());

            return Response.success(message);

        } catch (Exception e) {
            log.error("提交人工审核结果失败", e);
            return Response.fail("提交失败: " + e.getMessage());
        }
    }

    /**
     * 获取工作流实例状态
     */
    @GetMapping("/status/{conversationId}")
    @Operation(summary = "获取工作流实例状态")
    public Response<AiAgentInstance> getWorkflowStatus(@PathVariable String conversationId) {
        try {
            AiAgentInstance instance = agentInstanceMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiAgentInstance>()
                            .eq(AiAgentInstance::getConversationId, conversationId)
                            .orderByDesc(AiAgentInstance::getCreateTime)
                            .last("LIMIT 1"));

            if (instance == null) {
                return Response.fail("未找到工作流实例");
            }

            return Response.success(instance);

        } catch (Exception e) {
            log.error("查询工作流状态失败", e);
            return Response.fail("查询失败: " + e.getMessage());
        }
    }
}
