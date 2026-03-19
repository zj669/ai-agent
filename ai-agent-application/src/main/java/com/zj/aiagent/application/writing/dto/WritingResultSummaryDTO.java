package com.zj.aiagent.application.writing.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WritingResultSummaryDTO {

    private Long id;
    private Long taskId;
    private String resultType;
    private String summary;
    private String content;
    private LocalDateTime createdAt;
}
