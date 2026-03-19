package com.zj.aiagent.domain.writing.entity;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * swarm agent 在写作场景下的业务投影。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WritingAgent {

    private Long id;
    private Long sessionId;
    private Long swarmAgentId;
    private String role;
    private String description;
    private JsonNode skillTagsJson;
    @Builder.Default
    private String status = "IDLE";
    @Builder.Default
    private Integer sortOrder = 0;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
