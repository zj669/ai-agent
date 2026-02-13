package com.zj.aiagent.infrastructure.workflow.condition;

import com.zj.aiagent.domain.workflow.valobj.ComparisonOperator;
import com.zj.aiagent.domain.workflow.valobj.ConditionBranch;
import com.zj.aiagent.domain.workflow.valobj.ConditionGroup;
import com.zj.aiagent.domain.workflow.valobj.ConditionItem;
import com.zj.aiagent.domain.workflow.valobj.ExecutionContext;
import com.zj.aiagent.domain.workflow.valobj.LogicalOperator;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Property-Based Test: 分支评估优先级
 *
 * // Feature: condition-branch-refactor, Property 1: Branch evaluation selects first matching by priority
 * **Validates: Requirements 1.2, 1.4, 5.2**
 *
 * 验证：
 * 对于任意有序的 ConditionBranch 列表（恰好一个 default）和任意 ExecutionContext，
 * Condition_Evaluator 应返回最低优先级的匹配非 default 分支；
 * 如果没有非 default 分支匹配，则返回 default 分支。
 *
 * 测试策略：
 * - 使用 EQUALS 操作符和已知值控制哪些分支匹配、哪些不匹配
 * - 生成多个分支，每个分支有不同的优先级和匹配状态
 * - 验证评估器总是选择最低优先级的匹配分支
 * - 测试分支以随机顺序传入时，仍按优先级选择（不受插入顺序影响）
 */
class BranchEvaluationPriorityPropertyTest {

    private final StructuredConditionEvaluator evaluator = new StructuredConditionEvaluator();

    // ========== 常量 ==========

    private static final String CONTEXT_NODE_ID = "testNode";
    private static final String MATCH_VALUE = "match_value";
    private static final String MISMATCH_VALUE = "mismatch_value";
    private static final String ACTUAL_VALUE = "actual_value";

    // ========== 辅助方法 ==========

    /**
     * 创建一个条件分支，通过 shouldMatch 控制是否匹配。
     * 使用 EQUALS 操作符：左操作数引用 context 中的值，右操作数为已知字面值。
     * - shouldMatch=true: 右操作数 = MATCH_VALUE（与 context 中的值相同）→ 匹配
     * - shouldMatch=false: 右操作数 = MISMATCH_VALUE（与 context 中的值不同）→ 不匹配
     */
    private ConditionBranch createBranch(int priority, String targetNodeId,
                                          boolean shouldMatch, String conditionKey) {
        String rightOperand = shouldMatch ? MATCH_VALUE : MISMATCH_VALUE;

        ConditionItem item = ConditionItem.builder()
                .leftOperand("nodes." + CONTEXT_NODE_ID + "." + conditionKey)
                .operator(ComparisonOperator.EQUALS)
                .rightOperand(rightOperand)
                .build();

        ConditionGroup group = ConditionGroup.builder()
                .operator(LogicalOperator.AND)
                .conditions(List.of(item))
                .build();

        return ConditionBranch.builder()
                .priority(priority)
                .targetNodeId(targetNodeId)
                .description("Branch priority=" + priority + " match=" + shouldMatch)
                .isDefault(false)
                .conditionGroups(List.of(group))
                .build();
    }

    /**
     * 创建 default 分支
     */
    private ConditionBranch createDefaultBranch(int priority) {
        return ConditionBranch.builder()
                .priority(priority)
                .targetNodeId("default_target")
                .description("Default branch")
                .isDefault(true)
                .conditionGroups(List.of())
                .build();
    }

    /**
     * 构造 ExecutionContext，所有条件 key 的值都设为 MATCH_VALUE。
     * 这样 shouldMatch=true 的分支会匹配，shouldMatch=false 的不会。
     */
    private ExecutionContext createContext(int numKeys) {
        Map<String, Object> outputs = new HashMap<>();
        for (int i = 0; i < numKeys; i++) {
            outputs.put("key_" + i, MATCH_VALUE);
        }
        ExecutionContext context = ExecutionContext.builder().build();
        context.setNodeOutput(CONTEXT_NODE_ID, outputs);
        return context;
    }

    /**
     * 根据匹配状态列表，计算期望的评估结果。
     * 返回最低优先级的匹配分支的 targetNodeId，如果无匹配则返回 "default_target"。
     *
     * @param priorities    各分支的优先级
     * @param matchStatuses 各分支是否匹配
     * @return 期望的 targetNodeId
     */
    private String computeExpectedTarget(List<Integer> priorities, List<Boolean> matchStatuses) {
        int bestPriority = Integer.MAX_VALUE;
        String bestTarget = null;

        for (int i = 0; i < priorities.size(); i++) {
            if (matchStatuses.get(i) && priorities.get(i) < bestPriority) {
                bestPriority = priorities.get(i);
                bestTarget = "target_" + i;
            }
        }

        return bestTarget != null ? bestTarget : "default_target";
    }

    // ========== Generators ==========

    /**
     * 生成 2~8 个不重复的优先级值（非 default 分支数量）
     */
    @Provide
    Arbitrary<List<Integer>> uniquePriorities() {
        return Arbitraries.integers().between(0, 100)
                .set().ofMinSize(2).ofMaxSize(8)
                .map(set -> new ArrayList<>(set));
    }

    /**
     * 生成匹配状态列表（至少一个 true），长度由外部控制
     */
    private Arbitrary<List<Boolean>> matchStatusesWithAtLeastOneTrue(int size) {
        return Arbitraries.of(true, false).list().ofSize(size)
                .filter(list -> list.stream().anyMatch(b -> b));
    }

    /**
     * 生成全 false 的匹配状态列表
     */
    private List<Boolean> allFalseStatuses(int size) {
        return IntStream.range(0, size)
                .mapToObj(i -> false)
                .collect(Collectors.toList());
    }

    // ========== Property Tests ==========

    // Feature: condition-branch-refactor, Property 1: Branch evaluation selects first matching by priority
    // **Validates: Requirements 1.2, 1.4, 5.2**
    @Property(tries = 100)
    void multiple_matching_branches_selects_lowest_priority(
            @ForAll("uniquePriorities") List<Integer> priorities) {

        int branchCount = priorities.size();
        // 所有分支都匹配，应选择最低优先级的
        List<Boolean> matchStatuses = IntStream.range(0, branchCount)
                .mapToObj(i -> true)
                .collect(Collectors.toList());

        List<ConditionBranch> branches = new ArrayList<>();
        for (int i = 0; i < branchCount; i++) {
            branches.add(createBranch(priorities.get(i), "target_" + i, true, "key_" + i));
        }
        // 添加 default 分支，优先级设为最大
        branches.add(createDefaultBranch(Integer.MAX_VALUE));

        ExecutionContext context = createContext(branchCount);

        ConditionBranch result = evaluator.evaluate(branches, context);

        // 期望：最低优先级的分支
        String expectedTarget = computeExpectedTarget(priorities, matchStatuses);
        assert expectedTarget.equals(result.getTargetNodeId()) :
                String.format("All matching: priorities=%s, expected=%s, actual=%s",
                        priorities, expectedTarget, result.getTargetNodeId());
    }

    // Feature: condition-branch-refactor, Property 1: Branch evaluation selects first matching by priority
    // **Validates: Requirements 1.2, 1.4, 5.2**
    @Property(tries = 100)
    void no_matching_branches_returns_default(
            @ForAll @IntRange(min = 1, max = 8) int branchCount) {

        List<ConditionBranch> branches = new ArrayList<>();
        for (int i = 0; i < branchCount; i++) {
            // 所有非 default 分支都不匹配
            branches.add(createBranch(i, "target_" + i, false, "key_" + i));
        }
        branches.add(createDefaultBranch(Integer.MAX_VALUE));

        ExecutionContext context = createContext(branchCount);

        ConditionBranch result = evaluator.evaluate(branches, context);

        assert result.isDefault() :
                String.format("No matching branches should return default, but got targetNodeId=%s",
                        result.getTargetNodeId());
        assert "default_target".equals(result.getTargetNodeId()) :
                String.format("Default branch targetNodeId should be 'default_target', got '%s'",
                        result.getTargetNodeId());
    }

    // Feature: condition-branch-refactor, Property 1: Branch evaluation selects first matching by priority
    // **Validates: Requirements 1.2, 1.4, 5.2**
    @Property(tries = 100)
    void random_order_branches_still_selects_by_priority(
            @ForAll("uniquePriorities") List<Integer> priorities,
            @ForAll Random random) {

        int branchCount = priorities.size();
        // 所有分支都匹配
        List<ConditionBranch> branches = new ArrayList<>();
        for (int i = 0; i < branchCount; i++) {
            branches.add(createBranch(priorities.get(i), "target_" + i, true, "key_" + i));
        }
        branches.add(createDefaultBranch(Integer.MAX_VALUE));

        // 随机打乱分支顺序
        Collections.shuffle(branches, random);

        ExecutionContext context = createContext(branchCount);

        ConditionBranch result = evaluator.evaluate(branches, context);

        // 期望：最低优先级的分支（不受插入顺序影响）
        List<Boolean> allTrue = IntStream.range(0, branchCount)
                .mapToObj(i -> true)
                .collect(Collectors.toList());
        String expectedTarget = computeExpectedTarget(priorities, allTrue);

        assert expectedTarget.equals(result.getTargetNodeId()) :
                String.format("Random order: priorities=%s, expected=%s, actual=%s",
                        priorities, expectedTarget, result.getTargetNodeId());
    }

    // Feature: condition-branch-refactor, Property 1: Branch evaluation selects first matching by priority
    // **Validates: Requirements 1.2, 1.4, 5.2**
    @Property(tries = 100)
    void single_matching_branch_returns_that_branch(
            @ForAll @IntRange(min = 2, max = 8) int branchCount,
            @ForAll Random random) {

        // 随机选择一个分支作为唯一匹配的分支
        int matchIndex = random.nextInt(branchCount);

        List<ConditionBranch> branches = new ArrayList<>();
        for (int i = 0; i < branchCount; i++) {
            boolean shouldMatch = (i == matchIndex);
            branches.add(createBranch(i, "target_" + i, shouldMatch, "key_" + i));
        }
        branches.add(createDefaultBranch(Integer.MAX_VALUE));

        ExecutionContext context = createContext(branchCount);

        ConditionBranch result = evaluator.evaluate(branches, context);

        assert ("target_" + matchIndex).equals(result.getTargetNodeId()) :
                String.format("Single match: matchIndex=%d, expected=target_%d, actual=%s",
                        matchIndex, matchIndex, result.getTargetNodeId());
    }

    // Feature: condition-branch-refactor, Property 1: Branch evaluation selects first matching by priority
    // **Validates: Requirements 1.2, 1.4, 5.2**
    @Property(tries = 100)
    void mixed_matching_selects_lowest_priority_match(
            @ForAll("uniquePriorities") List<Integer> priorities,
            @ForAll Random random) {

        int branchCount = priorities.size();
        // 随机生成匹配状态，确保至少一个匹配
        List<Boolean> matchStatuses = new ArrayList<>();
        boolean hasMatch = false;
        for (int i = 0; i < branchCount; i++) {
            boolean match = random.nextBoolean();
            matchStatuses.add(match);
            if (match) hasMatch = true;
        }
        // 确保至少一个匹配
        if (!hasMatch) {
            int forceIndex = random.nextInt(branchCount);
            matchStatuses.set(forceIndex, true);
        }

        List<ConditionBranch> branches = new ArrayList<>();
        for (int i = 0; i < branchCount; i++) {
            branches.add(createBranch(priorities.get(i), "target_" + i,
                    matchStatuses.get(i), "key_" + i));
        }
        branches.add(createDefaultBranch(Integer.MAX_VALUE));

        ExecutionContext context = createContext(branchCount);

        ConditionBranch result = evaluator.evaluate(branches, context);

        String expectedTarget = computeExpectedTarget(priorities, matchStatuses);
        assert expectedTarget.equals(result.getTargetNodeId()) :
                String.format("Mixed matching: priorities=%s, matches=%s, expected=%s, actual=%s",
                        priorities, matchStatuses, expectedTarget, result.getTargetNodeId());
    }

    // Feature: condition-branch-refactor, Property 1: Branch evaluation selects first matching by priority
    // **Validates: Requirements 1.2, 1.4, 5.2**
    @Property(tries = 100)
    void result_is_never_a_higher_priority_than_expected(
            @ForAll("uniquePriorities") List<Integer> priorities,
            @ForAll Random random) {

        int branchCount = priorities.size();
        // 随机匹配状态
        List<Boolean> matchStatuses = new ArrayList<>();
        for (int i = 0; i < branchCount; i++) {
            matchStatuses.add(random.nextBoolean());
        }

        List<ConditionBranch> branches = new ArrayList<>();
        for (int i = 0; i < branchCount; i++) {
            branches.add(createBranch(priorities.get(i), "target_" + i,
                    matchStatuses.get(i), "key_" + i));
        }
        branches.add(createDefaultBranch(Integer.MAX_VALUE));

        ExecutionContext context = createContext(branchCount);

        ConditionBranch result = evaluator.evaluate(branches, context);

        if (!result.isDefault()) {
            // 结果分支的优先级应该 <= 所有其他匹配分支的优先级
            for (int i = 0; i < branchCount; i++) {
                if (matchStatuses.get(i)) {
                    assert result.getPriority() <= priorities.get(i) :
                            String.format("Result priority %d should be <= matching branch priority %d",
                                    result.getPriority(), priorities.get(i));
                }
            }
        } else {
            // 如果返回 default，则所有非 default 分支都不应匹配
            boolean anyMatch = matchStatuses.stream().anyMatch(b -> b);
            assert !anyMatch :
                    String.format("Default returned but some branches should match: matches=%s",
                            matchStatuses);
        }
    }

    // Feature: condition-branch-refactor, Property 1: Branch evaluation selects first matching by priority
    // **Validates: Requirements 1.2, 1.4, 5.2**
    @Property(tries = 100)
    void shuffled_branches_produce_same_result_as_sorted(
            @ForAll("uniquePriorities") List<Integer> priorities,
            @ForAll Random random) {

        int branchCount = priorities.size();
        // 随机匹配状态，确保至少一个匹配
        List<Boolean> matchStatuses = new ArrayList<>();
        boolean hasMatch = false;
        for (int i = 0; i < branchCount; i++) {
            boolean match = random.nextBoolean();
            matchStatuses.add(match);
            if (match) hasMatch = true;
        }
        if (!hasMatch) {
            matchStatuses.set(0, true);
        }

        // 创建排序版本的分支列表
        List<ConditionBranch> sortedBranches = new ArrayList<>();
        for (int i = 0; i < branchCount; i++) {
            sortedBranches.add(createBranch(priorities.get(i), "target_" + i,
                    matchStatuses.get(i), "key_" + i));
        }
        sortedBranches.add(createDefaultBranch(Integer.MAX_VALUE));

        // 创建打乱版本的分支列表
        List<ConditionBranch> shuffledBranches = new ArrayList<>(sortedBranches);
        Collections.shuffle(shuffledBranches, random);

        ExecutionContext context = createContext(branchCount);

        ConditionBranch sortedResult = evaluator.evaluate(sortedBranches, context);
        ConditionBranch shuffledResult = evaluator.evaluate(shuffledBranches, context);

        assert sortedResult.getTargetNodeId().equals(shuffledResult.getTargetNodeId()) :
                String.format("Sorted and shuffled should produce same result: sorted=%s, shuffled=%s, priorities=%s, matches=%s",
                        sortedResult.getTargetNodeId(), shuffledResult.getTargetNodeId(),
                        priorities, matchStatuses);
    }
}
