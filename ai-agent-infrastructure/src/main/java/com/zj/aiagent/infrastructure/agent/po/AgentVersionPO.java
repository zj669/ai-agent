package com.zj.aiagent.infrastructure.agent.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "agent_version", autoResultMap = true)
public class AgentVersionPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long agentId;
    private Integer version;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode graphSnapshot;

    private String description;
    private LocalDateTime createTime;
}
