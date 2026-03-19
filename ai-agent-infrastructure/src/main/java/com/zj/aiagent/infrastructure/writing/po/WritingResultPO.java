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
@TableName(value = "writing_result", autoResultMap = true)
public class WritingResultPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long taskId;
    private Long writingAgentId;
    private Long swarmAgentId;
    private String resultType;
    private String summary;
    private String content;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode structuredPayloadJson;
    private LocalDateTime createdAt;
}
