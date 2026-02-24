package com.zj.aiagent.domain.swarm.entity;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 蜂群工作空间
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwarmWorkspace {
    private Long id;
    private String name;
    private Long userId;
    private String defaultModel;
    private Long llmConfigId;
    @Builder.Default
    private Integer maxRoundsPerTurn = 10;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
