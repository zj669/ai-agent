package com.zj.aiagent.infrastructure.workflow.condition;

import com.zj.aiagent.domain.workflow.valobj.ComparisonOperator;
import com.zj.aiagent.domain.workflow.valobj.ConditionBranch;
import com.zj.aiagent.domain.workflow.valobj.ConditionGroup;
import com.zj.aiagent.domain.workflow.valobj.ConditionItem;
import com.zj.aiagent.domain.workflow.valobj.ExecutionContext;
import com.zj.aiagent.domain.workflow.valobj.LogicalOperator;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Property-Based Test: 多组 AND 组合
 *
 * // Feature: condition-branch-refactor, Property 4: Multi-group AND combination
 * **Validates: Requirements 2.5**
 *
 * 验证：
 * 对于任意 ConditionBranch 包含多个 ConditionGroups，
 * 该分支命中当且仅当每个 ConditionGroup 都评估为 true。
 * （即多个 ConditionGroup 之间为 AND 关系）
 *
 * 测试策略：
 * - 生成 2~5 个 ConditionGroup，每个 group 有已知的 true/false 评估结果
 * - 每个 group 使用 AND 逻辑操作符，包含 1~3 个 ConditionItem
 * - 通过控制 ExecutionContext 中的值来确定每个 group 的评估结果
 * - 构造一个非 default 分支（包含多个条件组）+ 一个 default 分支
 * - 如果 evaluate 返回非 default 分支，说明所有组都为 true（分支命中）
 * - 如果 evaluate 返回 default 分支，说明至少一个组为 false（分支未命中）
 */
class MultiGroupAndCombinationPropertyTest {

    private final StructuredConditionEvaluator evaluator = new StructuredConditionEvaluator();

    // ========== 辅助方法 ==========

    /**
     * 通过 evaluate() 间接测试多组 AND 组合逻辑。
     * 构造一个包含多个条件组的非 default 分支 + default 分支，
     * 评估后判断是否命中非 default 分支。
     *
     * @param groups  要测试的条件组列表
     * @param context 执行上下文
     * @return true 表示分支命中（所有组都为 true），false 表示分支未命中
     */
    private boolean evaluateBranch(List<ConditionGroup> groups, ExecutionContext context) {
        ConditionBranch matchBranch = ConditionBranch.builder()
                .priority(0)
                .targetNodeId("match")
                .description("test branch with multiple groups")
                .isDefault(false)
                .conditionGroups(groups)
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
     * 为指定的 group 索引创建一个已知评估结果的 ConditionGroup。
     * 每个 group 使用 AND 逻辑，包含一个 ConditionItem。
     *
     * - shouldBeTrue=true: group 内条件项的左操作数值等于右操作数 → group 为 true
     * - shouldBeTrue=false: group 内条件项的左操作数值不等于右操作数 → group 为 false
     *
     * @param groupIndex  组索引（用于生成唯一的 nodeId）
     * @param shouldBeTrue 该组是否应评估为 true
     * @return 条件组
     */
    private ConditionGroup createKnownGroup(int groupIndex, boolean shouldBeTrue) {
        String leftOperand = "nodes.group_" + groupIndex + ".result";

        ConditionItem item = ConditionItem.builder()
                .leftOperand(leftOperand)
                .operator(ComparisonOperator.EQUALS)
                .rightOperand("expected_value")
                .build();

        return ConditionGroup.builder()
                .operator(LogicalOperator.AND)
                .conditions(List.of(item))
                .build();
    }

    /**
     * 构造 ExecutionContext，为每个 group 设置对应的 node output 值。
     * - shouldBeTrue=true 的 group: 值为 "expected_value"（与右操作数匹配）
     * - shouldBeTrue=false 的 group: 值为 "wrong_value"（与右操作数不匹配）
     *
     * @param groupResults 每个 group 的期望评估结果
     * @return 配置好的 ExecutionContext
     */
    private ExecutionContext createContext(List<Boolean> groupResults) {
        ExecutionContext context = ExecutionContext.builder().build();
        for (int i = 0; i < groupResults.size(); i++) {
            String nodeId = "group_" + i;
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("result", groupResults.get(i) ? "expected_value" : "wrong_value");
            context.setNodeOutput(nodeId, outputs);
        }
        return context;
    }

    /**
     * 根据布尔值列表创建对应的条件组列表
     */
    private List<ConditionGroup> createGroups(List<Boolean> groupResults) {
        List<ConditionGroup> groups = new ArrayList<>();
        for (int i = 0; i < groupResults.size(); i++) {
            groups.add(createKnownGroup(i, groupResults.get(i)));
        }
        return groups;
    }

    // ========== Generators ==========

    /**
     * 生成 2~5 个布尔值的列表，表示每个 group 的期望评估结果
     */
    @Provide
    Arbitrary<List<Boolean>> groupResultLists() {
        return Arbitraries.of(true, false).list().ofMinSize(2).ofMaxSize(5);
    }

    /**
     * 生成全部为 true 的 group 结果列表（2~5 个）
     */
    @Provide
    Arbitrary<List<Boolean>> allTrueGroupLists() {
        return Arbitraries.integers().between(2, 5)
                .map(size -> IntStream.range(0, size)
                        .mapToObj(i -> true)
                        .collect(Collectors.toList()));
    }

    /**
     * 生成全部为 false 的 group 结果列表（2~5 个）
     */
    @Provide
    Arbitrary<List<Boolean>> allFalseGroupLists() {
        return Arbitraries.integers().between(2, 5)
                .map(size -> IntStream.range(0, size)
                        .mapToObj(i -> false)
                        .collect(Collectors.toList()));
    }

    /**
     * 生成至少包含一个 false 的 group 结果列表（2~5 个）
     */
    @Provide
    Arbitrary<List<Boolean>> mixedWithAtLeastOneFalse() {
        return Arbitraries.of(true, false).list().ofMinSize(2).ofMaxSize(5)
                .filter(list -> list.stream().anyMatch(b -> !b));
    }

    // ========== Property Tests: 核心 AND 组合逻辑 ==========

    // Feature: condition-branch-refactor, Property 4: Multi-group AND combination
    // **Validates: Requirements 2.5**
    @Property(tries = 100)
    void branch_matches_iff_all_groups_true(@ForAll("groupResultLists") List<Boolean> groupResults) {
        List<ConditionGroup> groups = createGroups(groupResults);
        ExecutionContext context = createContext(groupResults);

        boolean actual = evaluateBranch(groups, context);
        boolean expected = groupResults.stream().allMatch(b -> b);

        assert actual == expected :
                String.format("Multi-group AND: groupResults=%s, actual=%s, expected=%s",
                        groupResults, actual, expected);
    }

    // Feature: condition-branch-refactor, Property 4: Multi-group AND combination
    // **Validates: Requirements 2.5**
    @Property(tries = 100)
    void branch_with_all_true_groups_matches(@ForAll("allTrueGroupLists") List<Boolean> groupResults) {
        List<ConditionGroup> groups = createGroups(groupResults);
        ExecutionContext context = createContext(groupResults);

        boolean actual = evaluateBranch(groups, context);

        assert actual :
                String.format("Branch with all true groups should match: groupCount=%d",
                        groupResults.size());
    }

    // Feature: condition-branch-refactor, Property 4: Multi-group AND combination
    // **Validates: Requirements 2.5**
    @Property(tries = 100)
    void branch_with_all_false_groups_does_not_match(@ForAll("allFalseGroupLists") List<Boolean> groupResults) {
        List<ConditionGroup> groups = createGroups(groupResults);
        ExecutionContext context = createContext(groupResults);

        boolean actual = evaluateBranch(groups, context);

        assert !actual :
                String.format("Branch with all false groups should not match: groupCount=%d",
                        groupResults.size());
    }

    // Feature: condition-branch-refactor, Property 4: Multi-group AND combination
    // **Validates: Requirements 2.5**
    @Property(tries = 100)
    void branch_with_mixed_groups_does_not_match(
            @ForAll("mixedWithAtLeastOneFalse") List<Boolean> groupResults) {
        List<ConditionGroup> groups = createGroups(groupResults);
        ExecutionContext context = createContext(groupResults);

        boolean actual = evaluateBranch(groups, context);

        assert !actual :
                String.format("Branch with at least one false group should not match: groupResults=%s",
                        groupResults);
    }

    // ========== Property Tests: 单个 false group 足以使分支不匹配 ==========

    // Feature: condition-branch-refactor, Property 4: Multi-group AND combination
    // **Validates: Requirements 2.5**
    @Property(tries = 100)
    void single_false_group_prevents_match(@ForAll @IntRange(min = 2, max = 5) int groupCount,
                                           @ForAll @IntRange(min = 0, max = 4) int falseIndex) {
        // 确保 falseIndex 在有效范围内
        int actualFalseIndex = falseIndex % groupCount;

        // 构造全 true 列表，然后将一个位置设为 false
        List<Boolean> groupResults = new ArrayList<>(
                IntStream.range(0, groupCount)
                        .mapToObj(i -> true)
                        .collect(Collectors.toList()));
        groupResults.set(actualFalseIndex, false);

        List<ConditionGroup> groups = createGroups(groupResults);
        ExecutionContext context = createContext(groupResults);

        boolean actual = evaluateBranch(groups, context);

        assert !actual :
                String.format("Single false group at index %d should prevent match: groupResults=%s",
                        actualFalseIndex, groupResults);
    }

    // ========== Property Tests: 多条件项 group 的 AND 组合 ==========

    // Feature: condition-branch-refactor, Property 4: Multi-group AND combination
    // **Validates: Requirements 2.5**
    @Property(tries = 100)
    void multi_item_groups_and_combination(
            @ForAll @IntRange(min = 2, max = 4) int groupCount) {
        // 每个 group 包含 2 个条件项（全部为 true），验证多条件项 group 的 AND 组合
        ExecutionContext context = ExecutionContext.builder().build();
        List<ConditionGroup> groups = new ArrayList<>();

        for (int g = 0; g < groupCount; g++) {
            String nodeId = "multiNode_" + g;
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("key1", "val1");
            outputs.put("key2", "val2");
            context.setNodeOutput(nodeId, outputs);

            List<ConditionItem> items = List.of(
                    ConditionItem.builder()
                            .leftOperand("nodes." + nodeId + ".key1")
                            .operator(ComparisonOperator.EQUALS)
                            .rightOperand("val1")
                            .build(),
                    ConditionItem.builder()
                            .leftOperand("nodes." + nodeId + ".key2")
                            .operator(ComparisonOperator.EQUALS)
                            .rightOperand("val2")
                            .build()
            );

            groups.add(ConditionGroup.builder()
                    .operator(LogicalOperator.AND)
                    .conditions(items)
                    .build());
        }

        boolean actual = evaluateBranch(groups, context);

        assert actual :
                String.format("All groups with all-true multi-item conditions should match: groupCount=%d",
                        groupCount);
    }

    // Feature: condition-branch-refactor, Property 4: Multi-group AND combination
    // **Validates: Requirements 2.5**
    @Property(tries = 100)
    void mixed_operator_groups_and_combination(@ForAll("groupResultLists") List<Boolean> groupResults) {
        // 使用混合的 LogicalOperator（AND 和 OR）构造 groups，
        // 但每个 group 的最终评估结果由 groupResults 控制
        ExecutionContext context = ExecutionContext.builder().build();
        List<ConditionGroup> groups = new ArrayList<>();

        for (int i = 0; i < groupResults.size(); i++) {
            boolean shouldBeTrue = groupResults.get(i);
            String nodeId = "mixedNode_" + i;

            if (i % 2 == 0) {
                // 偶数索引 group 使用 AND 逻辑
                Map<String, Object> outputs = new HashMap<>();
                outputs.put("val", shouldBeTrue ? "match" : "no_match");
                context.setNodeOutput(nodeId, outputs);

                ConditionItem item = ConditionItem.builder()
                        .leftOperand("nodes." + nodeId + ".val")
                        .operator(ComparisonOperator.EQUALS)
                        .rightOperand("match")
                        .build();

                groups.add(ConditionGroup.builder()
                        .operator(LogicalOperator.AND)
                        .conditions(List.of(item))
                        .build());
            } else {
                // 奇数索引 group 使用 OR 逻辑
                Map<String, Object> outputs = new HashMap<>();
                outputs.put("val", shouldBeTrue ? "match" : "no_match");
                context.setNodeOutput(nodeId, outputs);

                ConditionItem trueItem = ConditionItem.builder()
                        .leftOperand("nodes." + nodeId + ".val")
                        .operator(ComparisonOperator.EQUALS)
                        .rightOperand("match")
                        .build();

                // 添加一个始终为 false 的条件项，OR 逻辑下不影响结果
                ConditionItem falseItem = ConditionItem.builder()
                        .leftOperand("nodes." + nodeId + ".val")
                        .operator(ComparisonOperator.EQUALS)
                        .rightOperand("never_match_value")
                        .build();

                groups.add(ConditionGroup.builder()
                        .operator(LogicalOperator.OR)
                        .conditions(List.of(trueItem, falseItem))
                        .build());
            }
        }

        boolean actual = evaluateBranch(groups, context);
        boolean expected = groupResults.stream().allMatch(b -> b);

        assert actual == expected :
                String.format("Mixed operator groups AND combination: groupResults=%s, actual=%s, expected=%s",
                        groupResults, actual, expected);
    }
}
