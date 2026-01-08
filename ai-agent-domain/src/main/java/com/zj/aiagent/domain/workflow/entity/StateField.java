package com.zj.aiagent.domain.workflow.entity;

import lombok.Builder;
import lombok.Data;

/**
 * 状态字段定义
 * <p>
 * 定义 State 中每个字段的元数据
 */
@Data
@Builder
public class StateField {

    /**
     * 字段名
     */
    private String name;

    /**
     * 字段类型
     * <p>
     * 支持: string, list, map, object, number, boolean
     */
    private String type;

    /**
     * 字段描述
     */
    private String description;

    /**
     * 是否必需
     */
    private boolean required;

    /**
     * Reducer 类型
     * <p>
     * 支持: overwrite, append, addMessages, increment, mergeMap
     */
    private String reducerType;

    /**
     * 默认值
     */
    private Object defaultValue;
}
