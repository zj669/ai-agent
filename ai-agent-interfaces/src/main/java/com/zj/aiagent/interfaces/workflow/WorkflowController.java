package com.zj.aiagent.interfaces.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.application.workflow.SchedulerService;
import com.zj.aiagent.domain.workflow.entity.Execution;
import com.zj.aiagent.domain.workflow.entity.WorkflowGraph;
import com.zj.aiagent.domain.workflow.port.ExecutionRepository;
import com.zj.aiagent.domain.workflow.valobj.ExecutionStatus;
import com.zj.aiagent.infrastructure.workflow.event.RedisSseListener;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 工作流执行控制器
 * 提供执行启动(SSE流式)、取消、历史查询等接口
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow/execution")
@RequiredArgsConstructor
public class WorkflowController {

    private final SchedulerService schedulerService;
    private final ExecutionRepository executionRepository;
    private final com.zj.aiagent.domain.workflow.port.WorkflowNodeExecutionLogRepository workflowNodeExecutionLogRepository;
    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final ObjectMapper objectMapper;

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutes
    private static final ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(10);

    /**
     * 启动工作流执行 (Direct POST Streaming)
     * POST 请求建立连接并直接返回 SSE 流
     */
    @PostMapping(value = "/start", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startExecution(@RequestBody StartExecutionRequest request) {
        log.info("[API] Starting execution stream for agent: {}", request.getAgentId());

        String executionId = UUID.randomUUID().toString();
        // 1. 创建 SSE Emitter
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 2. 创建并注册 Redis 监听器
        String channel = "workflow:channel:" + executionId;
        RedisSseListener listener = new RedisSseListener(objectMapper, payload -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("message") // 或者 payload.getNodeType()
                        .data(payload)); // 直接发送 Payload 对象，SpringMVC 会自动序列化
            } catch (IOException e) {
                log.error("[SSE] Error sending event to emitter: {}", e.getMessage());
                emitter.completeWithError(e);
            }
        });

        redisMessageListenerContainer.addMessageListener(listener, new ChannelTopic(channel));
        log.info("[SSE] Subscribed to Redis channel: {}", channel);

        // 3. 心跳任务 (每 15s 发送 ping)
        var heartbeatTask = heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().name("ping").data("pong"));
            } catch (Exception e) {
                // Ignore heartbeat errors, emitter might be closed
            }
        }, 15, 15, TimeUnit.SECONDS);

        // 4. 清理逻辑
        Runnable cleanUp = () -> {
            log.info("[SSE] Cleaning up resources for execution: {}", executionId);
            redisMessageListenerContainer.removeMessageListener(listener);
            heartbeatTask.cancel(true);
        };
        emitter.onCompletion(cleanUp);
        emitter.onTimeout(cleanUp);
        emitter.onError((e) -> cleanUp.run());

        // 5. 异步启动调度
        CompletableFuture.runAsync(() -> {
            try {
                Execution execution = Execution.builder()
                        .executionId(executionId)
                        .agentId(request.getAgentId())
                        .userId(request.getUserId())
                        .conversationId(request.getConversationId())
                        .graph(request.getGraph())
                        .status(ExecutionStatus.PENDING)
                        .build();

                // 发送初始连接成功事件
                emitter.send(SseEmitter.event().name("connected").data(Map.of("executionId", executionId)));

                schedulerService.startExecution(execution, request.getInputs());
            } catch (Exception e) {
                log.error("[API] Failed to start execution: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(Map.of("message", e.getMessage())));
                    emitter.completeWithError(e);
                } catch (IOException ex) {
                    // ignore
                }
            }
        });

        return emitter;
    }

    /**
     * 停止/取消执行
     */
    @PostMapping("/stop")
    public ResponseEntity<Void> stopExecution(@RequestBody StopExecutionRequest request) {
        log.info("[API] Stopping execution: {}", request.getExecutionId());
        schedulerService.cancelExecution(request.getExecutionId());
        return ResponseEntity.ok().build();
    }

    /**
     * 获取执行详情 (Debug)
     */
    @GetMapping("/{executionId}")
    public ResponseEntity<Execution> getExecution(@PathVariable String executionId) {
        return executionRepository.findById(executionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取节点执行日志 (Debug)
     */
    @GetMapping("/{executionId}/node/{nodeId}")
    public ResponseEntity<com.zj.aiagent.domain.workflow.entity.WorkflowNodeExecutionLog> getNodeExecutionLog(
            @PathVariable String executionId,
            @PathVariable String nodeId) {
        return java.util.Optional
                .ofNullable(workflowNodeExecutionLogRepository.findByExecutionIdAndNodeId(executionId, nodeId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取会话执行历史
     */
    @GetMapping("/history/{conversationId}")
    public ResponseEntity<java.util.List<Execution>> getHistory(@PathVariable String conversationId) {
        java.util.List<Execution> history = executionRepository.findByConversationId(conversationId);
        return ResponseEntity.ok(history);
    }

    // --- DTOs ---

    @Data
    public static class StartExecutionRequest {
        private Long agentId;
        private Long userId;
        private String conversationId;
        private WorkflowGraph graph;
        private Map<String, Object> inputs;
    }

    @Data
    public static class StopExecutionRequest {
        private String executionId;
    }
}
