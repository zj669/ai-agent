package com.zj.aiagent.domain.workflow.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EdgeDefinitionEntity {
    /**
     * 边ID
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


    private String condition;

    /**
     * 边类型
     * DEPENDENCY: 标准依赖边（默认）
     * LOOP_BACK: 循环边，不参与拓扑排序
     * CONDITIONAL: 条件边，由节点动态决定是否激活
     */
    private String edgeType ;

}
