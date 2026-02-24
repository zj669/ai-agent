package com.zj.aiagent.application.swarm.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkspaceDefaultsDTO {
    private Long workspaceId;
    private Long humanAgentId;
    private Long assistantAgentId;
    private Long defaultGroupId;
}
