package com.zj.aiagent.infrastructure.meta.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "node_template", autoResultMap = true)
public class NodeTemplatePO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String typeCode;
    private String name;
    private String description;
    private String icon;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode defaultSchemaPolicy;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode initialSchema;

    private String category;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
