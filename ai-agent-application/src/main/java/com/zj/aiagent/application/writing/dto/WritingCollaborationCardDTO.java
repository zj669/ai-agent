package com.zj.aiagent.application.writing.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WritingCollaborationCardDTO {

    private Long writingAgentId;
    private Long swarmAgentId;
    private String role;
    private String description;
    private String status;
    private Integer sortOrder;
    private WritingTaskSummaryDTO currentTask;
    private WritingResultSummaryDTO latestResult;
    private LocalDateTime updatedAt;
}
