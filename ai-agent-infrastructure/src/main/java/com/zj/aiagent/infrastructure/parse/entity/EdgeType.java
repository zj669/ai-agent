package com.zj.aiagent.infrastructure.parse.entity;

/**
 * 边类型枚举
 * 用于区分DAG中不同类型的边，支持循环结构
 */
public enum EdgeType {
    /**
     * 标准依赖边
     * 参与拓扑排序，表示节点间的依赖关系
     */
    DEPENDENCY,

    /**
     * 循环边（回路边）
     * 不参与拓扑排序，在运行时触发节点重新入队
     * 用于实现 ReAct 等需要循环执行的模式
     */
    LOOP_BACK,

    /**
     * 条件边
     * 由 RouterNode 等节点动态决定是否激活
     * 可以与 DEPENDENCY 或 LOOP_BACK 结合使用
     */
    CONDITIONAL
}
