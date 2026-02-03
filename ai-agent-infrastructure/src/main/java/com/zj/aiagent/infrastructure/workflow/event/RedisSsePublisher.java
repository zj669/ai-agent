package com.zj.aiagent.infrastructure.workflow.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.chat.valobj.SseEventPayload;
import com.zj.aiagent.infrastructure.redis.IRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Redis SSE 发布者
 * 将事件发布到 Redis 频道
 * 
 * 业务逻辑:
 * - 构建频道名称 (workflow:channel:{executionId})
 * - 序列化 SSE 事件负载
 * - 发布消息到 Redis 频道
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSsePublisher {

    private final IRedisService redisService;
    private final ObjectMapper objectMapper;

    private static final String CHANNEL_PREFIX = "workflow:channel:";

    /**
     * 发布 SSE 事件到 Redis 频道
     * 
     * @param payload SSE 事件负载
     */
    public void publish(SseEventPayload payload) {
        try {
            // 业务逻辑: 构建频道名称
            String channel = CHANNEL_PREFIX + payload.getExecutionId();
            
            // 业务逻辑: 序列化负载
            String message = objectMapper.writeValueAsString(payload);
            
            // 使用 IRedisService 的基础操作: 发布消息
            redisService.publish(channel, message);
            
            log.debug("[SSE-Pub] Published to {}: {}", channel, message);
        } catch (JsonProcessingException e) {
            log.error("[SSE-Pub] Failed to serialize payload", e);
        }
    }
}

