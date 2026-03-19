package com.zj.aiagent.application.writing.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WritingDraftSummaryDTO {

    private Long id;
    private Integer versionNo;
    private String title;
    private String content;
    private String status;
    private LocalDateTime createdAt;
}
