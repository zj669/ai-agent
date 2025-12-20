package com.zj.aiagemt.controller.client;

import com.zj.aiagemt.model.common.Response;
import com.zj.aiagemt.service.dag.logging.DagExecutionLog;
import com.zj.aiagemt.service.dag.logging.DagLoggingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * DAG日志控制器
 */
@Slf4j
@RestController
@RequestMapping("/client/dag/logs")
@Tag(name = "DAG日志", description = "DAG执行日志查询接口")
public class DagLoggingController {

    private final DagLoggingService loggingService;

    public DagLoggingController(DagLoggingService loggingService) {
        this.loggingService = loggingService;
    }

    /**
     * 获取执行日志
     */
    @GetMapping("/execution/{executionId}")
    @Operation(summary = "获取指定执行的所有日志")
    public Response<List<DagExecutionLog>> getExecutionLogs(@PathVariable String executionId) {
        try {
            List<DagExecutionLog> logs = loggingService.getExecutionLogs(executionId);
            return Response.success(logs);
        } catch (Exception e) {
            log.error("获取执行日志失败", e);
            return Response.fail("获取失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话日志
     */
    @GetMapping("/conversation/{conversationId}")
    @Operation(summary = "获取指定会话的所有日志")
    public Response<List<DagExecutionLog>> getConversationLogs(@PathVariable String conversationId) {
        try {
            List<DagExecutionLog> logs = loggingService.getConversationLogs(conversationId);
            return Response.success(logs);
        } catch (Exception e) {
            log.error("获取会话日志失败", e);
            return Response.fail("获取失败: " + e.getMessage());
        }
    }
}
