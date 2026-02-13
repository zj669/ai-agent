package com.zj.aiagent.domain.workflow.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 条件项值对象
 * 表示单个条件比较，包含左操作数、比较操作符和右操作数
 *
 * 左操作数通常为变量引用（如 "nodes.llm_1.output" 或 "inputs.query"），
 * 右操作数可以是字面值或变量引用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionItem {

    /**
     * 左操作数（变量引用，如 "nodes.llm_1.output" 或 "inputs.query"）
     */
    private String leftOperand;

    /**
     * 比较操作符
     */
    private ComparisonOperator operator;

    /**
     * 右操作数（字面值或变量引用）
     */
    private Object rightOperand;
}
