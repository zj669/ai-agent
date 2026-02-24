package com.zj.aiagent.infrastructure.swarm.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("swarm_message")
public class SwarmMessagePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workspaceId;
    private Long groupId;
    private Long senderId;
    private String contentType;
    private String content;
    private LocalDateTime sendTime;
}
