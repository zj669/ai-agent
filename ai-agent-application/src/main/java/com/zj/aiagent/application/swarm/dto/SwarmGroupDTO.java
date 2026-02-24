package com.zj.aiagent.application.swarm.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SwarmGroupDTO {
    private Long id;
    private Long workspaceId;
    private String name;
    private List<Long> memberIds;
    private Integer unreadCount;
    private SwarmMessageDTO lastMessage;
    private Integer contextTokens;
}
