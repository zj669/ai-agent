package com.zj.aiagent.application.swarm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SwarmMessageDTO {
    private Long id;
    private Long groupId;
    private Long senderId;
    private String content;
    private String contentType;
    private LocalDateTime sendTime;
}
