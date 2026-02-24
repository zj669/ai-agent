package com.zj.aiagent.infrastructure.swarm.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("swarm_group_member")
public class SwarmGroupMemberPO {
    private Long groupId;
    private Long agentId;
    private Long lastReadMessageId;
    private LocalDateTime joinedAt;
}
