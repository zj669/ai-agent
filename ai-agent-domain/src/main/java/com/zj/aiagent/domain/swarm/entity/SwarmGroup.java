package com.zj.aiagent.domain.swarm.entity;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 蜂群IM群组（P2P = 2人群）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwarmGroup {
    private Long id;
    private Long workspaceId;
    private String name;
    @Builder.Default
    private Integer contextTokens = 0;
    private LocalDateTime createdAt;
}
