package com.zj.aiagent.application.writing.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WritingSessionSummaryDTO {

    private Long id;
    private Long workspaceId;
    private Long rootAgentId;
    private Long humanAgentId;
    private Long defaultGroupId;
    private String title;
    private String goal;
    private String status;
    private Long currentDraftId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
