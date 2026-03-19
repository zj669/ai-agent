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
@TableName(value = "writing_draft", autoResultMap = true)
public class WritingDraftPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Integer versionNo;
    private String title;
    private String content;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode sourceResultIdsJson;
    private String status;
    private Long createdBySwarmAgentId;
    private LocalDateTime createdAt;
}
