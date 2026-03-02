package com.zj.aiagent.infrastructure.swarm.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("swarm_workspace_agent")
public class SwarmWorkspaceAgentPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workspaceId;
    private Long agentId;
    private String role;
    private String description;
    private Long parentId;
    private String llmHistory;
    private String status;
    private LocalDateTime createdAt;
}
