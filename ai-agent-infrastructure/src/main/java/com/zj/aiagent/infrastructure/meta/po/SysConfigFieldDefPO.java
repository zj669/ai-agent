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
@TableName(value = "sys_config_field_def", autoResultMap = true)
public class SysConfigFieldDefPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String fieldKey;
    private String fieldLabel;
    private String fieldType;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode options;

    private String defaultValue;
    private String placeholder;
    private String description;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode validationRules;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
