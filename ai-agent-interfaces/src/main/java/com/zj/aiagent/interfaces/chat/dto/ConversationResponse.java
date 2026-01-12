package com.zj.aiagent.interfaces.chat.dto;

import com.zj.aiagent.domain.chat.entity.Conversation;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

@Data
public class ConversationResponse {
    private String id;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ConversationResponse from(Conversation conversation) {
        ConversationResponse response = new ConversationResponse();
        BeanUtils.copyProperties(conversation, response);
        return response;
    }
}
