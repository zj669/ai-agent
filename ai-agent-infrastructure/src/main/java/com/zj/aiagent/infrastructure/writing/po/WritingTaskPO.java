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
@TableName(value = "writing_task", autoResultMap = true)
public class WritingTaskPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskUuid;
    private Long sessionId;
    private Long writingAgentId;
    private Long swarmAgentId;
    private String taskType;
    private String title;
    private String instruction;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode inputPayloadJson;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode expectedOutputSchemaJson;

    private String status;
    private Integer priority;
    private Long createdBySwarmAgentId;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
