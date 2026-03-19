package com.zj.aiagent.domain.writing.entity;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 主 Agent 汇总后的草稿版本。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WritingDraft {

    private Long id;
    private Long sessionId;
    private Integer versionNo;
    private String title;
    private String content;
    private JsonNode sourceResultIdsJson;
    @Builder.Default
    private String status = "DRAFT";
    private Long createdBySwarmAgentId;
    private LocalDateTime createdAt;
}
