package com.zj.aiagent.infrastructure.workflow.condition;

import com.zj.aiagent.domain.workflow.valobj.ComparisonOperator;
import com.zj.aiagent.domain.workflow.valobj.ConditionBranch;
import com.zj.aiagent.domain.workflow.valobj.ConditionGroup;
import com.zj.aiagent.domain.workflow.valobj.ConditionItem;
import com.zj.aiagent.domain.workflow.valobj.ExecutionContext;
import com.zj.aiagent.domain.workflow.valobj.LogicalOperator;
import net.jqwik.api.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Property-Based Test: 变量引用解析
 *
 * // Feature: condition-branch-refactor, Property 6: Variable reference resolution
 * **Validates: Requirements 2.6, 3.3, 8.2, 8.3**
 *
 * 验证：对于任意有效的变量引用（格式 nodes.{nodeId}.{outputKey} 或 inputs.{key}），
 * 以及任意包含该引用值的 ExecutionContext，resolver 应返回 context 中存储的精确值。
 * 对于引用不存在变量的情况，resolver 应返回 null（条件视为不满足）。
 *
 * 由于 resolveOperand 是 private 方法，通过构造单条件 ConditionBranch 调用 evaluate() 间接测试。
 * 使用 EQUALS 操作符验证解析后的值与期望值匹配。
 */
class VariableReferenceResolutionPropertyTest {

    private final StructuredConditionEvaluator evaluator = new StructuredConditionEvaluator();

    // ========== 辅助方法 ==========

    /**
     * 通过 evaluate() 间接测试变量引用解析。
     * 构造一个条件：leftOperand 为变量引用，rightOperand 为期望值，操作符为 EQUALS。
     * 如果 evaluate 返回非 default 分支，说明变量引用解析正确且值匹配。
     */
    private boolean evaluateVariableReference(String variableRef, Object expectedValue, ExecutionContext context) {
        ConditionItem item = ConditionItem.builder()
                .leftOperand(variableRef)
                .operator(ComparisonOperator.EQUALS)
                .rightOperand(expectedValue)
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

    // ========== Generators ==========

    /**
     * 生成合法的 nodeId（字母数字下划线，不为空，不含点号）
     */
    @Provide
    Arbitrary<String> validNodeIds() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars('_')
                .ofMinLength(1)
                .ofMaxLength(20)
                .filter(s -> !s.isEmpty());
    }

    /**
     * 生成合法的 outputKey（字母数字下划线，不为空，不含点号）
     */
    @Provide
    Arbitrary<String> validOutputKeys() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars('_')
                .ofMinLength(1)
                .ofMaxLength(20)
                .filter(s -> !s.isEmpty());
    }

    /**
     * 生成随机字符串值
     */
    @Provide
    Arbitrary<String> stringValues() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30);
    }

    /**
     * 生成随机数值
     */
    @Provide
    Arbitrary<Double> numericValues() {
        return Arbitraries.doubles().between(-100_000, 100_000)
                .filter(d -> !d.isNaN() && !d.isInfinite());
    }

    /**
     * 生成随机整数值
     */
    @Provide
    Arbitrary<Integer> intValues() {
        return Arbitraries.integers().between(-100_000, 100_000);
    }

    // ========== Property Tests: nodes.{nodeId}.{key} 引用解析 ==========

    // Feature: condition-branch-refactor, Property 6: Variable reference resolution
    // **Validates: Requirements 2.6, 3.3, 8.2, 8.3**
    @Property(tries = 100)
    void node_reference_resolves_string_value(
            @ForAll("validNodeIds") String nodeId,
            @ForAll("validOutputKeys") String outputKey,
            @ForAll("stringValues") String value) {
        // 将值存入 ExecutionContext
        ExecutionContext context = ExecutionContext.builder().build();
        context.setNodeOutput(nodeId, Map.of(outputKey, value));

        // 构造变量引用
        String reference = "nodes." + nodeId + "." + outputKey;

        // 验证解析后的值与存储的值匹配
        boolean matched = evaluateVariableReference(reference, value, context);
        assert matched :
                String.format("Node reference should resolve to stored value: ref='%s', value='%s'",
                        reference, value);
    }

    // Feature: condition-branch-refactor, Property 6: Variable reference resolution
    // **Validates: Requirements 2.6, 3.3, 8.2, 8.3**
    @Property(tries = 100)
    void node_reference_resolves_numeric_value(
            @ForAll("validNodeIds") String nodeId,
            @ForAll("validOutputKeys") String outputKey,
            @ForAll("numericValues") Double value) {
        ExecutionContext context = ExecutionContext.builder().build();
        context.setNodeOutput(nodeId, Map.of(outputKey, value));

        String reference = "nodes." + nodeId + "." + outputKey;

        boolean matched = evaluateVariableReference(reference, value, context);
        assert matched :
                String.format("Node reference should resolve numeric value: ref='%s', value=%s",
                        reference, value);
    }

    // Feature: condition-branch-refactor, Property 6: Variable reference resolution
    // **Validates: Requirements 2.6, 3.3, 8.2, 8.3**
    @Property(tries = 100)
    void node_reference_resolves_integer_value(
            @ForAll("validNodeIds") String nodeId,
            @ForAll("validOutputKeys") String outputKey,
            @ForAll("intValues") Integer value) {
        ExecutionContext context = ExecutionContext.builder().build();
        context.setNodeOutput(nodeId, Map.of(outputKey, value));

        String reference = "nodes." + nodeId + "." + outputKey;

        boolean matched = evaluateVariableReference(reference, value, context);
        assert matched :
                String.format("Node reference should resolve integer value: ref='%s', value=%d",
                        reference, value);
    }

    // ========== Property Tests: inputs.{key} 引用解析 ==========

    // Feature: condition-branch-refactor, Property 6: Variable reference resolution
    // **Validates: Requirements 2.6, 3.3, 8.2, 8.3**
    @Property(tries = 100)
    void input_reference_resolves_string_value(
            @ForAll("validOutputKeys") String inputKey,
            @ForAll("stringValues") String value) {
        ExecutionContext context = ExecutionContext.builder().build();
        context.setInputs(Map.of(inputKey, value));

        String reference = "inputs." + inputKey;

        boolean matched = evaluateVariableReference(reference, value, context);
        assert matched :
                String.format("Input reference should resolve to stored value: ref='%s', value='%s'",
                        reference, value);
    }

    // Feature: condition-branch-refactor, Property 6: Variable reference resolution
    // **Validates: Requirements 2.6, 3.3, 8.2, 8.3**
    @Property(tries = 100)
    void input_reference_resolves_numeric_value(
            @ForAll("validOutputKeys") String inputKey,
            @ForAll("numericValues") Double value) {
        ExecutionContext context = ExecutionContext.builder().build();
        context.setInputs(Map.of(inputKey, value));

        String reference = "inputs." + inputKey;

        boolean matched = evaluateVariableReference(reference, value, context);
        assert matched :
                String.format("Input reference should resolve numeric value: ref='%s', value=%s",
                        reference, value);
    }

    // ========== Property Tests: 不存在的变量引用 → null → 条件不满足 ==========

    // Feature: condition-branch-refactor, Property 6: Variable reference resolution
    // **Validates: Requirements 2.6, 3.3, 8.2, 8.3**
    @Property(tries = 100)
    void nonexistent_node_reference_treats_condition_as_not_satisfied(
            @ForAll("validNodeIds") String existingNodeId,
            @ForAll("validOutputKeys") String existingKey,
            @ForAll("stringValues") String value,
            @ForAll("validNodeIds") String missingNodeId) {
        // 确保 missingNodeId 与 existingNodeId 不同
        Assume.that(!missingNodeId.equals(existingNodeId));

        ExecutionContext context = ExecutionContext.builder().build();
        context.setNodeOutput(existingNodeId, Map.of(existingKey, value));

        // 引用不存在的 nodeId
        String reference = "nodes." + missingNodeId + "." + existingKey;

        // 不存在的引用 → null → 条件不满足 → 返回 default 分支
        boolean matched = evaluateVariableReference(reference, value, context);
        assert !matched :
                String.format("Nonexistent node reference should not match: ref='%s'", reference);
    }

    // Feature: condition-branch-refactor, Property 6: Variable reference resolution
    // **Validates: Requirements 2.6, 3.3, 8.2, 8.3**
    @Property(tries = 100)
    void nonexistent_output_key_treats_condition_as_not_satisfied(
            @ForAll("validNodeIds") String nodeId,
            @ForAll("validOutputKeys") String existingKey,
            @ForAll("stringValues") String value,
            @ForAll("validOutputKeys") String missingKey) {
        // 确保 missingKey 与 existingKey 不同
        Assume.that(!missingKey.equals(existingKey));

        ExecutionContext context = ExecutionContext.builder().build();
        context.setNodeOutput(nodeId, Map.of(existingKey, value));

        // 引用不存在的 outputKey
        String reference = "nodes." + nodeId + "." + missingKey;

        boolean matched = evaluateVariableReference(reference, value, context);
        assert !matched :
                String.format("Nonexistent output key reference should not match: ref='%s'", reference);
    }

    // Feature: condition-branch-refactor, Property 6: Variable reference resolution
    // **Validates: Requirements 2.6, 3.3, 8.2, 8.3**
    @Property(tries = 100)
    void nonexistent_input_key_treats_condition_as_not_satisfied(
            @ForAll("validOutputKeys") String existingKey,
            @ForAll("stringValues") String value,
            @ForAll("validOutputKeys") String missingKey) {
        Assume.that(!missingKey.equals(existingKey));

        ExecutionContext context = ExecutionContext.builder().build();
        context.setInputs(Map.of(existingKey, value));

        // 引用不存在的 input key
        String reference = "inputs." + missingKey;

        boolean matched = evaluateVariableReference(reference, value, context);
        assert !matched :
                String.format("Nonexistent input key reference should not match: ref='%s'", reference);
    }

    // ========== Property Tests: 空 context 场景 ==========

    // Feature: condition-branch-refactor, Property 6: Variable reference resolution
    // **Validates: Requirements 2.6, 3.3, 8.2, 8.3**
    @Property(tries = 100)
    void empty_context_node_reference_treats_condition_as_not_satisfied(
            @ForAll("validNodeIds") String nodeId,
            @ForAll("validOutputKeys") String outputKey,
            @ForAll("stringValues") String value) {
        // 空 context，没有任何 node output
        ExecutionContext context = ExecutionContext.builder().build();

        String reference = "nodes." + nodeId + "." + outputKey;

        boolean matched = evaluateVariableReference(reference, value, context);
        assert !matched :
                String.format("Empty context node reference should not match: ref='%s'", reference);
    }

    // Feature: condition-branch-refactor, Property 6: Variable reference resolution
    // **Validates: Requirements 2.6, 3.3, 8.2, 8.3**
    @Property(tries = 100)
    void empty_context_input_reference_treats_condition_as_not_satisfied(
            @ForAll("validOutputKeys") String inputKey,
            @ForAll("stringValues") String value) {
        // 空 context，没有任何 inputs
        ExecutionContext context = ExecutionContext.builder().build();

        String reference = "inputs." + inputKey;

        boolean matched = evaluateVariableReference(reference, value, context);
        assert !matched :
                String.format("Empty context input reference should not match: ref='%s'", reference);
    }

    // ========== Property Tests: 多节点输出隔离性 ==========

    // Feature: condition-branch-refactor, Property 6: Variable reference resolution
    // **Validates: Requirements 2.6, 3.3, 8.2, 8.3**
    @Property(tries = 100)
    void node_reference_resolves_correct_node_among_multiple(
            @ForAll("validNodeIds") String nodeId1,
            @ForAll("validNodeIds") String nodeId2,
            @ForAll("validOutputKeys") String outputKey,
            @ForAll("stringValues") String value1,
            @ForAll("stringValues") String value2) {
        // 确保两个 nodeId 不同且两个值不同
        Assume.that(!nodeId1.equals(nodeId2));
        Assume.that(!value1.equals(value2));

        ExecutionContext context = ExecutionContext.builder().build();
        context.setNodeOutput(nodeId1, Map.of(outputKey, value1));
        context.setNodeOutput(nodeId2, Map.of(outputKey, value2));

        // 引用 nodeId1 应该得到 value1
        String ref1 = "nodes." + nodeId1 + "." + outputKey;
        boolean matched1 = evaluateVariableReference(ref1, value1, context);
        assert matched1 :
                String.format("Should resolve to node1's value: ref='%s', expected='%s'", ref1, value1);

        // 引用 nodeId2 应该得到 value2
        String ref2 = "nodes." + nodeId2 + "." + outputKey;
        boolean matched2 = evaluateVariableReference(ref2, value2, context);
        assert matched2 :
                String.format("Should resolve to node2's value: ref='%s', expected='%s'", ref2, value2);

        // 交叉验证：nodeId1 的引用不应该匹配 value2
        boolean crossMatched = evaluateVariableReference(ref1, value2, context);
        assert !crossMatched :
                String.format("Node1 reference should NOT match node2's value: ref='%s', wrongValue='%s'",
                        ref1, value2);
    }

    // ========== Property Tests: 多 key 输出隔离性 ==========

    // Feature: condition-branch-refactor, Property 6: Variable reference resolution
    // **Validates: Requirements 2.6, 3.3, 8.2, 8.3**
    @Property(tries = 100)
    void node_reference_resolves_correct_key_among_multiple(
            @ForAll("validNodeIds") String nodeId,
            @ForAll("validOutputKeys") String key1,
            @ForAll("validOutputKeys") String key2,
            @ForAll("stringValues") String value1,
            @ForAll("stringValues") String value2) {
        Assume.that(!key1.equals(key2));
        Assume.that(!value1.equals(value2));

        ExecutionContext context = ExecutionContext.builder().build();
        Map<String, Object> outputs = new HashMap<>();
        outputs.put(key1, value1);
        outputs.put(key2, value2);
        context.setNodeOutput(nodeId, outputs);

        // 引用 key1 应该得到 value1
        String ref1 = "nodes." + nodeId + "." + key1;
        boolean matched1 = evaluateVariableReference(ref1, value1, context);
        assert matched1 :
                String.format("Should resolve to key1's value: ref='%s', expected='%s'", ref1, value1);

        // 引用 key2 应该得到 value2
        String ref2 = "nodes." + nodeId + "." + key2;
        boolean matched2 = evaluateVariableReference(ref2, value2, context);
        assert matched2 :
                String.format("Should resolve to key2's value: ref='%s', expected='%s'", ref2, value2);
    }
}
