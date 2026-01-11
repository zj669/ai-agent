package com.zj.aiagent.domain.agent.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lightweight projection of Agent (excludes graphJson)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentSummary {
    private Long id;
    private Long userId;
    private String name;
    private String description;
    private String icon;
    private AgentStatus status;
    private Long publishedVersionId;
    private LocalDateTime updateTime;
}
