package com.zj.aiagent.domain.workflow.entity;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 节点模板
 * <p>
 * 定义节点的预设配置，类似前端的"节点库"中的节点类型。
 * <p>
 * 用户从节点库拖拽节点时，实际上是实例化了一个带预设配置的通用节点。
 *
 */
@Data
@Builder
public class NodeTemplate {

    /**
     * 模板 ID（唯一标识）
     */
    private String templateId;

    /**
     * 模板名称（如 "PlanNode"）
     */
    private String templateName;

    /**
     * 模板显示标签（如 "规划节点"）
     */
    private String templateLabel;

    /**
     * 基础节点类型
     * <p>
     * 支持: LLM_NODE, TOOL_NODE, ROUTER_NODE, CUSTOM_NODE
     */
    private String baseType;

    /**
     * 图标名称
     */
    private String icon;

    /**
     * System Prompt 模板
     * <p>
     * 支持变量占位符，语法: {@code {state.fieldName}}
     * <p>
     * 示例:
     * 
     * <pre>
     * 你是一个规划专家。
     *
     * 用户问题: {state.userQuestion}
     * 可用工具: {state.availableTools}
     *
     * 请输出 JSON 格式的执行计划...
     * </pre>
     */
    private String systemPromptTemplate;

    /**
     * 输出 Schema 定义
     * <p>
     * 声明本节点输出的字段列表
     */
    private List<StateField> outputSchema;

    /**
     * 默认配置
     * <p>
     * 模板的预设配置（如默认模型、温度等）
     */
    private Map<String, Object> defaultConfig;

    /**
     * 用户可编辑的字段列表
     * <p>
     * 例如: ["model", "mcpTools", "temperature"]
     */
    private List<String> editableFields;

    /**
     * 是否系统内置模板
     */
    private boolean builtIn;

    /**
     * 是否已废弃
     */
    private boolean deprecated;

    /**
     * 模板描述
     */
    private String description;
}
