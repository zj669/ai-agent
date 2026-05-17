package com.zj.aiagent.domain.workflow.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工作流节点字段定义。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldSchema {

    private String key;

    private String label;

    private String type;

    private String description;

    private Boolean required;

    private Object defaultValue;

    private String sourceRef;

    private String reducerType;

    private Boolean system;
}
