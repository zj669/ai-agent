package com.zj.aiagent.infrastructure.swarm.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("swarm_group")
public class SwarmGroupPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workspaceId;
    private String name;
    private Integer contextTokens;
    private LocalDateTime createdAt;
}
