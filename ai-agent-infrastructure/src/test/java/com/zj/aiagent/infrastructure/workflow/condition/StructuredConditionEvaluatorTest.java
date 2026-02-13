package com.zj.aiagent.infrastructure.workflow.condition;

import com.zj.aiagent.domain.workflow.exception.ConditionConfigurationException;
import com.zj.aiagent.domain.workflow.valobj.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StructuredConditionEvaluator 单元测试 — 边界情况
 *
 * 覆盖场景：
 * - 无 default 分支抛异常 (Requirements 6.2)
 * - 多 default 分支抛异常 (Requirements 6.3)
 * - 空 branches 列表抛异常 (Requirements 6.2)
 * - null branches 列表抛异常 (Requirements 6.2)
 * - null operator 的 ConditionItem 被跳过 (Requirements 3.4)
 * - 变量不存在时条件视为不满足 (Requirements 3.3)
 */
class StructuredConditionEvaluatorTest {

    private StructuredConditionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new StructuredConditionEvaluator();
    }

    // ========== 无 default 分支 ==========

    @Test
    @DisplayName("should_throw_ConditionConfigurationException_when_no_default_branch")
    void should_throw_ConditionConfigurationException_when_no_default_branch() {
        // 构建两个非 default 分支，无 default
        List<ConditionBranch> branches = List.of(
                ConditionBranch.builder()
                        .priority(0)
                        .targetNodeId("node_1")
                        .isDefault(false)
                        .conditionGroups(List.of(
                                ConditionGroup.builder()
                                        .operator(LogicalOperator.AND)
                                        .conditions(List.of(
                                                ConditionItem.builder()
                                                        .leftOperand("inputs.query")
                                                        .operator(ComparisonOperator.EQUALS)
                                                        .rightOperand("hello")
                                                        .build()))
                                        .build()))
                        .build(),
                ConditionBranch.builder()
                        .priority(1)
                        .targetNodeId("node_2")
                        .isDefault(false)
                        .conditionGroups(List.of(
                                ConditionGroup.builder()
                                        .operator(LogicalOperator.AND)
                                        .conditions(List.of(
                                                ConditionItem.builder()
                                                        .leftOperand("inputs.query")
                                                        .operator(ComparisonOperator.EQUALS)
                                                        .rightOperand("world")
                                                        .build()))
                                        .build()))
                        .build()
        );

        ExecutionContext context = ExecutionContext.builder().build();

        ConditionConfigurationException ex = assertThrows(
                ConditionConfigurationException.class,
                () -> evaluator.evaluate(branches, context)
        );
        assertTrue(ex.getMessage().contains("default"));
    }

    // ========== 多 default 分支 ==========

    @Test
    @DisplayName("should_throw_ConditionConfigurationException_when_multiple_default_branches")
    void should_throw_ConditionConfigurationException_when_multiple_default_branches() {
        List<ConditionBranch> branches = List.of(
                ConditionBranch.builder()
                        .priority(0)
                        .targetNodeId("node_1")
                        .isDefault(true)
                        .conditionGroups(Collections.emptyList())
                        .build(),
                ConditionBranch.builder()
                        .priority(1)
                        .targetNodeId("node_2")
                        .isDefault(true)
                        .conditionGroups(Collections.emptyList())
                        .build()
        );

        ExecutionContext context = ExecutionContext.builder().build();

        ConditionConfigurationException ex = assertThrows(
                ConditionConfigurationException.class,
                () -> evaluator.evaluate(branches, context)
        );
        assertTrue(ex.getMessage().contains("多个 default"));
    }

    // ========== 空 branches 列表 ==========

    @Test
    @DisplayName("should_throw_ConditionConfigurationException_when_empty_branches")
    void should_throw_ConditionConfigurationException_when_empty_branches() {
        List<ConditionBranch> branches = new ArrayList<>();
        ExecutionContext context = ExecutionContext.builder().build();

        ConditionConfigurationException ex = assertThrows(
                ConditionConfigurationException.class,
                () -> evaluator.evaluate(branches, context)
        );
        assertTrue(ex.getMessage().contains("不能为空"));
    }

    // ========== null branches 列表 ==========

    @Test
    @DisplayName("should_throw_ConditionConfigurationException_when_null_branches")
    void should_throw_ConditionConfigurationException_when_null_branches() {
        ExecutionContext context = ExecutionContext.builder().build();

        ConditionConfigurationException ex = assertThrows(
                ConditionConfigurationException.class,
                () -> evaluator.evaluate(null, context)
        );
        assertTrue(ex.getMessage().contains("不能为空"));
    }

    // ========== null operator 的 ConditionItem 被跳过 ==========

    @Test
    @DisplayName("should_skip_condition_item_with_null_operator_and_fall_through_to_default")
    void should_skip_condition_item_with_null_operator_and_fall_through_to_default() {
        // 非 default 分支的条件项 operator 为 null → 该条件视为 false → 分支不匹配 → 走 default
        ConditionBranch branchWithNullOp = ConditionBranch.builder()
                .priority(0)
                .targetNodeId("node_1")
                .isDefault(false)
                .conditionGroups(List.of(
                        ConditionGroup.builder()
                                .operator(LogicalOperator.AND)
                                .conditions(List.of(
                                        ConditionItem.builder()
                                                .leftOperand("inputs.query")
                                                .operator(null) // null operator
                                                .rightOperand("hello")
                                                .build()))
                                .build()))
                .build();

        ConditionBranch defaultBranch = ConditionBranch.builder()
                .priority(1)
                .targetNodeId("node_default")
                .isDefault(true)
                .conditionGroups(Collections.emptyList())
                .build();

        List<ConditionBranch> branches = List.of(branchWithNullOp, defaultBranch);

        ExecutionContext context = ExecutionContext.builder()
                .inputs(Map.of("query", "hello"))
                .build();

        ConditionBranch result = evaluator.evaluate(branches, context);

        assertNotNull(result);
        assertTrue(result.isDefault());
        assertEquals("node_default", result.getTargetNodeId());
    }

    // ========== 变量不存在时条件视为不满足 ==========

    @Test
    @DisplayName("should_treat_condition_as_not_satisfied_when_node_output_variable_not_exists")
    void should_treat_condition_as_not_satisfied_when_node_output_variable_not_exists() {
        // 引用不存在的节点输出 → 变量解析为 null → 条件不满足 → 走 default
        ConditionBranch branchWithMissingVar = ConditionBranch.builder()
                .priority(0)
                .targetNodeId("node_1")
                .isDefault(false)
                .conditionGroups(List.of(
                        ConditionGroup.builder()
                                .operator(LogicalOperator.AND)
                                .conditions(List.of(
                                        ConditionItem.builder()
                                                .leftOperand("nodes.nonexistent_node.output")
                                                .operator(ComparisonOperator.EQUALS)
                                                .rightOperand("expected_value")
                                                .build()))
                                .build()))
                .build();

        ConditionBranch defaultBranch = ConditionBranch.builder()
                .priority(1)
                .targetNodeId("node_default")
                .isDefault(true)
                .conditionGroups(Collections.emptyList())
                .build();

        List<ConditionBranch> branches = List.of(branchWithMissingVar, defaultBranch);

        // 空的 ExecutionContext，没有任何节点输出
        ExecutionContext context = ExecutionContext.builder().build();

        ConditionBranch result = evaluator.evaluate(branches, context);

        assertNotNull(result);
        assertTrue(result.isDefault());
        assertEquals("node_default", result.getTargetNodeId());
    }

    @Test
    @DisplayName("should_treat_condition_as_not_satisfied_when_input_variable_not_exists")
    void should_treat_condition_as_not_satisfied_when_input_variable_not_exists() {
        // 引用不存在的全局输入 → 变量解析为 null → 条件不满足 → 走 default
        ConditionBranch branchWithMissingInput = ConditionBranch.builder()
                .priority(0)
                .targetNodeId("node_1")
                .isDefault(false)
                .conditionGroups(List.of(
                        ConditionGroup.builder()
                                .operator(LogicalOperator.AND)
                                .conditions(List.of(
                                        ConditionItem.builder()
                                                .leftOperand("inputs.nonexistent_key")
                                                .operator(ComparisonOperator.EQUALS)
                                                .rightOperand("expected_value")
                                                .build()))
                                .build()))
                .build();

        ConditionBranch defaultBranch = ConditionBranch.builder()
                .priority(1)
                .targetNodeId("node_default")
                .isDefault(true)
                .conditionGroups(Collections.emptyList())
                .build();

        List<ConditionBranch> branches = List.of(branchWithMissingInput, defaultBranch);

        // inputs 中没有 nonexistent_key
        ExecutionContext context = ExecutionContext.builder()
                .inputs(Map.of("other_key", "some_value"))
                .build();

        ConditionBranch result = evaluator.evaluate(branches, context);

        assertNotNull(result);
        assertTrue(result.isDefault());
        assertEquals("node_default", result.getTargetNodeId());
    }
}
