package com.zj.aiagent.infrastructure.workflow.util;

import com.zj.aiagent.domain.workflow.valobj.ComparisonOperator;
import com.zj.aiagent.domain.workflow.valobj.ConditionItem;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SpEL 表达式转 ConditionItem 转换器
 * 将旧版 SpEL 条件表达式解析为结构化 ConditionItem
 *
 * 支持的 SpEL 模式：
 * <ul>
 *   <li>{@code #variable op value} — op 为 ==, !=, >, <, >=, <=</li>
 *   <li>{@code #variable.contains('text')} → CONTAINS</li>
 *   <li>{@code #variable.startsWith('text')} → STARTS_WITH</li>
 *   <li>{@code #variable.endsWith('text')} → ENDS_WITH</li>
 *   <li>{@code #variable.isEmpty()} → IS_EMPTY</li>
 * </ul>
 */
public final class SpelToConditionConverter {

    /**
     * 简单比较操作符的正则模式
     * 匹配格式: #variable op value
     * 其中 op 为 ==, !=, >=, <=, >, <
     * value 部分不允许包含 &&, ||, ? 等复合表达式字符
     */
    private static final Pattern SPEL_COMPARISON_PATTERN =
            Pattern.compile("^#(\\w+)\\s*(==|!=|>=|<=|>|<)\\s*(.+)$");

    /**
     * 字符串方法调用的正则模式
     * 匹配格式: #variable.methodName('value') 或 #variable.methodName()
     */
    private static final Pattern SPEL_METHOD_PATTERN =
            Pattern.compile("^#(\\w+)\\.(contains|startsWith|endsWith|isEmpty)\\(([^)]*)?\\)$");

    private SpelToConditionConverter() {
        // Utility class, no instantiation
    }

    /**
     * 尝试将 SpEL 表达式解析为 ConditionItem
     *
     * @param spelExpression SpEL 表达式字符串
     * @return 解析成功返回 ConditionItem，失败返回 null
     */
    public static ConditionItem parse(String spelExpression) {
        if (spelExpression == null || spelExpression.isBlank()) {
            return null;
        }

        String trimmed = spelExpression.trim();

        // 尝试匹配比较操作符模式: #variable op value
        Matcher comparisonMatcher = SPEL_COMPARISON_PATTERN.matcher(trimmed);
        if (comparisonMatcher.matches()) {
            String variable = comparisonMatcher.group(1);
            String operator = comparisonMatcher.group(2);
            String rawValue = comparisonMatcher.group(3).trim();

            // 排除复合表达式（包含 &&, ||, ?, # 等）
            if (rawValue.contains("&&") || rawValue.contains("||")
                    || rawValue.contains("?") || rawValue.contains("#")) {
                return null;
            }

            ComparisonOperator compOp = mapSpelOperator(operator);
            if (compOp == null) {
                return null;
            }

            Object parsedValue = parseLiteralValue(rawValue);
            return ConditionItem.builder()
                    .leftOperand("inputs." + variable)
                    .operator(compOp)
                    .rightOperand(parsedValue)
                    .build();
        }

        // 尝试匹配方法调用模式: #variable.method('value') 或 #variable.method()
        Matcher methodMatcher = SPEL_METHOD_PATTERN.matcher(trimmed);
        if (methodMatcher.matches()) {
            String variable = methodMatcher.group(1);
            String methodName = methodMatcher.group(2);
            String methodArg = methodMatcher.group(3);

            ComparisonOperator compOp = mapSpelMethod(methodName);
            if (compOp == null) {
                return null;
            }

            // isEmpty() 不需要右操作数
            if (compOp == ComparisonOperator.IS_EMPTY) {
                return ConditionItem.builder()
                        .leftOperand("inputs." + variable)
                        .operator(compOp)
                        .build();
            }

            // contains/startsWith/endsWith 需要解析参数
            Object parsedArg = parseLiteralValue(methodArg != null ? methodArg.trim() : "");
            return ConditionItem.builder()
                    .leftOperand("inputs." + variable)
                    .operator(compOp)
                    .rightOperand(parsedArg)
                    .build();
        }

        // 无法解析
        return null;
    }

    /**
     * 将 SpEL 比较操作符映射为 ComparisonOperator
     */
    private static ComparisonOperator mapSpelOperator(String spelOp) {
        return switch (spelOp) {
            case "==" -> ComparisonOperator.EQUALS;
            case "!=" -> ComparisonOperator.NOT_EQUALS;
            case ">" -> ComparisonOperator.GREATER_THAN;
            case "<" -> ComparisonOperator.LESS_THAN;
            case ">=" -> ComparisonOperator.GREATER_THAN_OR_EQUAL;
            case "<=" -> ComparisonOperator.LESS_THAN_OR_EQUAL;
            default -> null;
        };
    }

    /**
     * 将 SpEL 方法名映射为 ComparisonOperator
     */
    private static ComparisonOperator mapSpelMethod(String methodName) {
        return switch (methodName) {
            case "contains" -> ComparisonOperator.CONTAINS;
            case "startsWith" -> ComparisonOperator.STARTS_WITH;
            case "endsWith" -> ComparisonOperator.ENDS_WITH;
            case "isEmpty" -> ComparisonOperator.IS_EMPTY;
            default -> null;
        };
    }

    /**
     * 解析 SpEL 字面量值
     * 支持: 字符串(单引号)、布尔值、null、数字
     */
    private static Object parseLiteralValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String trimmed = rawValue.trim();

        if (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1);
        }

        if ("true".equalsIgnoreCase(trimmed)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(trimmed)) {
            return Boolean.FALSE;
        }

        if ("null".equalsIgnoreCase(trimmed)) {
            return null;
        }

        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException ignored) {
        }

        try {
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException ignored) {
        }

        return trimmed;
    }
}
