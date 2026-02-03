package com.zj.aiagent.infrastructure.workflow.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.chat.valobj.SseEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.lang.NonNull;

import java.util.function.Consumer;

/**
 * Redis SSE 消息监听器
 * 将 Redis 消息反序列化为 SseEventPayload 并通过回调处理
 */
@Slf4j
@RequiredArgsConstructor
public class RedisSseListener implements MessageListener {

    private final ObjectMapper objectMapper;
    private final Consumer<SseEventPayload> eventHandler;

    @Override
    public void onMessage(@NonNull Message message, byte[] pattern) {
        try {
            byte[] body = message.getBody();
            
            // 先将字节数组转换为字符串
            String messageStr = new String(body, java.nio.charset.StandardCharsets.UTF_8);
            
            // 如果消息被 Redisson 的 JsonJacksonCodec 双重序列化（字符串外面包了引号）
            // 需要先去掉外层的引号
            if (messageStr.startsWith("\"") && messageStr.endsWith("\"")) {
                // 去掉首尾引号并处理转义字符
                messageStr = objectMapper.readValue(messageStr, String.class);
            }
            
            // 反序列化为 SseEventPayload
            SseEventPayload payload = objectMapper.readValue(messageStr, SseEventPayload.class);
            eventHandler.accept(payload);
        } catch (Exception e) {
            log.error("[SSE-Sub] Failed to deserialize message", e);
        }
    }
}
