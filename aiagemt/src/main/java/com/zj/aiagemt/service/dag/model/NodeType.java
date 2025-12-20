package com.zj.aiagemt.service.dag.model;

import com.zj.aiagemt.model.common.IBaseEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum NodeType implements IBaseEnum<Integer> {
    ACT_NODE(0, "精准执行节点"),
    HUMAN_NODE(1, "人工节点"),
    PLAN_NODE(2, "计划节点"),
    REACT_NODE(3, "ReAct节点"),
    ROUTER_NODE(4, "路由节点"),
    ;

    private Integer value;
    private String label;
}
