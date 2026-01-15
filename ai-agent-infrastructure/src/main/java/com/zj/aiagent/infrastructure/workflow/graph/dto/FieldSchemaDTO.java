package com.zj.aiagent.infrastructure.workflow.graph.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 字段定义 DTO
 * 对应 inputSchema/outputSchema 中的元素
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldSchemaDTO {

    /**
     * 字段键名
     */
    private String key;

    /**
     * 显示标签
     */
    private String label;

    /**
     * 数据类型（string, number, boolean, array, object）
     */
    private String type;

    /**
     * 字段描述
     */
    private String description;

    /**
     * 是否必填
     */
    private Boolean required;

    /**
     * 默认值
     */
    private Object defaultValue;

    /**
     * 数据来源引用（仅 inputSchema）
     * 如 state.user_input, node.llm-1.output.result
     */
    private String sourceRef;

    /**
     * 合并策略（overwrite, append, addMessages）
     */
    private String reducerType;
}
