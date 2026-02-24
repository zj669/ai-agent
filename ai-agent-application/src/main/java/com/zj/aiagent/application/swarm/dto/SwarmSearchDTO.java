package com.zj.aiagent.application.swarm.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SwarmSearchDTO {
    private List<SwarmAgentDTO> agents;
    private List<SwarmGroupDTO> groups;
}
