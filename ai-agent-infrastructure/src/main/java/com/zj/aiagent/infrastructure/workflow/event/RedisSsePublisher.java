package com.zj.aiagent.infrastructure.workflow.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.chat.valobj.SseEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis SSE 发布者
 * 将事件发布到 Redis 频道
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSsePublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CHANNEL_PREFIX = "workflow:channel:";

    public void publish(SseEventPayload payload) {
        try {
            String channel = CHANNEL_PREFIX + payload.getExecutionId();
            String message = objectMapper.writeValueAsString(payload);
            redisTemplate.convertAndSend(channel, message);
            log.debug("[SSE-Pub] Published to {}: {}", channel, message);
        } catch (JsonProcessingException e) {
            log.error("[SSE-Pub] Failed to serialize payload", e);
        }
    }
}
