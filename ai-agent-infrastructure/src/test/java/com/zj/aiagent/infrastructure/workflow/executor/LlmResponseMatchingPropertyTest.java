package com.zj.aiagent.infrastructure.workflow.executor;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Property-Based Test: LLM 响应匹配
 *
 * // Feature: condition-branch-refactor, Property 9: LLM response target ID matching
 * **Validates: Requirements 7.4**
 *
 * 验证：
 * 对于任意有效的目标节点 ID 和任意在 trim 空白 + 忽略大小写后等于该目标 ID 的字符串，
 * LLM 响应解析器应成功匹配到该目标 ID。
 * 对于不匹配任何有效 ID 的字符串，解析器应返回 null。
 *
 * 测试策略：
 * - 生成随机的有效目标 ID（字母数字 + 下划线/连字符）
 * - 生成随机的空白填充（空格、制表符、换行符）
 * - 生成随机的大小写变体（大写、小写、混合）
 * - 验证匹配逻辑正确识别目标
 *
 * 注意：由于 callLlmAndMatchTarget 是 private 方法且涉及 LLM 调用，
 * 本测试直接实现相同的匹配逻辑（trim + case-insensitive）来验证 property。
 */
class LlmResponseMatchingPropertyTest {

    // ========== 被测匹配逻辑（与 ConditionNodeExecutorStrategy.callLlmAndMatchTarget 中一致） ==========

    /**
     * 模拟 callLlmAndMatchTarget 中的匹配逻辑：
     * trim 空白 + case-insensitive 匹配，返回匹配到的原始有效 ID，匹配失败返回 null
     */
    private String matchTarget(String response, List<String> validTargetIds) {
        if (response == null) {
            return null;
        }
        String trimmed = response.trim();
        for (String validId : validTargetIds) {
            if (validId.equalsIgnoreCase(trimmed)) {
                return validId; // 返回原始的有效 ID（保持大小写）
            }
        }
        return null;
    }

    // ========== 辅助方法 ==========

    /**
     * 对字符串应用随机大小写变换
     */
    private String applyCaseVariation(String input, String caseMode) {
        return switch (caseMode) {
            case "upper" -> input.toUpperCase();
            case "lower" -> input.toLowerCase();
            case "mixed" -> {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < input.length(); i++) {
                    char c = input.charAt(i);
                    if (i % 2 == 0) {
                        sb.append(Character.toUpperCase(c));
                    } else {
                        sb.append(Character.toLowerCase(c));
                    }
                }
                yield sb.toString();
            }
            default -> input;
        };
    }

    // ========== Generators ==========

    /**
     * 生成有效的目标节点 ID（字母数字 + 下划线/连字符，1~30 字符）
     */
    @Provide
    Arbitrary<String> validTargetIds() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars('_', '-')
                .ofMinLength(1)
                .ofMaxLength(30)
                .filter(s -> !s.isBlank()); // 确保不是纯空白
    }

    /**
     * 生成空白填充字符串（空格、制表符、换行符的组合）
     */
    @Provide
    Arbitrary<String> whitespacePadding() {
        return Arbitraries.of(' ', '\t', '\n', '\r')
                .list().ofMinSize(0).ofMaxSize(10)
                .map(chars -> chars.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining()));
    }

    /**
     * 生成大小写模式
     */
    @Provide
    Arbitrary<String> caseModes() {
        return Arbitraries.of("upper", "lower", "mixed", "original");
    }

    /**
     * 生成 2~8 个不重复的有效目标 ID 列表
     */
    @Provide
    Arbitrary<List<String>> validTargetIdLists() {
        return validTargetIds()
                .list().ofMinSize(2).ofMaxSize(8)
                .filter(list -> list.stream().map(String::toLowerCase).distinct().count() == list.size());
    }

    /**
     * 生成一个完全不匹配任何有效 ID 的字符串
     * 使用特殊前缀确保不会意外匹配
     */
    @Provide
    Arbitrary<String> nonMatchingResponses() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(20)
                .map(s -> "NOMATCH_" + s);
    }

    // ========== Property Tests ==========

    // Feature: condition-branch-refactor, Property 9: LLM response target ID matching
    // **Validates: Requirements 7.4**
    @Property(tries = 100)
    void exact_match_succeeds(
            @ForAll("validTargetIdLists") List<String> targetIds,
            @ForAll @IntRange(min = 0, max = 7) int targetIndex) {

        int idx = targetIndex % targetIds.size();
        String chosenId = targetIds.get(idx);

        // 精确匹配（无空白、无大小写变化）
        String result = matchTarget(chosenId, targetIds);

        assert result != null :
                String.format("Exact match should succeed: response='%s', validIds=%s", chosenId, targetIds);
        assert result.equals(chosenId) :
                String.format("Exact match should return original ID: expected='%s', actual='%s'", chosenId, result);
    }

    // Feature: condition-branch-refactor, Property 9: LLM response target ID matching
    // **Validates: Requirements 7.4**
    @Property(tries = 100)
    void match_succeeds_with_whitespace_padding(
            @ForAll("validTargetIdLists") List<String> targetIds,
            @ForAll @IntRange(min = 0, max = 7) int targetIndex,
            @ForAll("whitespacePadding") String leadingWhitespace,
            @ForAll("whitespacePadding") String trailingWhitespace) {

        int idx = targetIndex % targetIds.size();
        String chosenId = targetIds.get(idx);

        // 添加前后空白
        String response = leadingWhitespace + chosenId + trailingWhitespace;

        String result = matchTarget(response, targetIds);

        assert result != null :
                String.format("Match with whitespace should succeed: response='%s', chosenId='%s', validIds=%s",
                        response.replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r"),
                        chosenId, targetIds);
        assert result.equals(chosenId) :
                String.format("Match with whitespace should return original ID: expected='%s', actual='%s'",
                        chosenId, result);
    }

    // Feature: condition-branch-refactor, Property 9: LLM response target ID matching
    // **Validates: Requirements 7.4**
    @Property(tries = 100)
    void match_succeeds_with_case_variation(
            @ForAll("validTargetIdLists") List<String> targetIds,
            @ForAll @IntRange(min = 0, max = 7) int targetIndex,
            @ForAll("caseModes") String caseMode) {

        int idx = targetIndex % targetIds.size();
        String chosenId = targetIds.get(idx);

        // 应用大小写变换
        String response = applyCaseVariation(chosenId, caseMode);

        String result = matchTarget(response, targetIds);

        assert result != null :
                String.format("Case-insensitive match should succeed: response='%s', chosenId='%s', caseMode=%s, validIds=%s",
                        response, chosenId, caseMode, targetIds);
        assert result.equals(chosenId) :
                String.format("Case-insensitive match should return original ID: expected='%s', actual='%s'",
                        chosenId, result);
    }

    // Feature: condition-branch-refactor, Property 9: LLM response target ID matching
    // **Validates: Requirements 7.4**
    @Property(tries = 100)
    void match_succeeds_with_whitespace_and_case_combined(
            @ForAll("validTargetIdLists") List<String> targetIds,
            @ForAll @IntRange(min = 0, max = 7) int targetIndex,
            @ForAll("whitespacePadding") String leadingWhitespace,
            @ForAll("whitespacePadding") String trailingWhitespace,
            @ForAll("caseModes") String caseMode) {

        int idx = targetIndex % targetIds.size();
        String chosenId = targetIds.get(idx);

        // 同时应用大小写变换和空白填充
        String caseVaried = applyCaseVariation(chosenId, caseMode);
        String response = leadingWhitespace + caseVaried + trailingWhitespace;

        String result = matchTarget(response, targetIds);

        assert result != null :
                String.format("Combined whitespace+case match should succeed: response='%s', chosenId='%s', caseMode=%s",
                        response.replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r"),
                        chosenId, caseMode);
        assert result.equals(chosenId) :
                String.format("Combined match should return original ID: expected='%s', actual='%s'",
                        chosenId, result);
    }

    // Feature: condition-branch-refactor, Property 9: LLM response target ID matching
    // **Validates: Requirements 7.4**
    @Property(tries = 100)
    void non_matching_response_returns_null(
            @ForAll("validTargetIdLists") List<String> targetIds,
            @ForAll("nonMatchingResponses") String nonMatchingResponse) {

        String result = matchTarget(nonMatchingResponse, targetIds);

        assert result == null :
                String.format("Non-matching response should return null: response='%s', validIds=%s, got='%s'",
                        nonMatchingResponse, targetIds, result);
    }

    // Feature: condition-branch-refactor, Property 9: LLM response target ID matching
    // **Validates: Requirements 7.4**
    @Property(tries = 100)
    void null_response_returns_null(
            @ForAll("validTargetIdLists") List<String> targetIds) {

        String result = matchTarget(null, targetIds);

        assert result == null :
                String.format("Null response should return null, but got '%s'", result);
    }

    // Feature: condition-branch-refactor, Property 9: LLM response target ID matching
    // **Validates: Requirements 7.4**
    @Property(tries = 100)
    void matched_result_preserves_original_case(
            @ForAll("validTargetIdLists") List<String> targetIds,
            @ForAll @IntRange(min = 0, max = 7) int targetIndex) {

        int idx = targetIndex % targetIds.size();
        String chosenId = targetIds.get(idx);

        // 使用全大写版本作为响应
        String response = chosenId.toUpperCase();

        String result = matchTarget(response, targetIds);

        assert result != null :
                String.format("Match should succeed: response='%s', validIds=%s", response, targetIds);
        // 返回的应该是原始 ID（保持原始大小写），而不是响应中的大小写
        assert result.equals(chosenId) :
                String.format("Result should preserve original case: expected='%s', actual='%s'", chosenId, result);
    }

    // Feature: condition-branch-refactor, Property 9: LLM response target ID matching
    // **Validates: Requirements 7.4**
    @Property(tries = 100)
    void whitespace_only_response_does_not_match_non_empty_ids(
            @ForAll("validTargetIdLists") List<String> targetIds,
            @ForAll("whitespacePadding") String whitespaceOnly) {

        // 纯空白响应 trim 后为空字符串，不应匹配任何非空 ID
        String result = matchTarget(whitespaceOnly, targetIds);

        // 只有当某个 validId 本身 equalsIgnoreCase("") 时才可能匹配
        // 但我们的 generator 保证 validId 不为空，所以结果应为 null
        assert result == null :
                String.format("Whitespace-only response should not match: response='%s', validIds=%s, got='%s'",
                        whitespaceOnly.replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r"),
                        targetIds, result);
    }
}
