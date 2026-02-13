package com.zj.aiagent.infrastructure.workflow.condition;

import com.zj.aiagent.domain.workflow.valobj.ComparisonOperator;
import com.zj.aiagent.domain.workflow.valobj.ConditionBranch;
import com.zj.aiagent.domain.workflow.valobj.ConditionGroup;
import com.zj.aiagent.domain.workflow.valobj.ConditionItem;
import com.zj.aiagent.domain.workflow.valobj.ExecutionContext;
import com.zj.aiagent.domain.workflow.valobj.LogicalOperator;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Property-Based Test: 比较操作符正确性
 *
 * // Feature: condition-branch-refactor, Property 5: Comparison operator correctness
 * **Validates: Requirements 2.4**
 *
 * 验证：对于任意一对可比较的值（String, Number, null）和任意 ComparisonOperator，
 * compareValues 函数的结果与参考实现一致。
 *
 * 由于 compareValues 是 private 方法，通过构造单条件 ConditionBranch 调用 evaluate() 间接测试。
 * 构造方式：一个非 default 分支（包含单个条件组、单个条件项）+ 一个 default 分支。
 * 如果 evaluate 返回非 default 分支，说明 compareValues 返回 true；否则返回 false。
 */
class StructuredConditionEvaluatorPropertyTest {

    private final StructuredConditionEvaluator evaluator = new StructuredConditionEvaluator();

    // ========== 辅助方法 ==========

    /**
     * 通过 evaluate() 间接调用 compareValues。
     * 构造一个包含单条件的分支 + default 分支，评估后判断是否命中非 default 分支。
     */
    private boolean evaluateComparison(Object leftValue, ComparisonOperator op, Object rightValue) {
        // 构造 ExecutionContext，将 leftValue 放入 node output
        ExecutionContext context = ExecutionContext.builder().build();
        context.setNodeOutput("testNode", Map.of("value", leftValue != null ? leftValue : "__NULL_MARKER__"));

        // 如果 leftValue 为 null，需要特殊处理：不放入 nodeOutput 让 resolveOperand 返回 null
        String leftOperand;
        if (leftValue == null) {
            leftOperand = "nodes.testNode.nonExistentKey";
        } else {
            leftOperand = "nodes.testNode.value";
        }

        ConditionItem item = ConditionItem.builder()
                .leftOperand(leftOperand)
                .operator(op)
                .rightOperand(rightValue)
                .build();

        ConditionGroup group = ConditionGroup.builder()
                .operator(LogicalOperator.AND)
                .conditions(List.of(item))
                .build();

        ConditionBranch matchBranch = ConditionBranch.builder()
                .priority(0)
                .targetNodeId("target")
                .description("test branch")
                .isDefault(false)
                .conditionGroups(List.of(group))
                .build();

        ConditionBranch defaultBranch = ConditionBranch.builder()
                .priority(1)
                .targetNodeId("default")
                .description("default branch")
                .isDefault(true)
                .conditionGroups(List.of())
                .build();

        ConditionBranch result = evaluator.evaluate(List.of(matchBranch, defaultBranch), context);
        return "target".equals(result.getTargetNodeId());
    }

    // ========== 参考实现 ==========

    /**
     * 参考实现：EQUALS — 与 StructuredConditionEvaluator.compareEquals 对齐
     */
    private boolean referenceEquals(Object left, Object right) {
        if (left == null && right == null) return true;
        if (left == null || right == null) return false;
        if (left instanceof Number && right instanceof Number) {
            return Double.compare(((Number) left).doubleValue(), ((Number) right).doubleValue()) == 0;
        }
        return left.toString().equals(right.toString());
    }

    /**
     * 参考实现：CONTAINS — String.contains
     */
    private boolean referenceContains(Object left, Object right) {
        if (left == null || right == null) return false;
        return left.toString().contains(right.toString());
    }

    /**
     * 参考实现：数值比较
     */
    private int referenceNumericCompare(Object left, Object right) {
        double l = toDoubleRef(left);
        double r = toDoubleRef(right);
        return Double.compare(l, r);
    }

    private double toDoubleRef(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        return Double.parseDouble(value.toString());
    }

    /**
     * 参考实现：IS_EMPTY — null 或空字符串
     */
    private boolean referenceIsEmpty(Object value) {
        if (value == null) return true;
        return value.toString().isEmpty();
    }

    /**
     * 参考实现：STARTS_WITH
     */
    private boolean referenceStartsWith(Object left, Object right) {
        if (left == null || right == null) return false;
        return left.toString().startsWith(right.toString());
    }

    /**
     * 参考实现：ENDS_WITH
     */
    private boolean referenceEndsWith(Object left, Object right) {
        if (left == null || right == null) return false;
        return left.toString().endsWith(right.toString());
    }

    // ========== Generators ==========

    @Provide
    Arbitrary<String> nonEmptyStrings() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20);
    }

    @Provide
    Arbitrary<Double> finiteDoubles() {
        return Arbitraries.doubles().between(-1_000_000, 1_000_000)
                .filter(d -> !d.isNaN() && !d.isInfinite());
    }

    @Provide
    Arbitrary<Integer> reasonableInts() {
        return Arbitraries.integers().between(-100_000, 100_000);
    }

    // ========== Property Tests: EQUALS / NOT_EQUALS ==========

    // Feature: condition-branch-refactor, Property 5: Comparison operator correctness
    // **Validates: Requirements 2.4**
    @Property(tries = 100)
    void equals_with_strings_matches_reference(
            @ForAll("nonEmptyStrings") String left,
            @ForAll("nonEmptyStrings") String right) {
        boolean actual = evaluateComparison(left, ComparisonOperator.EQUALS, right);
        boolean expected = referenceEquals(left, right);
        assert actual == expected :
                String.format("EQUALS failed: left='%s', right='%s', actual=%s, expected=%s",
                        left, right, actual, expected);
    }

    // Feature: condition-branch-refactor, Property 5: Comparison operator correctness
    // **Validates: Requirements 2.4**
    @Property(tries = 100)
    void equals_with_numbers_matches_reference(
            @ForAll("finiteDoubles") Double left,
            @ForAll("finiteDoubles") Double right) {
        boolean actual = evaluateComparison(left, ComparisonOperator.EQUALS, right);
        boolean expected = referenceEquals(left, right);
        assert actual == expected :
                String.format("EQUALS failed: left=%s, right=%s, actual=%s, expected=%s",
                        left, right, actual, expected);
    }

    // Feature: condition-branch-refactor, Property 5: Comparison operator correctness
    // **Validates: Requirements 2.4**
    @Property(tries = 100)
    void not_equals_is_negation_of_equals(
            @ForAll("nonEmptyStrings") String left,
            @ForAll("nonEmptyStrings") String right) {
        boolean equalsResult = evaluateComparison(left, ComparisonOperator.EQUALS, right);
        boolean notEqualsResult = evaluateComparison(left, ComparisonOperator.NOT_EQUALS, right);
        assert equalsResult != notEqualsResult :
                String.format("NOT_EQUALS should be negation of EQUALS: left='%s', right='%s'",
                        left, right);
    }

    // ========== Property Tests: CONTAINS / NOT_CONTAINS ==========

    // Feature: condition-branch-refactor, Property 5: Comparison operator correctness
    // **Validates: Requirements 2.4**
    @Property(tries = 100)
    void contains_with_strings_matches_reference(
            @ForAll("nonEmptyStrings") String left,
            @ForAll("nonEmptyStrings") String right) {
        boolean actual = evaluateComparison(left, ComparisonOperator.CONTAINS, right);
        boolean expected = referenceContains(left, right);
        assert actual == expected :
                String.format("CONTAINS failed: left='%s', right='%s', actual=%s, expected=%s",
                        left, right, actual, expected);
    }

    // Feature: condition-branch-refactor, Property 5: Comparison operator correctness
    // **Validates: Requirements 2.4**
    @Property(tries = 100)
    void not_contains_is_negation_of_contains(
            @ForAll("nonEmptyStrings") String left,
            @ForAll("nonEmptyStrings") String right) {
        boolean containsResult = evaluateComparison(left, ComparisonOperator.CONTAINS, right);
        boolean notContainsResult = evaluateComparison(left, ComparisonOperator.NOT_CONTAINS, right);
        assert containsResult != notContainsResult :
                String.format("NOT_CONTAINS should be negation of CONTAINS: left='%s', right='%s'",
                        left, right);
    }

    // ========== Property Tests: GREATER_THAN / LESS_THAN / GTE / LTE ==========

    // Feature: condition-branch-refactor, Property 5: Comparison operator correctness
    // **Validates: Requirements 2.4**
    @Property(tries = 100)
    void greater_than_matches_reference(
            @ForAll("finiteDoubles") Double left,
            @ForAll("finiteDoubles") Double right) {
        boolean actual = evaluateComparison(left, ComparisonOperator.GREATER_THAN, right);
        boolean expected = referenceNumericCompare(left, right) > 0;
        assert actual == expected :
                String.format("GREATER_THAN failed: left=%s, right=%s, actual=%s, expected=%s",
                        left, right, actual, expected);
    }

    // Feature: condition-branch-refactor, Property 5: Comparison operator correctness
    // **Validates: Requirements 2.4**
    @Property(tries = 100)
    void less_than_matches_reference(
            @ForAll("finiteDoubles") Double left,
            @ForAll("finiteDoubles") Double right) {
        boolean actual = evaluateComparison(left, ComparisonOperator.LESS_THAN, right);
        boolean expected = referenceNumericCompare(left, right) < 0;
        assert actual == expected :
                String.format("LESS_THAN failed: left=%s, right=%s, actual=%s, expected=%s",
                        left, right, actual, expected);
    }

    // Feature: condition-branch-refactor, Property 5: Comparison operator correctness
    // **Validates: Requirements 2.4**
    @Property(tries = 100)
    void greater_than_or_equal_matches_reference(
            @ForAll("finiteDoubles") Double left,
            @ForAll("finiteDoubles") Double right) {
        boolean actual = evaluateComparison(left, ComparisonOperator.GREATER_THAN_OR_EQUAL, right);
        boolean expected = referenceNumericCompare(left, right) >= 0;
        assert actual == expected :
                String.format("GTE failed: left=%s, right=%s, actual=%s, expected=%s",
                        left, right, actual, expected);
    }

    // Feature: condition-branch-refactor, Property 5: Comparison operator correctness
    // **Validates: Requirements 2.4**
    @Property(tries = 100)
    void less_than_or_equal_matches_reference(
            @ForAll("finiteDoubles") Double left,
            @ForAll("finiteDoubles") Double right) {
        boolean actual = evaluateComparison(left, ComparisonOperator.LESS_THAN_OR_EQUAL, right);
        boolean expected = referenceNumericCompare(left, right) <= 0;
        assert actual == expected :
                String.format("LTE failed: left=%s, right=%s, actual=%s, expected=%s",
                        left, right, actual, expected);
    }

    // Feature: condition-branch-refactor, Property 5: Comparison operator correctness
    // **Validates: Requirements 2.4**
    @Property(tries = 100)
    void numeric_comparison_consistency(
            @ForAll("finiteDoubles") Double left,
            @ForAll("finiteDoubles") Double right) {
        // GT and LTE are complementary
        boolean gt = evaluateComparison(left, ComparisonOperator.GREATER_THAN, right);
        boolean lte = evaluateComparison(left, ComparisonOperator.LESS_THAN_OR_EQUAL, right);
        assert gt != lte :
                String.format("GT and LTE should be complementary: left=%s, right=%s, GT=%s, LTE=%s",
                        left, right, gt, lte);

        // LT and GTE are complementary
        boolean lt = evaluateComparison(left, ComparisonOperator.LESS_THAN, right);
        boolean gte = evaluateComparison(left, ComparisonOperator.GREATER_THAN_OR_EQUAL, right);
        assert lt != gte :
                String.format("LT and GTE should be complementary: left=%s, right=%s, LT=%s, GTE=%s",
                        left, right, lt, gte);
    }

    // ========== Property Tests: IS_EMPTY / IS_NOT_EMPTY ==========

    // Feature: condition-branch-refactor, Property 5: Comparison operator correctness
    // **Validates: Requirements 2.4**
    @Property(tries = 100)
    void is_empty_with_non_empty_strings(@ForAll("nonEmptyStrings") String value) {
        boolean actual = evaluateComparison(value, ComparisonOperator.IS_EMPTY, null);
        boolean expected = referenceIsEmpty(value);
        assert actual == expected :
                String.format("IS_EMPTY failed for non-empty string: value='%s', actual=%s, expected=%s",
                        value, actual, expected);
    }

    // Feature: condition-branch-refactor, Property 5: Comparison operator correctness
    // **Validates: Requirements 2.4**
    @Property(tries = 100)
    void is_not_empty_is_negation_of_is_empty(@ForAll("nonEmptyStrings") String value) {
        boolean isEmpty = evaluateComparison(value, ComparisonOperator.IS_EMPTY, null);
        boolean isNotEmpty = evaluateComparison(value, ComparisonOperator.IS_NOT_EMPTY, null);
        assert isEmpty != isNotEmpty :
                String.format("IS_NOT_EMPTY should be negation of IS_EMPTY: value='%s'", value);
    }

    // Feature: condition-branch-refactor, Property 5: Comparison operator correctness
    // **Validates: Requirements 2.4**
    @Property(tries = 1)
    void is_empty_with_null_value() {
        // null 值应该被视为 empty
        boolean actual = evaluateComparisonWithNull(ComparisonOperator.IS_EMPTY);
        assert actual : "IS_EMPTY should return true for null value";
    }

    // Feature: condition-branch-refactor, Property 5: Comparison operator correctness
    // **Validates: Requirements 2.4**
    @Property(tries = 1)
    void is_empty_with_empty_string() {
        boolean actual = evaluateComparison("", ComparisonOperator.IS_EMPTY, null);
        assert actual : "IS_EMPTY should return true for empty string";
    }

    // ========== Property Tests: STARTS_WITH / ENDS_WITH ==========

    // Feature: condition-branch-refactor, Property 5: Comparison operator correctness
    // **Validates: Requirements 2.4**
    @Property(tries = 100)
    void starts_with_matches_reference(
            @ForAll("nonEmptyStrings") String left,
            @ForAll("nonEmptyStrings") String right) {
        boolean actual = evaluateComparison(left, ComparisonOperator.STARTS_WITH, right);
        boolean expected = referenceStartsWith(left, right);
        assert actual == expected :
                String.format("STARTS_WITH failed: left='%s', right='%s', actual=%s, expected=%s",
                        left, right, actual, expected);
    }

    // Feature: condition-branch-refactor, Property 5: Comparison operator correctness
    // **Validates: Requirements 2.4**
    @Property(tries = 100)
    void ends_with_matches_reference(
            @ForAll("nonEmptyStrings") String left,
            @ForAll("nonEmptyStrings") String right) {
        boolean actual = evaluateComparison(left, ComparisonOperator.ENDS_WITH, right);
        boolean expected = referenceEndsWith(left, right);
        assert actual == expected :
                String.format("ENDS_WITH failed: left='%s', right='%s', actual=%s, expected=%s",
                        left, right, actual, expected);
    }

    // Feature: condition-branch-refactor, Property 5: Comparison operator correctness
    // **Validates: Requirements 2.4**
    @Property(tries = 100)
    void starts_with_prefix_always_true(
            @ForAll("nonEmptyStrings") String prefix,
            @ForAll("nonEmptyStrings") String suffix) {
        String combined = prefix + suffix;
        boolean actual = evaluateComparison(combined, ComparisonOperator.STARTS_WITH, prefix);
        assert actual :
                String.format("STARTS_WITH should be true when left starts with right: left='%s', right='%s'",
                        combined, prefix);
    }

    // Feature: condition-branch-refactor, Property 5: Comparison operator correctness
    // **Validates: Requirements 2.4**
    @Property(tries = 100)
    void ends_with_suffix_always_true(
            @ForAll("nonEmptyStrings") String prefix,
            @ForAll("nonEmptyStrings") String suffix) {
        String combined = prefix + suffix;
        boolean actual = evaluateComparison(combined, ComparisonOperator.ENDS_WITH, suffix);
        assert actual :
                String.format("ENDS_WITH should be true when left ends with right: left='%s', right='%s'",
                        combined, suffix);
    }

    // ========== Property Tests: 跨操作符一致性 ==========

    // Feature: condition-branch-refactor, Property 5: Comparison operator correctness
    // **Validates: Requirements 2.4**
    @Property(tries = 100)
    void contains_implies_substring_relationship(
            @ForAll("nonEmptyStrings") String prefix,
            @ForAll("nonEmptyStrings") String middle,
            @ForAll("nonEmptyStrings") String suffix) {
        String combined = prefix + middle + suffix;
        boolean actual = evaluateComparison(combined, ComparisonOperator.CONTAINS, middle);
        assert actual :
                String.format("CONTAINS should be true when left contains right: left='%s', right='%s'",
                        combined, middle);
    }

    // Feature: condition-branch-refactor, Property 5: Comparison operator correctness
    // **Validates: Requirements 2.4**
    @Property(tries = 100)
    void equals_reflexive(@ForAll("nonEmptyStrings") String value) {
        boolean actual = evaluateComparison(value, ComparisonOperator.EQUALS, value);
        assert actual : String.format("EQUALS should be reflexive: value='%s'", value);
    }

    // Feature: condition-branch-refactor, Property 5: Comparison operator correctness
    // **Validates: Requirements 2.4**
    @Property(tries = 100)
    void equals_reflexive_numbers(@ForAll("finiteDoubles") Double value) {
        boolean actual = evaluateComparison(value, ComparisonOperator.EQUALS, value);
        assert actual : String.format("EQUALS should be reflexive for numbers: value=%s", value);
    }

    // Feature: condition-branch-refactor, Property 5: Comparison operator correctness
    // **Validates: Requirements 2.4**
    @Property(tries = 100)
    void numeric_integer_comparison_matches_reference(
            @ForAll("reasonableInts") Integer left,
            @ForAll("reasonableInts") Integer right) {
        boolean gtActual = evaluateComparison(left, ComparisonOperator.GREATER_THAN, right);
        boolean gtExpected = left.compareTo(right) > 0;
        assert gtActual == gtExpected :
                String.format("GREATER_THAN failed for integers: left=%d, right=%d", left, right);

        boolean ltActual = evaluateComparison(left, ComparisonOperator.LESS_THAN, right);
        boolean ltExpected = left.compareTo(right) < 0;
        assert ltActual == ltExpected :
                String.format("LESS_THAN failed for integers: left=%d, right=%d", left, right);
    }

    // ========== null 值处理的辅助方法 ==========

    /**
     * 专门用于测试 null 左操作数的场景。
     * 通过引用一个不存在的 key 让 resolveOperand 返回 null。
     */
    private boolean evaluateComparisonWithNull(ComparisonOperator op) {
        ExecutionContext context = ExecutionContext.builder().build();
        // 不设置任何 node output，引用不存在的节点让 resolveOperand 返回 null

        ConditionItem item = ConditionItem.builder()
                .leftOperand("nodes.nonExistent.value")
                .operator(op)
                .rightOperand(null)
                .build();

        ConditionGroup group = ConditionGroup.builder()
                .operator(LogicalOperator.AND)
                .conditions(List.of(item))
                .build();

        ConditionBranch matchBranch = ConditionBranch.builder()
                .priority(0)
                .targetNodeId("target")
                .description("test branch")
                .isDefault(false)
                .conditionGroups(List.of(group))
                .build();

        ConditionBranch defaultBranch = ConditionBranch.builder()
                .priority(1)
                .targetNodeId("default")
                .description("default branch")
                .isDefault(true)
                .conditionGroups(List.of())
                .build();

        ConditionBranch result = evaluator.evaluate(List.of(matchBranch, defaultBranch), context);
        return "target".equals(result.getTargetNodeId());
    }
}
