package com.zj.aiagent.infrastructure.swarm.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("swarm_workspace")
public class SwarmWorkspacePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long userId;
    private String defaultModel;
    private Long llmConfigId;
    private Integer maxRoundsPerTurn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
