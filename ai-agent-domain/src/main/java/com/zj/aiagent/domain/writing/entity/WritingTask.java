package com.zj.aiagent.domain.writing.entity;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 主 Agent 拆分出的写作任务。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WritingTask {

    private Long id;
    private String taskUuid;
    private Long sessionId;
    private Long writingAgentId;
    private Long swarmAgentId;

    @Builder.Default
    private String taskType = "WRITING";

    private String title;
    private String instruction;
    private JsonNode inputPayloadJson;
    private JsonNode expectedOutputSchemaJson;

    @Builder.Default
    private String status = "PENDING";

    @Builder.Default
    private Integer priority = 0;

    private Long createdBySwarmAgentId;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
