package com.zj.aiagent.domain.workflow.entity;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 状态 Schema 定义
 * <p>
 * 声明 Workflow State 的结构、类型和 Reducer 策略
 * <p>
 * 类似 LangGraph 的 TypedDict State 定义
 */
@Data
@Builder
public class StateSchema {

    /**
     * Schema ID
     */
    private String schemaId;

    /**
     * Schema 名称
     */
    private String schemaName;

    /**
     * 字段定义列表
     */
    private List<StateField> fields;

    /**
     * 输入字段列表（Workflow 的外部输入）
     * <p>
     * 类似 LangGraph 的 InputState
     */
    private List<String> inputFields;

    /**
     * 输出字段列表（Workflow 的外部输出）
     * <p>
     * 类似 LangGraph 的 OutputState
     */
    private List<String> outputFields;
}
