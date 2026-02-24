package com.zj.aiagent.interfaces.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.application.chat.ChatApplicationService;
import com.zj.aiagent.application.workflow.SchedulerService;
import com.zj.aiagent.domain.agent.entity.Agent;
import com.zj.aiagent.domain.agent.repository.AgentRepository;
import com.zj.aiagent.domain.chat.entity.Conversation;
import com.zj.aiagent.domain.chat.entity.Message;
import com.zj.aiagent.domain.workflow.valobj.ExecutionMode;
import com.zj.aiagent.infrastructure.workflow.event.RedisSseListener;
import com.zj.aiagent.interfaces.chat.dto.ConversationResponse;
import com.zj.aiagent.interfaces.chat.dto.MessageResponse;
import com.zj.aiagent.shared.context.UserContext;
import com.zj.aiagent.shared.response.PageResult;
import com.zj.aiagent.shared.response.Response;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 聊天接口
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatApplicationService chatApplicationService;
    private final SchedulerService schedulerService;
    private final AgentRepository agentRepository;
    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final ObjectMapper objectMapper;

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;
    private static final ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(2);

    /**
     * 创建会话
     */
    @PostMapping("/conversations")
    public Response<String> createConversation(@RequestParam String agentId) {
        Long userId = UserContext.getUserId();
        return Response.success(chatApplicationService.createConversation(String.valueOf(userId), agentId));
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/conversations")
    public Response<PageResult<ConversationResponse>> getConversations(
            @RequestParam String agentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        String userId = String.valueOf(UserContext.getUserId());
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        PageResult<Conversation> result = chatApplicationService.getConversationHistory(userId, agentId, pageable);

        List<ConversationResponse> list = result.getList().stream()
                .map(ConversationResponse::from)
                .collect(Collectors.toList());

        return Response.success(new PageResult<>(result.getTotal(), result.getPages(), list));
    }

    /**
     * 获取会话消息历史
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public Response<List<MessageResponse>> getMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "asc") String order) {

        String userId = String.valueOf(UserContext.getUserId());
        Sort.Direction direction = "desc".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(direction, "createdAt"));

        List<Message> messages = chatApplicationService.getMessagesWithAuth(conversationId, userId, pageable);
        return Response.success(messages.stream()
                .map(MessageResponse::from)
                .collect(Collectors.toList()));
    }

    /**
     * 发送消息并获取流式响应
     * 查询会话关联的 agentId，根据 agent 的已发布工作流启动执行
     */
    @PostMapping(value = "/conversations/{conversationId}/messages",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessage(
            @PathVariable String conversationId,
            @RequestBody SendMessageRequest request) {

        Long userId = UserContext.getUserId();

        // 1. 查询会话获取 agentId
        Conversation conversation = chatApplicationService
                .getConversationById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));

        if (!conversation.getUserId().equals(String.valueOf(userId))) {
            throw new IllegalArgumentException("No permission to access conversation");
        }

        Long agentId = Long.parseLong(conversation.getAgentId());

        // 2. 验证 agent 已发布
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));
        if (agent.getPublishedVersionId() == null) {
            throw new IllegalStateException("Agent has no published version");
        }

        // 3. 创建 SSE Emitter
        String executionId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        String channel = "workflow:channel:" + executionId;
        RedisSseListener listener = new RedisSseListener(objectMapper, payload -> {
            try {
                String eventName = payload.getEventType() != null
                        ? payload.getEventType().name().toLowerCase() : "message";
                emitter.send(SseEmitter.event().name(eventName).data(payload));
                
                // 当收到最终 FINISH 事件时，关闭 SSE 流
                if ("finish".equals(eventName) && payload.getStatus() != null &&
                    (payload.getStatus() == com.zj.aiagent.domain.workflow.valobj.ExecutionStatus.SUCCEEDED ||
                     payload.getStatus() == com.zj.aiagent.domain.workflow.valobj.ExecutionStatus.FAILED)) {
                    String nodeType = payload.getNodeType();
                    if ("END".equals(nodeType) || nodeType == null) {
                        emitter.complete();
                    }
                }
            } catch (IOException e) {
                log.error("[Chat SSE] Error sending event: {}", e.getMessage());
                emitter.completeWithError(e);
            }
        });

        redisMessageListenerContainer.addMessageListener(listener, new ChannelTopic(channel));

        var heartbeatTask = heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().name("ping").data("pong"));
            } catch (Exception e) {
                // ignore
            }
        }, 15, 15, TimeUnit.SECONDS);

        Runnable cleanUp = () -> {
            redisMessageListenerContainer.removeMessageListener(listener);
            heartbeatTask.cancel(true);
        };
        emitter.onCompletion(cleanUp);
        emitter.onTimeout(cleanUp);
        emitter.onError(e -> cleanUp.run());

        // 4. 异步启动工作流
        CompletableFuture.runAsync(() -> {
            try {
                emitter.send(SseEmitter.event().name("connected")
                        .data(Map.of("executionId", executionId)));

                schedulerService.startExecution(
                        executionId, agentId, userId, conversationId,
                        null, // 使用已发布版本
                        Map.of("input", request.getContent()),
                        ExecutionMode.STANDARD);
            } catch (Exception e) {
                log.error("[Chat] Failed to start execution: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data(Map.of("message", e.getMessage())));
                    emitter.completeWithError(e);
                } catch (IOException ex) {
                    // ignore
                }
            }
        });

        return emitter;
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/conversations/{conversationId}")
    public void deleteConversation(@PathVariable String conversationId) {
        String userId = String.valueOf(UserContext.getUserId());
        chatApplicationService.deleteConversationWithAuth(conversationId, userId);
    }

    @Data
    public static class SendMessageRequest {
        private String content;
    }

}
