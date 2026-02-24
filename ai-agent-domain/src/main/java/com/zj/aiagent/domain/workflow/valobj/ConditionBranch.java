package com.zj.aiagent.domain.workflow.valobj;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 条件分支值对象
 * 表示条件节点的一个分支（if / else if / else），包含优先级、条件组和目标节点 ID
 *
 * 分支按 priority 升序评估（0 最优先），首个命中的分支胜出。
 * isDefault 为 true 的分支作为 else 兜底，当所有非 default 分支都不匹配时选中。
 * 一个条件节点必须恰好有一个 default 分支。
 *
 * 多个 conditionGroups 之间为 AND 关系：所有组都满足才命中该分支。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionBranch {

    /**
     * 分支优先级（0 起始，越小越优先）
     */
    private int priority;

    /**
     * 目标节点 ID
     */
    private String targetNodeId;

    /**
     * 分支描述（LLM 模式使用）
     */
    private String description;

    /**
     * 是否为默认分支（else）
     * 注意：Lombok 对 boolean isXxx 字段生成 isXxx() getter，
     * Jackson 会将其映射为 JSON 属性 "xxx"（去掉 is 前缀），
     * 导致 JSON 中的 "isDefault" 无法正确反序列化。
     * 必须用 @JsonProperty 显式指定 JSON 属性名。
     */
    @JsonProperty("isDefault")
    private boolean isDefault;

    /**
     * 条件组列表（AND 关系：所有组都满足才命中）
     */
    private List<ConditionGroup> conditionGroups;
}
