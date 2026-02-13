package com.zj.aiagent.domain.workflow.valobj;

/**
 * 逻辑操作符枚举
 * 用于条件组（ConditionGroup）内多个条件项的逻辑组合
 */
public enum LogicalOperator {

    /** 逻辑与：所有条件都满足时为 true */
    AND,

    /** 逻辑或：至少一个条件满足时为 true */
    OR
}
