package com.zj.aiagent.infrastructure.workflow.stream;

import com.zj.aiagent.domain.chat.valobj.SseEventPayload;
import com.zj.aiagent.domain.chat.valobj.SseEventPayload.ContentPayload;
import com.zj.aiagent.domain.workflow.port.StreamPublisher;
import com.zj.aiagent.domain.workflow.valobj.ExecutionStatus;
import com.zj.aiagent.domain.workflow.valobj.NodeExecutionResult;
import com.zj.aiagent.domain.workflow.valobj.SseEventType;
import com.zj.aiagent.domain.workflow.valobj.StreamContext;
import com.zj.aiagent.infrastructure.workflow.event.RedisSsePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis SSE 流式推送器实现
 * 实现 StreamPublisher 端口接口
 */
@Slf4j
@RequiredArgsConstructor
public class RedisSseStreamPublisher implements StreamPublisher {

    private final RedisSsePublisher ssePublisher;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final StreamContext context;

    @Override
    public void publishStart() {
        log.debug("[Stream] Publishing START for node: {}", context.getNodeId());
        publish(SseEventType.START, ExecutionStatus.RUNNING, null, null, false);
    }

    @Override
    public void publishDelta(String delta) {
        if (delta == null || delta.isEmpty()) {
            return;
        }
        log.trace("[Stream] Publishing delta for node: {}, length: {}",
                context.getNodeId(), delta.length());
        publish(SseEventType.UPDATE, ExecutionStatus.RUNNING, null, delta, false);
    }

    @Override
    public void publishThought(String thought) {
        if (thought == null || thought.isEmpty()) {
            return;
        }
        log.debug("[Stream] Publishing thought for node: {}", context.getNodeId());
        publish(SseEventType.UPDATE, ExecutionStatus.RUNNING, null, thought, true);
    }

    @Override
    public void publishFinish(NodeExecutionResult result) {
        log.debug("[Stream] Publishing FINISH for node: {}, status: {}",
                context.getNodeId(), result.getStatus());

        String content = null;
        if (result.getOutputs() != null) {
            Object response = result.getOutputs().get("response");
            if (response == null) {
                response = result.getOutputs().get("text");
            }
            content = response != null ? response.toString() : null;
        }

        publish(SseEventType.FINISH, result.getStatus(), content, null, false);
    }

    @Override
    public void publishError(String errorMessage) {
        log.warn("[Stream] Publishing ERROR for node: {}, message: {}",
                context.getNodeId(), errorMessage);

        SseEventPayload payload = buildPayload(SseEventType.ERROR, ExecutionStatus.FAILED);
        payload.setPayload(ContentPayload.builder()
                .title(context.getNodeName())
                .content(errorMessage)
                .renderMode("TEXT")
                .build());
        ssePublisher.publish(payload);
    }

    @Override
    public void publishData(Object data, String renderMode) {
        log.debug("[Stream] Publishing data for node: {}, renderMode: {}",
                context.getNodeId(), renderMode);

        SseEventPayload payload = buildPayload(SseEventType.UPDATE, ExecutionStatus.RUNNING);
        String content = data != null ? data.toString() : null;
        payload.setPayload(ContentPayload.builder()
                .title(context.getNodeName())
                .content(content)
                .renderMode(renderMode)
                .build());
        ssePublisher.publish(payload);
    }

    @Override
    public void publishEvent(String eventType, java.util.Map<String, Object> payload) {
        log.debug("[Stream] Publishing custom event: {} for node: {}", eventType, context.getNodeId());

        String jsonContent;
        try {
            jsonContent = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Failed to serialize event payload", e);
            jsonContent = "{}";
        }

        SseEventPayload ssePayload = buildPayload(SseEventType.UPDATE, ExecutionStatus.RUNNING);
        ssePayload.setPayload(ContentPayload.builder()
                .title(eventType)
                .content(jsonContent)
                .renderMode("JSON_EVENT")
                .build());
        ssePublisher.publish(ssePayload);
    }

    /**
     * 通用推送方法
     */
    private void publish(SseEventType eventType, ExecutionStatus status,
            String content, String delta, boolean isThought) {
        SseEventPayload payload = buildPayload(eventType, status);
        payload.setPayload(ContentPayload.builder()
                .title(context.getNodeName())
                .content(content)
                .delta(delta)
                .isThought(isThought)
                .renderMode(isThought ? "THOUGHT" : "MARKDOWN")
                .build());
        ssePublisher.publish(payload);
    }

    /**
     * 构建基础 Payload
     */
    private SseEventPayload buildPayload(SseEventType eventType, ExecutionStatus status) {
        return SseEventPayload.builder()
                .executionId(context.getExecutionId())
                .nodeId(context.getNodeId())
                .parentId(context.getParentId())
                .nodeType(context.getNodeType())
                .eventType(eventType)
                .status(status)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
