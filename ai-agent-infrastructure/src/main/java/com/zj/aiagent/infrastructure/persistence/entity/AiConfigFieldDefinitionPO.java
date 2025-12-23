package com.zj.aiagent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 配置字段定义PO
 *
 * @author zj
 * @since 2025-12-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_config_field_definition")
public class AiConfigFieldDefinitionPO {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 配置类型 (MODEL, HUMAN_INTERVENTION等)
     */
    @TableField("config_type")
    private String configType;

    /**
     * 字段名称
     */
    @TableField("field_name")
    private String fieldName;

    /**
     * 字段标签
     */
    @TableField("field_label")
    private String fieldLabel;

    /**
     * 字段类型 (text, number, boolean, select, textarea)
     */
    @TableField("field_type")
    private String fieldType;

    /**
     * 是否必填
     */
    @TableField("required")
    private Boolean required;

    /**
     * 字段描述
     */
    @TableField("description")
    private String description;

    /**
     * 默认值
     */
    @TableField("default_value")
    private String defaultValue;

    /**
     * 可选项 (JSON数组,用于select类型)
     */
    @TableField("options")
    private String options;

    /**
     * 排序顺序
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
