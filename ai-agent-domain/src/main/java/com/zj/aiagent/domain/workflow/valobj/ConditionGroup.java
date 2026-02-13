package com.zj.aiagent.domain.workflow.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 条件组值对象
 * 一组通过 AND 或 OR 逻辑连接的条件项
 *
 * 当 operator 为 AND 时，所有条件项都满足才为 true；
 * 当 operator 为 OR 时，至少一个条件项满足即为 true。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionGroup {

    /**
     * 组内逻辑操作符（AND 或 OR）
     */
    private LogicalOperator operator;

    /**
     * 条件项列表
     */
    private List<ConditionItem> conditions;
}
