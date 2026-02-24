package com.zj.aiagent.domain.swarm.entity;

import com.zj.aiagent.domain.swarm.valobj.SwarmAgentStatus;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 蜂群工作空间Agent（关联 agent_info，人类节点 agentId 为空）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwarmAgent {
    private Long id;
    private Long workspaceId;
    /** 关联的 agent_info.id，人类节点为 null */
    private Long agentId;
    private String role;
    /** 父Agent ID（谁创建的，指向本表 id） */
    private Long parentId;
    private String llmHistory;
    @Builder.Default
    private SwarmAgentStatus status = SwarmAgentStatus.IDLE;
    private LocalDateTime createdAt;
}
