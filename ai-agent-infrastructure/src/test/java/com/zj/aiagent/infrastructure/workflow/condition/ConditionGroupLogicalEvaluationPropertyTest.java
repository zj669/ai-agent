package com.zj.aiagent.infrastructure.workflow.condition;

import com.zj.aiagent.domain.workflow.valobj.ComparisonOperator;
import com.zj.aiagent.domain.workflow.valobj.ConditionBranch;
import com.zj.aiagent.domain.workflow.valobj.ConditionGroup;
import com.zj.aiagent.domain.workflow.valobj.ConditionItem;
import com.zj.aiagent.domain.workflow.valobj.ExecutionContext;
import com.zj.aiagent.domain.workflow.valobj.LogicalOperator;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Property-Based Test: 条件组逻辑评估
 *
 * // Feature: condition-branch-refactor, Property 3: Condition group logical evaluation
 * **Validates: Requirements 2.2, 2.3**
 *
 * 验证：
 * - 对于任意 ConditionGroup（operator=AND）和一组 ConditionItems，
 *   组评估结果为 true 当且仅当所有 items 都为 true。
 * - 对于任意 ConditionGroup（operator=OR）和一组 ConditionItems，
 *   组评估结果为 true 当且仅当至少一个 item 为 true。
 *
 * 测试策略：
 * - 使用 EQUALS 比较操作符构造已知 true/false 结果的条件项
 * - 将已知值放入 ExecutionContext 的 node output 中
 * - 通过 evaluate() 公共方法间接测试 evaluateGroup（因为 evaluateGroup 是 private）
 * - 构造方式：一个非 default 分支（包含单个条件组）+ 一个 default 分支
 *   如果 evaluate 返回非 default 分支，说明条件组评估为 true；否则为 false
 */
class ConditionGroupLogicalEvaluationPropertyTest {

    private final StructuredConditionEvaluator evaluator = new StructuredConditionEvaluator();

    // ========== 辅助方法 ==========

    /**
     * 通过 evaluate() 间接测试条件组的逻辑评估。
     * 构造一个包含单个条件组的非 default 分支 + default 分支，
     * 评估后判断是否命中非 default 分支（即条件组评估为 true）。
     *
     * @param group   要测试的条件组
     * @param context 执行上下文（包含条件项引用的值）
     * @return true 表示条件组评估为 true，false 表示评估为 false
     */
    private boolean evaluateGroup(ConditionGroup group, ExecutionContext context) {
        ConditionBranch matchBranch = ConditionBranch.builder()
                .priority(0)
                .targetNodeId("match")
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

        ConditionBranch result = evaluator.evaluate(
                List.of(matchBranch, defaultBranch), context);
        return "match".equals(result.getTargetNodeId());
    }

    /**
     * 构造一个已知评估结果的 ConditionItem。
     * 使用 EQUALS 操作符：
     * - 如果 shouldBeTrue=true，左操作数引用的值等于右操作数 → 条件为 true
     * - 如果 shouldBeTrue=false，左操作数引用的值不等于右操作数 → 条件为 false
     *
     * @param index       条件项索引（用于生成唯一的 key）
     * @param shouldBeTrue 该条件项是否应评估为 true
     * @return 条件项
     */
    private ConditionItem createKnownItem(int index, boolean shouldBeTrue) {
        String key = "val_" + index;
        String leftOperand = "nodes.testNode." + key;

        if (shouldBeTrue) {
            // 左操作数引用的值 = "match_value"，右操作数也是 "match_value" → EQUALS 为 true
            return ConditionItem.builder()
                    .leftOperand(leftOperand)
                    .operator(ComparisonOperator.EQUALS)
                    .rightOperand("match_value")
                    .build();
        } else {
            // 左操作数引用的值 = "actual_value"，右操作数是 "different_value" → EQUALS 为 false
            return ConditionItem.builder()
                    .leftOperand(leftOperand)
                    .operator(ComparisonOperator.EQUALS)
                    .rightOperand("different_value")
                    .build();
        }
    }

    /**
     * 构造 ExecutionContext，为每个条件项设置对应的 node output 值。
     * - shouldBeTrue=true 的条件项：值为 "match_value"
     * - shouldBeTrue=false 的条件项：值为 "actual_value"
     *
     * @param booleans 每个条件项的期望评估结果
     * @return 配置好的 ExecutionContext
     */
    private ExecutionContext createContext(List<Boolean> booleans) {
        ExecutionContext context = ExecutionContext.builder().build();
        Map<String, Object> outputs = new java.util.HashMap<>();
        for (int i = 0; i < booleans.size(); i++) {
            String key = "val_" + i;
            outputs.put(key, booleans.get(i) ? "match_value" : "actual_value");
        }
        context.setNodeOutput("testNode", outputs);
        return context;
    }

    /**
     * 构造条件组
     */
    private ConditionGroup createGroup(LogicalOperator operator, List<Boolean> booleans) {
        List<ConditionItem> items = new ArrayList<>();
        for (int i = 0; i < booleans.size(); i++) {
            items.add(createKnownItem(i, booleans.get(i)));
        }
        return ConditionGroup.builder()
                .operator(operator)
                .conditions(items)
                .build();
    }

    // ========== Generators ==========

    /**
     * 基础布尔值 Arbitrary
     */
    private Arbitrary<Boolean> booleanArbitrary() {
        return Arbitraries.of(true, false);
    }

    /**
     * 生成 1~10 个布尔值的列表，表示每个条件项的期望评估结果
     */
    @Provide
    Arbitrary<List<Boolean>> booleanLists() {
        return booleanArbitrary().list().ofMinSize(1).ofMaxSize(10);
    }

    /**
     * 生成至少包含一个 true 的布尔值列表（用于验证 OR 的 true 场景）
     */
    @Provide
    Arbitrary<List<Boolean>> booleanListsWithAtLeastOneTrue() {
        return booleanArbitrary().list().ofMinSize(1).ofMaxSize(10)
                .filter(list -> list.stream().anyMatch(b -> b));
    }

    /**
     * 生成全部为 false 的布尔值列表（用于验证 OR 的 false 场景）
     */
    @Provide
    Arbitrary<List<Boolean>> allFalseLists() {
        return Arbitraries.integers().between(1, 10)
                .map(size -> IntStream.range(0, size)
                        .mapToObj(i -> false)
                        .collect(java.util.stream.Collectors.toList()));
    }

    /**
     * 生成全部为 true 的布尔值列表（用于验证 AND 的 true 场景）
     */
    @Provide
    Arbitrary<List<Boolean>> allTrueLists() {
        return Arbitraries.integers().between(1, 10)
                .map(size -> IntStream.range(0, size)
                        .mapToObj(i -> true)
                        .collect(java.util.stream.Collectors.toList()));
    }

    /**
     * 生成至少包含一个 false 的布尔值列表（用于验证 AND 的 false 场景）
     */
    @Provide
    Arbitrary<List<Boolean>> booleanListsWithAtLeastOneFalse() {
        return booleanArbitrary().list().ofMinSize(1).ofMaxSize(10)
                .filter(list -> list.stream().anyMatch(b -> !b));
    }

    // ========== Property Tests: AND 逻辑 ==========

    // Feature: condition-branch-refactor, Property 3: Condition group logical evaluation
    // **Validates: Requirements 2.2, 2.3**
    @Property(tries = 100)
    void and_group_evaluates_to_true_iff_all_items_true(@ForAll("booleanLists") List<Boolean> booleans) {
        ConditionGroup group = createGroup(LogicalOperator.AND, booleans);
        ExecutionContext context = createContext(booleans);

        boolean actual = evaluateGroup(group, context);
        boolean expected = booleans.stream().allMatch(b -> b);

        assert actual == expected :
                String.format("AND group: booleans=%s, actual=%s, expected=%s",
                        booleans, actual, expected);
    }

    // Feature: condition-branch-refactor, Property 3: Condition group logical evaluation
    // **Validates: Requirements 2.2, 2.3**
    @Property(tries = 100)
    void and_group_with_all_true_evaluates_to_true(@ForAll("allTrueLists") List<Boolean> booleans) {
        ConditionGroup group = createGroup(LogicalOperator.AND, booleans);
        ExecutionContext context = createContext(booleans);

        boolean actual = evaluateGroup(group, context);

        assert actual :
                String.format("AND group with all true should evaluate to true: size=%d", booleans.size());
    }

    // Feature: condition-branch-refactor, Property 3: Condition group logical evaluation
    // **Validates: Requirements 2.2, 2.3**
    @Property(tries = 100)
    void and_group_with_at_least_one_false_evaluates_to_false(
            @ForAll("booleanListsWithAtLeastOneFalse") List<Boolean> booleans) {
        ConditionGroup group = createGroup(LogicalOperator.AND, booleans);
        ExecutionContext context = createContext(booleans);

        boolean actual = evaluateGroup(group, context);

        assert !actual :
                String.format("AND group with at least one false should evaluate to false: booleans=%s",
                        booleans);
    }

    // ========== Property Tests: OR 逻辑 ==========

    // Feature: condition-branch-refactor, Property 3: Condition group logical evaluation
    // **Validates: Requirements 2.2, 2.3**
    @Property(tries = 100)
    void or_group_evaluates_to_true_iff_at_least_one_item_true(@ForAll("booleanLists") List<Boolean> booleans) {
        ConditionGroup group = createGroup(LogicalOperator.OR, booleans);
        ExecutionContext context = createContext(booleans);

        boolean actual = evaluateGroup(group, context);
        boolean expected = booleans.stream().anyMatch(b -> b);

        assert actual == expected :
                String.format("OR group: booleans=%s, actual=%s, expected=%s",
                        booleans, actual, expected);
    }

    // Feature: condition-branch-refactor, Property 3: Condition group logical evaluation
    // **Validates: Requirements 2.2, 2.3**
    @Property(tries = 100)
    void or_group_with_at_least_one_true_evaluates_to_true(
            @ForAll("booleanListsWithAtLeastOneTrue") List<Boolean> booleans) {
        ConditionGroup group = createGroup(LogicalOperator.OR, booleans);
        ExecutionContext context = createContext(booleans);

        boolean actual = evaluateGroup(group, context);

        assert actual :
                String.format("OR group with at least one true should evaluate to true: booleans=%s",
                        booleans);
    }

    // Feature: condition-branch-refactor, Property 3: Condition group logical evaluation
    // **Validates: Requirements 2.2, 2.3**
    @Property(tries = 100)
    void or_group_with_all_false_evaluates_to_false(@ForAll("allFalseLists") List<Boolean> booleans) {
        ConditionGroup group = createGroup(LogicalOperator.OR, booleans);
        ExecutionContext context = createContext(booleans);

        boolean actual = evaluateGroup(group, context);

        assert !actual :
                String.format("OR group with all false should evaluate to false: size=%d", booleans.size());
    }

    // ========== Property Tests: AND 与 OR 的对偶性 ==========

    // Feature: condition-branch-refactor, Property 3: Condition group logical evaluation
    // **Validates: Requirements 2.2, 2.3**
    @Property(tries = 100)
    void and_or_duality_single_item(@ForAll boolean itemValue) {
        // 单个条件项时，AND 和 OR 的结果应该相同
        List<Boolean> booleans = List.of(itemValue);

        ConditionGroup andGroup = createGroup(LogicalOperator.AND, booleans);
        ConditionGroup orGroup = createGroup(LogicalOperator.OR, booleans);
        ExecutionContext context = createContext(booleans);

        boolean andResult = evaluateGroup(andGroup, context);
        boolean orResult = evaluateGroup(orGroup, context);

        assert andResult == orResult :
                String.format("Single item: AND and OR should agree: itemValue=%s, AND=%s, OR=%s",
                        itemValue, andResult, orResult);
        assert andResult == itemValue :
                String.format("Single item: result should equal item value: itemValue=%s, result=%s",
                        itemValue, andResult);
    }

    // Feature: condition-branch-refactor, Property 3: Condition group logical evaluation
    // **Validates: Requirements 2.2, 2.3**
    @Property(tries = 100)
    void and_implies_or(@ForAll("booleanLists") List<Boolean> booleans) {
        // 如果 AND 为 true，则 OR 也必须为 true（AND 是 OR 的充分条件）
        ConditionGroup andGroup = createGroup(LogicalOperator.AND, booleans);
        ConditionGroup orGroup = createGroup(LogicalOperator.OR, booleans);
        ExecutionContext context = createContext(booleans);

        boolean andResult = evaluateGroup(andGroup, context);
        boolean orResult = evaluateGroup(orGroup, context);

        if (andResult) {
            assert orResult :
                    String.format("If AND is true, OR must also be true: booleans=%s", booleans);
        }
    }

    // ========== Property Tests: 不同组大小 ==========

    // Feature: condition-branch-refactor, Property 3: Condition group logical evaluation
    // **Validates: Requirements 2.2, 2.3**
    @Property(tries = 100)
    void and_group_varying_sizes(@ForAll @IntRange(min = 1, max = 15) int size) {
        // 生成指定大小的全 true 列表，验证 AND 为 true
        List<Boolean> allTrue = IntStream.range(0, size)
                .mapToObj(i -> true)
                .collect(java.util.stream.Collectors.toList());

        ConditionGroup group = createGroup(LogicalOperator.AND, allTrue);
        ExecutionContext context = createContext(allTrue);

        boolean actual = evaluateGroup(group, context);
        assert actual :
                String.format("AND group with %d all-true items should be true", size);

        // 将最后一个改为 false，验证 AND 为 false
        List<Boolean> withOneFalse = new ArrayList<>(allTrue);
        withOneFalse.set(size - 1, false);

        ConditionGroup groupWithFalse = createGroup(LogicalOperator.AND, withOneFalse);
        ExecutionContext contextWithFalse = createContext(withOneFalse);

        boolean actualWithFalse = evaluateGroup(groupWithFalse, contextWithFalse);
        assert !actualWithFalse :
                String.format("AND group with %d items (one false) should be false", size);
    }

    // Feature: condition-branch-refactor, Property 3: Condition group logical evaluation
    // **Validates: Requirements 2.2, 2.3**
    @Property(tries = 100)
    void or_group_varying_sizes(@ForAll @IntRange(min = 1, max = 15) int size) {
        // 生成指定大小的全 false 列表，验证 OR 为 false
        List<Boolean> allFalse = IntStream.range(0, size)
                .mapToObj(i -> false)
                .collect(java.util.stream.Collectors.toList());

        ConditionGroup group = createGroup(LogicalOperator.OR, allFalse);
        ExecutionContext context = createContext(allFalse);

        boolean actual = evaluateGroup(group, context);
        assert !actual :
                String.format("OR group with %d all-false items should be false", size);

        // 将第一个改为 true，验证 OR 为 true
        List<Boolean> withOneTrue = new ArrayList<>(allFalse);
        withOneTrue.set(0, true);

        ConditionGroup groupWithTrue = createGroup(LogicalOperator.OR, withOneTrue);
        ExecutionContext contextWithTrue = createContext(withOneTrue);

        boolean actualWithTrue = evaluateGroup(groupWithTrue, contextWithTrue);
        assert actualWithTrue :
                String.format("OR group with %d items (one true) should be true", size);
    }
}
