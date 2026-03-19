package com.zj.aiagent.domain.writing.entity;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一次写作协作会话的业务根对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WritingSession {

    private Long id;
    private Long workspaceId;
    private Long rootAgentId;
    private Long humanAgentId;
    private Long defaultGroupId;
    private String title;
    private String goal;
    private JsonNode constraintsJson;
    @Builder.Default
    private String status = "PLANNING";
    private Long currentDraftId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
