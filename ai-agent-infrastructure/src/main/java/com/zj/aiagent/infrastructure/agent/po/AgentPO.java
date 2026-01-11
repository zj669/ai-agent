package com.zj.aiagent.infrastructure.agent.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "agent_info", autoResultMap = true)
public class AgentPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String name;
    private String description;
    private String icon;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode graphJson; // Mapped to JSON automatically

    private Integer status;
    private Long publishedVersionId;

    @Version
    private Integer version;

    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
