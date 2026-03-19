package com.zj.aiagent.application.writing.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WritingMessageViewDTO {

    private Long id;
    private Long senderId;
    private String senderRole;
    private String contentType;
    private String content;
    private LocalDateTime sendTime;
}
