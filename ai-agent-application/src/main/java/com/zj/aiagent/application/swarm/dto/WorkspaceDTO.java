package com.zj.aiagent.application.swarm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WorkspaceDTO {
    private Long id;
    private String name;
    private Long userId;
    private Integer agentCount;
    private Integer maxRoundsPerTurn;
    private Long llmConfigId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
