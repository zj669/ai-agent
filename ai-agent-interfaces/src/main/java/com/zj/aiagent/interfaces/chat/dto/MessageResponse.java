package com.zj.aiagent.interfaces.chat.dto;

import com.zj.aiagent.domain.chat.entity.Message;
import com.zj.aiagent.domain.chat.valobj.Citation;
import com.zj.aiagent.domain.chat.valobj.MessageRole;
import com.zj.aiagent.domain.chat.valobj.MessageStatus;
import com.zj.aiagent.domain.chat.valobj.ThoughtStep;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class MessageResponse {
    private String id;
    private String conversationId;
    private MessageRole role;
    private String content;
    private List<ThoughtStep> thoughtProcess;
    private List<Citation> citations;
    private MessageStatus status;
    private LocalDateTime createdAt;
    private Map<String, Object> metadata;

    public static MessageResponse from(Message message) {
        MessageResponse response = new MessageResponse();
        BeanUtils.copyProperties(message, response);
        return response;
    }
}
