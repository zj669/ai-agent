package com.zj.aiagent.infrastructure.swarm.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("swarm_group_member")
public class SwarmGroupMemberPO {
    // 联合主键之一，标注 @TableId 消除 MyBatis-Plus 警告
    @TableId(type = IdType.INPUT)
    private Long groupId;
    private Long agentId;
    private Long lastReadMessageId;
    private LocalDateTime joinedAt;
}
