package com.zj.aiagent.domain.agent.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent Version Snapshot Entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentVersion {
    private Long id;
    private Long agentId;
    private Integer version;
    private String graphSnapshot;
    private String description; // Version description/changelog
    private LocalDateTime createTime;

    public AgentVersion(Long agentId, Integer version, String graphSnapshot) {
        this.agentId = agentId;
        this.version = version;
        this.graphSnapshot = graphSnapshot;
        this.createTime = LocalDateTime.now();
    }
}
