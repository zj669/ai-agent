package com.zj.aiagent.domain.swarm.entity;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 蜂群IM消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwarmMessage {
    private Long id;
    private Long workspaceId;
    private Long groupId;
    private Long senderId;
    @Builder.Default
    private String contentType = "text";
    private String content;
    private LocalDateTime sendTime;
}
