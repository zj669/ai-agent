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
@TableName(value = "writing_session", autoResultMap = true)
public class WritingSessionPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workspaceId;
    private Long rootAgentId;
    private Long humanAgentId;
    private Long defaultGroupId;
    private String title;
    private String goal;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode constraintsJson;
    private String status;
    private Long currentDraftId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
