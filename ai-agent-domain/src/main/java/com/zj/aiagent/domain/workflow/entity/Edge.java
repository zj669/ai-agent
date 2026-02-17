package com.zj.aiagent.domain.workflow.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 边实体
 * 表示工作流图中两个节点之间的连接
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Edge {

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

    /**
     * 条件表达式或决策描述
     * - 当源节点是条件节点且使用表达式模式时：SpEL 表达式（如 "#input > 100"）
     * - 当源节点是条件节点且使用 LLM 模式时：决策条件描述（如 "用户表达了购买意向"）
     * - 其他情况下：可为空
     */
    private String condition;

    /**
     * 边类型
     */
    private EdgeType edgeType;

    /**
     * 检查是否为条件边
     */
    public boolean isConditional() {
        return edgeType == EdgeType.CONDITIONAL ||
                (condition != null && !condition.isEmpty());
    }

    /**
     * 检查是否为默认边
     * 仅显式 DEFAULT 类型或条件标记为 default 时视为默认边，避免误判条件边。
     */
    public boolean isDefault() {
        if (edgeType == EdgeType.DEFAULT) {
            return true;
        }
        if (condition == null) {
            return false;
        }
        return "default".equalsIgnoreCase(condition.trim());
    }

    /**
     * 边类型枚举
     */
    public enum EdgeType {
        /**
         * 标准依赖边
         */
        DEPENDENCY,

        /**
         * 条件边
         */
        CONDITIONAL,

        /**
         * 默认边（条件节点的兜底路径）
         */
        DEFAULT
    }
}
