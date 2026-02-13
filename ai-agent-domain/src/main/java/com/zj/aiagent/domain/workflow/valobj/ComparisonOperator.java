package com.zj.aiagent.domain.workflow.valobj;

/**
 * 比较操作符枚举
 * 用于条件项（ConditionItem）中的值比较
 */
public enum ComparisonOperator {

    /** 等于 */
    EQUALS,

    /** 不等于 */
    NOT_EQUALS,

    /** 包含（字符串） */
    CONTAINS,

    /** 不包含（字符串） */
    NOT_CONTAINS,

    /** 大于 */
    GREATER_THAN,

    /** 小于 */
    LESS_THAN,

    /** 大于等于 */
    GREATER_THAN_OR_EQUAL,

    /** 小于等于 */
    LESS_THAN_OR_EQUAL,

    /** 为空（null 或空字符串） */
    IS_EMPTY,

    /** 不为空 */
    IS_NOT_EMPTY,

    /** 以...开头（字符串） */
    STARTS_WITH,

    /** 以...结尾（字符串） */
    ENDS_WITH
}
