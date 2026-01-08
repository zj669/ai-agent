package com.zj.aiagent.infrastructure.parse.entity;

import com.zj.aiagent.shared.model.enums.IBaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum NodeType implements IBaseEnum<Integer> {
    ACT_NODE(0, "精准执行节点"),
    // HUMAN_NODE(1, "人工节点"), // 已废弃：人工介入现在是通用配置
    PLAN_NODE(2, "计划节点"),
    EVALUE_NODE(3, "ReAct节点"),
    ;

    private Integer value;
    private String label;
}
