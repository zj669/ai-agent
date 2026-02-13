package com.zj.aiagent.infrastructure.workflow.condition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.workflow.valobj.ComparisonOperator;
import com.zj.aiagent.domain.workflow.valobj.ConditionBranch;
import com.zj.aiagent.domain.workflow.valobj.ConditionGroup;
import com.zj.aiagent.domain.workflow.valobj.ConditionItem;
import com.zj.aiagent.domain.workflow.valobj.LogicalOperator;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Property-Based Test: 分支配置序列化 round-trip
 *
 * // Feature: condition-branch-refactor, Property 2: Branch configuration serialization round-trip
 * **Validates: Requirements 1.5, 5.3**
 *
 * 验证：
 * 对于任意有效的 List&lt;ConditionBranch&gt;（包含带优先级、条件组、条件项的分支，
 * 以及恰好一个 default 分支），序列化为 JSON 再反序列化后，应产生等价的列表，
 * 具有相同的优先级、操作符、操作数和 default 标志。
 *
 * 测试策略：
 * - 使用 jqwik 生成随机有效的 ConditionBranch 列表
 * - 使用 Jackson ObjectMapper 进行 JSON 序列化/反序列化
 * - rightOperand 为 Object 类型，Jackson 可能将 Integer 反序列化为 Integer，
 *   因此使用语义等价比较而非直接 equals
 * - 生成的 rightOperand 限制为 String 和 Number 类型
 */
class BranchSerializationRoundTripPropertyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ========== 辅助方法 ==========

    /**
     * 语义比较两个 ConditionBranch 列表。
     * 由于 Jackson 对 Object 类型的反序列化行为（Integer 可能保持 Integer，
     * Double 保持 Double），需要对 rightOperand 做语义等价比较。
     */
    private void assertBranchListsEquivalent(List<ConditionBranch> original,
                                              List<ConditionBranch> deserialized) {
        assert original.size() == deserialized.size() :
                String.format("List size mismatch: original=%d, deserialized=%d",
                        original.size(), deserialized.size());

        for (int i = 0; i < original.size(); i++) {
            assertBranchEquivalent(original.get(i), deserialized.get(i), i);
        }
    }

    private void assertBranchEquivalent(ConditionBranch orig, ConditionBranch deser, int index) {
        assert orig.getPriority() == deser.getPriority() :
                String.format("Branch[%d] priority mismatch: %d vs %d",
                        index, orig.getPriority(), deser.getPriority());

        assert Objects.equals(orig.getTargetNodeId(), deser.getTargetNodeId()) :
                String.format("Branch[%d] targetNodeId mismatch: '%s' vs '%s'",
                        index, orig.getTargetNodeId(), deser.getTargetNodeId());

        assert Objects.equals(orig.getDescription(), deser.getDescription()) :
                String.format("Branch[%d] description mismatch: '%s' vs '%s'",
                        index, orig.getDescription(), deser.getDescription());

        assert orig.isDefault() == deser.isDefault() :
                String.format("Branch[%d] isDefault mismatch: %b vs %b",
                        index, orig.isDefault(), deser.isDefault());

        List<ConditionGroup> origGroups = orig.getConditionGroups() != null
                ? orig.getConditionGroups() : List.of();
        List<ConditionGroup> deserGroups = deser.getConditionGroups() != null
                ? deser.getConditionGroups() : List.of();

        assert origGroups.size() == deserGroups.size() :
                String.format("Branch[%d] conditionGroups size mismatch: %d vs %d",
                        index, origGroups.size(), deserGroups.size());

        for (int g = 0; g < origGroups.size(); g++) {
            assertGroupEquivalent(origGroups.get(g), deserGroups.get(g), index, g);
        }
    }

    private void assertGroupEquivalent(ConditionGroup orig, ConditionGroup deser,
                                        int branchIdx, int groupIdx) {
        assert orig.getOperator() == deser.getOperator() :
                String.format("Branch[%d].Group[%d] operator mismatch: %s vs %s",
                        branchIdx, groupIdx, orig.getOperator(), deser.getOperator());

        List<ConditionItem> origItems = orig.getConditions() != null
                ? orig.getConditions() : List.of();
        List<ConditionItem> deserItems = deser.getConditions() != null
                ? deser.getConditions() : List.of();

        assert origItems.size() == deserItems.size() :
                String.format("Branch[%d].Group[%d] conditions size mismatch: %d vs %d",
                        branchIdx, groupIdx, origItems.size(), deserItems.size());

        for (int c = 0; c < origItems.size(); c++) {
            assertItemEquivalent(origItems.get(c), deserItems.get(c), branchIdx, groupIdx, c);
        }
    }

    private void assertItemEquivalent(ConditionItem orig, ConditionItem deser,
                                       int branchIdx, int groupIdx, int itemIdx) {
        assert Objects.equals(orig.getLeftOperand(), deser.getLeftOperand()) :
                String.format("Branch[%d].Group[%d].Item[%d] leftOperand mismatch: '%s' vs '%s'",
                        branchIdx, groupIdx, itemIdx,
                        orig.getLeftOperand(), deser.getLeftOperand());

        assert orig.getOperator() == deser.getOperator() :
                String.format("Branch[%d].Group[%d].Item[%d] operator mismatch: %s vs %s",
                        branchIdx, groupIdx, itemIdx,
                        orig.getOperator(), deser.getOperator());

        assertOperandEquivalent(orig.getRightOperand(), deser.getRightOperand(),
                branchIdx, groupIdx, itemIdx);
    }

    /**
     * 语义比较 rightOperand。
     * Jackson 反序列化 Object 类型时：
     * - String → String（精确匹配）
     * - Integer → Integer（精确匹配）
     * - Double → Double（精确匹配）
     * - Long → 可能为 Integer 或 Long
     * 使用 Number.doubleValue() 进行数值比较以处理类型差异。
     */
    private void assertOperandEquivalent(Object orig, Object deser,
                                          int branchIdx, int groupIdx, int itemIdx) {
        if (orig == null && deser == null) {
            return;
        }

        assert orig != null && deser != null :
                String.format("Branch[%d].Group[%d].Item[%d] rightOperand null mismatch: orig=%s, deser=%s",
                        branchIdx, groupIdx, itemIdx, orig, deser);

        if (orig instanceof Number && deser instanceof Number) {
            double origVal = ((Number) orig).doubleValue();
            double deserVal = ((Number) deser).doubleValue();
            assert Double.compare(origVal, deserVal) == 0 :
                    String.format("Branch[%d].Group[%d].Item[%d] rightOperand numeric mismatch: %s(%s) vs %s(%s)",
                            branchIdx, groupIdx, itemIdx,
                            orig, orig.getClass().getSimpleName(),
                            deser, deser.getClass().getSimpleName());
        } else {
            assert orig.equals(deser) :
                    String.format("Branch[%d].Group[%d].Item[%d] rightOperand mismatch: '%s'(%s) vs '%s'(%s)",
                            branchIdx, groupIdx, itemIdx,
                            orig, orig.getClass().getSimpleName(),
                            deser, deser.getClass().getSimpleName());
        }
    }

    // ========== Generators ==========

    /**
     * 生成随机的 rightOperand（String 或 Number 类型）
     */
    @Provide
    Arbitrary<Object> rightOperands() {
        Arbitrary<Object> strings = Arbitraries.strings()
                .alpha().numeric()
                .ofMinLength(1).ofMaxLength(20)
                .map(s -> s);

        Arbitrary<Object> integers = Arbitraries.integers()
                .between(-1000, 1000)
                .map(i -> i);

        Arbitrary<Object> doubles = Arbitraries.doubles()
                .between(-1000.0, 1000.0)
                .ofScale(2)
                .map(d -> d);

        return Arbitraries.oneOf(strings, integers, doubles);
    }

    /**
     * 生成随机的 ConditionItem
     */
    @Provide
    Arbitrary<ConditionItem> conditionItems() {
        Arbitrary<String> leftOperands = Arbitraries.of(
                "nodes.llm_1.output", "nodes.node_2.result",
                "inputs.query", "inputs.userId",
                "nodes.classifier.intent", "nodes.extractor.value"
        );

        Arbitrary<ComparisonOperator> operators = Arbitraries.of(ComparisonOperator.values());

        return Combinators.combine(leftOperands, operators, rightOperands())
                .as((left, op, right) -> ConditionItem.builder()
                        .leftOperand(left)
                        .operator(op)
                        .rightOperand(right)
                        .build());
    }

    /**
     * 生成随机的 ConditionGroup（1~3 个条件项）
     */
    @Provide
    Arbitrary<ConditionGroup> conditionGroups() {
        Arbitrary<LogicalOperator> operators = Arbitraries.of(LogicalOperator.values());
        Arbitrary<List<ConditionItem>> items = conditionItems().list().ofMinSize(1).ofMaxSize(3);

        return Combinators.combine(operators, items)
                .as((op, conditions) -> ConditionGroup.builder()
                        .operator(op)
                        .conditions(conditions)
                        .build());
    }

    /**
     * 生成随机的非 default ConditionBranch
     */
    private Arbitrary<ConditionBranch> nonDefaultBranch(int priority) {
        Arbitrary<String> targetNodeIds = Arbitraries.of(
                "node_a", "node_b", "node_c", "node_d", "node_e"
        );

        Arbitrary<String> descriptions = Arbitraries.of(
                "用户表达了购买意向", "用户在咨询问题",
                "高优先级请求", "需要人工审核",
                "Branch " + priority
        );

        Arbitrary<List<ConditionGroup>> groups = conditionGroups().list().ofMinSize(1).ofMaxSize(3);

        return Combinators.combine(targetNodeIds, descriptions, groups)
                .as((target, desc, grps) -> ConditionBranch.builder()
                        .priority(priority)
                        .targetNodeId(target)
                        .description(desc)
                        .isDefault(false)
                        .conditionGroups(grps)
                        .build());
    }

    /**
     * 生成包含恰好一个 default 分支的有效 ConditionBranch 列表。
     * 非 default 分支数量为 1~5，default 分支优先级最大。
     */
    @Provide
    Arbitrary<List<ConditionBranch>> validBranchLists() {
        return Arbitraries.integers().between(1, 5).flatMap(count -> {
            // 生成 count 个非 default 分支
            List<Arbitrary<ConditionBranch>> branchArbitraries = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                branchArbitraries.add(nonDefaultBranch(i));
            }

            return Combinators.combine(branchArbitraries).as(branches -> {
                List<ConditionBranch> result = new ArrayList<>(branches);
                // 添加恰好一个 default 分支
                result.add(ConditionBranch.builder()
                        .priority(count)
                        .targetNodeId("default_node")
                        .description("默认分支")
                        .isDefault(true)
                        .conditionGroups(List.of())
                        .build());
                return result;
            });
        });
    }

    // ========== Property Tests ==========

    // Feature: condition-branch-refactor, Property 2: Branch configuration serialization round-trip
    // **Validates: Requirements 1.5, 5.3**
    @Property(tries = 100)
    void branch_list_survives_json_round_trip(
            @ForAll("validBranchLists") List<ConditionBranch> originalBranches)
            throws JsonProcessingException {

        // 序列化为 JSON
        String json = objectMapper.writeValueAsString(originalBranches);

        // 反序列化回 List<ConditionBranch>
        List<ConditionBranch> deserialized = objectMapper.readValue(json,
                new TypeReference<List<ConditionBranch>>() {});

        // 验证等价性
        assertBranchListsEquivalent(originalBranches, deserialized);
    }

    // Feature: condition-branch-refactor, Property 2: Branch configuration serialization round-trip
    // **Validates: Requirements 1.5, 5.3**
    @Property(tries = 100)
    void priorities_preserved_after_round_trip(
            @ForAll("validBranchLists") List<ConditionBranch> originalBranches)
            throws JsonProcessingException {

        String json = objectMapper.writeValueAsString(originalBranches);
        List<ConditionBranch> deserialized = objectMapper.readValue(json,
                new TypeReference<List<ConditionBranch>>() {});

        // 验证所有优先级完全一致
        List<Integer> originalPriorities = originalBranches.stream()
                .map(ConditionBranch::getPriority)
                .collect(Collectors.toList());
        List<Integer> deserializedPriorities = deserialized.stream()
                .map(ConditionBranch::getPriority)
                .collect(Collectors.toList());

        assert originalPriorities.equals(deserializedPriorities) :
                String.format("Priorities mismatch: original=%s, deserialized=%s",
                        originalPriorities, deserializedPriorities);
    }

    // Feature: condition-branch-refactor, Property 2: Branch configuration serialization round-trip
    // **Validates: Requirements 1.5, 5.3**
    @Property(tries = 100)
    void default_flags_preserved_after_round_trip(
            @ForAll("validBranchLists") List<ConditionBranch> originalBranches)
            throws JsonProcessingException {

        String json = objectMapper.writeValueAsString(originalBranches);
        List<ConditionBranch> deserialized = objectMapper.readValue(json,
                new TypeReference<List<ConditionBranch>>() {});

        // 验证 default 标志完全一致
        List<Boolean> originalDefaults = originalBranches.stream()
                .map(ConditionBranch::isDefault)
                .collect(Collectors.toList());
        List<Boolean> deserializedDefaults = deserialized.stream()
                .map(ConditionBranch::isDefault)
                .collect(Collectors.toList());

        assert originalDefaults.equals(deserializedDefaults) :
                String.format("Default flags mismatch: original=%s, deserialized=%s",
                        originalDefaults, deserializedDefaults);

        // 验证恰好一个 default 分支
        long defaultCount = deserialized.stream().filter(ConditionBranch::isDefault).count();
        assert defaultCount == 1 :
                String.format("Expected exactly 1 default branch after round-trip, got %d", defaultCount);
    }

    // Feature: condition-branch-refactor, Property 2: Branch configuration serialization round-trip
    // **Validates: Requirements 1.5, 5.3**
    @Property(tries = 100)
    void operators_preserved_after_round_trip(
            @ForAll("validBranchLists") List<ConditionBranch> originalBranches)
            throws JsonProcessingException {

        String json = objectMapper.writeValueAsString(originalBranches);
        List<ConditionBranch> deserialized = objectMapper.readValue(json,
                new TypeReference<List<ConditionBranch>>() {});

        // 收集所有 ComparisonOperator 和 LogicalOperator
        for (int i = 0; i < originalBranches.size(); i++) {
            ConditionBranch origBranch = originalBranches.get(i);
            ConditionBranch deserBranch = deserialized.get(i);

            List<ConditionGroup> origGroups = origBranch.getConditionGroups() != null
                    ? origBranch.getConditionGroups() : List.of();
            List<ConditionGroup> deserGroups = deserBranch.getConditionGroups() != null
                    ? deserBranch.getConditionGroups() : List.of();

            for (int g = 0; g < origGroups.size(); g++) {
                ConditionGroup origGroup = origGroups.get(g);
                ConditionGroup deserGroup = deserGroups.get(g);

                // 验证 LogicalOperator
                assert origGroup.getOperator() == deserGroup.getOperator() :
                        String.format("Branch[%d].Group[%d] LogicalOperator mismatch: %s vs %s",
                                i, g, origGroup.getOperator(), deserGroup.getOperator());

                List<ConditionItem> origItems = origGroup.getConditions() != null
                        ? origGroup.getConditions() : List.of();
                List<ConditionItem> deserItems = deserGroup.getConditions() != null
                        ? deserGroup.getConditions() : List.of();

                for (int c = 0; c < origItems.size(); c++) {
                    // 验证 ComparisonOperator
                    assert origItems.get(c).getOperator() == deserItems.get(c).getOperator() :
                            String.format("Branch[%d].Group[%d].Item[%d] ComparisonOperator mismatch: %s vs %s",
                                    i, g, c,
                                    origItems.get(c).getOperator(),
                                    deserItems.get(c).getOperator());
                }
            }
        }
    }

    // Feature: condition-branch-refactor, Property 2: Branch configuration serialization round-trip
    // **Validates: Requirements 1.5, 5.3**
    @Property(tries = 100)
    void double_round_trip_is_idempotent(
            @ForAll("validBranchLists") List<ConditionBranch> originalBranches)
            throws JsonProcessingException {

        // 第一次 round-trip
        String json1 = objectMapper.writeValueAsString(originalBranches);
        List<ConditionBranch> firstRoundTrip = objectMapper.readValue(json1,
                new TypeReference<List<ConditionBranch>>() {});

        // 第二次 round-trip
        String json2 = objectMapper.writeValueAsString(firstRoundTrip);
        List<ConditionBranch> secondRoundTrip = objectMapper.readValue(json2,
                new TypeReference<List<ConditionBranch>>() {});

        // 两次 round-trip 的结果应完全一致（幂等性）
        assertBranchListsEquivalent(firstRoundTrip, secondRoundTrip);

        // JSON 字符串也应完全一致
        assert json1.equals(json2) :
                String.format("JSON not idempotent:\n  first:  %s\n  second: %s", json1, json2);
    }

    // Feature: condition-branch-refactor, Property 2: Branch configuration serialization round-trip
    // **Validates: Requirements 1.5, 5.3**
    @Property(tries = 100)
    void target_node_ids_and_descriptions_preserved(
            @ForAll("validBranchLists") List<ConditionBranch> originalBranches)
            throws JsonProcessingException {

        String json = objectMapper.writeValueAsString(originalBranches);
        List<ConditionBranch> deserialized = objectMapper.readValue(json,
                new TypeReference<List<ConditionBranch>>() {});

        for (int i = 0; i < originalBranches.size(); i++) {
            ConditionBranch orig = originalBranches.get(i);
            ConditionBranch deser = deserialized.get(i);

            assert Objects.equals(orig.getTargetNodeId(), deser.getTargetNodeId()) :
                    String.format("Branch[%d] targetNodeId mismatch: '%s' vs '%s'",
                            i, orig.getTargetNodeId(), deser.getTargetNodeId());

            assert Objects.equals(orig.getDescription(), deser.getDescription()) :
                    String.format("Branch[%d] description mismatch: '%s' vs '%s'",
                            i, orig.getDescription(), deser.getDescription());
        }
    }
}
