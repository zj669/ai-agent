package com.zj.aiagent.application.writing.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WritingTaskSummaryDTO {

    private Long id;
    private String taskUuid;
    private String taskType;
    private String title;
    private String instruction;
    private String status;
    private Integer priority;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
