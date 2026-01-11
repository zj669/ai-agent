package com.zj.aiagent.infrastructure.workflow.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "workflow_node_execution_log", autoResultMap = true)
public class WorkflowNodeExecutionLogPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String executionId;
    private String nodeId;
    private String nodeName;
    private String nodeType;

    private String renderMode;
    private Integer status;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode inputs;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode outputs;

    private String errorMessage;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
