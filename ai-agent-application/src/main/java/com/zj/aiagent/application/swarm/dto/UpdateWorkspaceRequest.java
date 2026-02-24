package com.zj.aiagent.application.swarm.dto;

import lombok.Data;

@Data
public class UpdateWorkspaceRequest {
    private String name;
    private Integer maxRoundsPerTurn;
    private Long llmConfigId;
}
