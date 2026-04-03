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
    /** 子Agent的能力边界和职责描述 */
    private String description;
    /** 父Agent ID（谁创建的，指向本表 id） */
    private Long parentId;
    private String llmHistory;
    /** 所属写作会话 ID */
    private Long sessionId;
    /** 协作面板排序 */
    @Builder.Default
    private Integer sortOrder = 0;
    @Builder.Default
    private SwarmAgentStatus status = SwarmAgentStatus.IDLE;
    private LocalDateTime createdAt;

    /**
     * 判断当前 Agent 是否为 Coordinator（协调者）。
     * 规则：parentId == null（直接服务用户，无父 Agent）。
     * 注意：即使没有子 Agent，刚启动时也应视为 Coordinator（会接收用户消息）。
     * @param hasChildren 当前 Agent 是否有子 Agent（此参数已废弃，仅保留签名兼容）
     */
    public boolean isCoordinator(boolean hasChildren) {
        return parentId == null;
    }

    /**
     * 判断当前 Agent 是否为 Worker（执行者）
     * 规则：有父 Agent
     */
    public boolean isWorker() {
        return parentId != null;
    }
}
