package com.zj.aiagent.interfaces.web.dto.response.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 配置字段属性定义响应
 *
 * @author zj
 * @since 2025-12-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigFieldDefinitionResponse {

    /**
     * 字段名称
     */
    private String fieldName;

    /**
     * 字段标签（显示名称）
     */
    private String fieldLabel;

    /**
     * 字段类型（text, number, boolean, select, textarea, json, password等）
     */
    private String fieldType;

    /**
     * 是否必填
     */
    private Boolean required;

    /**
     * 字段描述
     */
    private String description;

    /**
     * 默认值
     */
    private Object defaultValue;

    /**
     * 可选项（针对select类型）
     */
    private List<String> options;
}
