package com.zj.aiagent.infrastructure.writing.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName(value = "writing_agent", autoResultMap = true)
public class WritingAgentPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long swarmAgentId;
    private String role;
    private String description;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode skillTagsJson;
    private String status;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
