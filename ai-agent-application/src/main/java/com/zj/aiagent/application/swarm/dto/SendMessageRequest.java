package com.zj.aiagent.application.swarm.dto;

import lombok.Data;

@Data
public class SendMessageRequest {
    private Long senderId;
    private String content;
    private String contentType = "text";
}
