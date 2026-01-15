package com.zj.aiagent.infrastructure.workflow.graph.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 边定义 DTO
 * 对应 graphJson 中的 edges[] 元素
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EdgeJsonDTO {

    /**
     * 边唯一标识
     */
    private String edgeId;

    /**
     * 源节点ID
     */
    private String source;

    /**
     * 目标节点ID
     */
    private String target;

    /**
     * 边标签（用于条件分支显示）
     */
    private String label;

    /**
     * 条件表达式（可选）
     */
    private String condition;

    /**
     * 边类型（DEPENDENCY, LOOP_BACK, CONDITIONAL）
     */
    private String edgeType;
}
