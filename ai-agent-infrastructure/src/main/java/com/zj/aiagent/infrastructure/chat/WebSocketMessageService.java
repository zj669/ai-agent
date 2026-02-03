package com.zj.aiagent.infrastructure.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket 消息推送服务
 * 用于向前端推送实时消息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketMessageService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 推送会话标题更新
     * @param conversationId 会话ID
     * @param title 新标题
     */
    public void sendTitleUpdate(String conversationId, String title) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("conversationId", conversationId);
            message.put("title", title);
            
            String destination = "/topic/conversation/" + conversationId + "/title";
            messagingTemplate.convertAndSend(destination, message);
            
            log.debug("Sent title update to {}: {}", destination, title);
        } catch (Exception e) {
            log.error("Failed to send title update for conversation {}", conversationId, e);
        }
    }
}
