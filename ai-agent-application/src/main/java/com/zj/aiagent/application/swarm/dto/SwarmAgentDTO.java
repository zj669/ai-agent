package com.zj.aiagent.application.swarm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SwarmAgentDTO {
    private Long id;
    private Long workspaceId;
    private Long agentId;
    private String role;
    private Long parentId;
    private String status;
    private LocalDateTime createdAt;
}
