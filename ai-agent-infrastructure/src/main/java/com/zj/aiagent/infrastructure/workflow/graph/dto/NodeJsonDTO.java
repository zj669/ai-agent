package com.zj.aiagent.infrastructure.workflow.graph.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 节点定义 DTO
 * 对应 graphJson 中的 nodes[] 元素
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NodeJsonDTO {

    /**
     * 节点唯一标识
     */
    private String nodeId;

    /**
     * 节点显示名称
     */
    private String nodeName;

    /**
     * 节点类型（如 START, END, LLM, HTTP, CONDITION, PARALLEL, TOOL）
     */
    private String nodeType;

    /**
     * 输入字段定义
     */
    private List<FieldSchemaDTO> inputSchema;

    /**
     * 输出字段定义
     */
    private List<FieldSchemaDTO> outputSchema;

    /**
     * 用户可配置项
     */
    private Map<String, Object> userConfig;

    /**
     * 前端可视化位置
     */
    private PositionDTO position;

    /**
     * 模板ID（可选）
     */
    private String templateId;
}
