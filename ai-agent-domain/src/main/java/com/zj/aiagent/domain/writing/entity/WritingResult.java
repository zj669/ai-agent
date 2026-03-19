package com.zj.aiagent.domain.writing.entity;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 子 Agent 任务执行结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WritingResult {

    private Long id;
    private Long sessionId;
    private Long taskId;
    private Long writingAgentId;
    private Long swarmAgentId;
    @Builder.Default
    private String resultType = "TEXT";
    private String summary;
    private String content;
    private JsonNode structuredPayloadJson;
    private LocalDateTime createdAt;
}
