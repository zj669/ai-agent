package com.zj.aiagent.infrastructure.workflow.expression;

import com.zj.aiagent.domain.workflow.port.ExpressionResolverPort;
import com.zj.aiagent.domain.workflow.valobj.ExecutionContext;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 工作流值引用解析器实现。
 *
 * 标准引用格式：
 * - inputs.key
 * - nodeId.output.key
 * - sharedState.key
 *
 * 兼容历史 SpEL：#{#nodeOutputs['nodeId']['key']}。
 *
 * 注意：此类位于 infrastructure 层，保持 domain 层纯净
 */
@Slf4j
@Component
public class ExpressionResolver implements ExpressionResolverPort {

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final String INPUTS_PREFIX = "inputs.";
    private static final String SHARED_STATE_PREFIX = "sharedState.";
    private static final String LEGACY_STATE_PREFIX = "state.";
    private static final String LEGACY_NODES_PREFIX = "nodes.";
    private static final String OUTPUT_SEGMENT = ".output.";

    /**
     * 解析工作流标准值引用或历史 SpEL。
     *
     * @param expression 表达式字符串
     * @param context    执行上下文
     * @return 解析结果；引用解析失败时抛出异常
     */
    @Override
    public Object resolve(String expression, ExecutionContext context) {
        if (expression == null) {
            return null;
        }

        String trimmed = expression.trim();
        if (trimmed.isEmpty()) {
            return expression;
        }

        if (trimmed.startsWith("#{") && trimmed.endsWith("}")) {
            return resolveLegacySpel(trimmed, context);
        }

        if (trimmed.startsWith(INPUTS_PREFIX)) {
            return resolveInputsReference(trimmed, context);
        }

        if (trimmed.startsWith(SHARED_STATE_PREFIX)) {
            return resolveSharedStateReference(
                trimmed,
                SHARED_STATE_PREFIX,
                context
            );
        }

        if (trimmed.startsWith(LEGACY_STATE_PREFIX)) {
            return resolveSharedStateReference(
                trimmed,
                LEGACY_STATE_PREFIX,
                context
            );
        }

        if (trimmed.startsWith(LEGACY_NODES_PREFIX)) {
            return resolveLegacyNodesReference(trimmed, context);
        }

        if (trimmed.contains(OUTPUT_SEGMENT)) {
            return resolveNodeOutputReference(trimmed, context);
        }

        return expression;
    }

    /**
     * 批量解析输入参数
     *
     * @param inputMappings 输入参数映射
     * @param context       执行上下文
     * @return 解析后的参数映射
     */
    @Override
    public Map<String, Object> resolveInputs(Map<String, Object> inputMappings, ExecutionContext context) {
        Map<String, Object> resolved = new HashMap<>();

        if (inputMappings == null) {
            return resolved;
        }

        for (Map.Entry<String, Object> entry : inputMappings.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                try {
                    resolved.put(entry.getKey(), resolve((String) value, context));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                        "引用解析失败：input=" +
                            entry.getKey() +
                            " ref=" +
                            value +
                            " reason=" +
                            e.getMessage(),
                        e
                    );
                }
            } else {
                resolved.put(entry.getKey(), value);
            }
        }

        return resolved;
    }

    private Object resolveInputsReference(
        String reference,
        ExecutionContext context
    ) {
        String key = reference.substring(INPUTS_PREFIX.length());
        if (key.isBlank()) {
            throw new IllegalArgumentException("全局输入引用缺少 key");
        }

        Map<String, Object> inputs = context != null ? context.getInputs() : null;
        if (inputs == null || !inputs.containsKey(key)) {
            throw new IllegalArgumentException("全局输入不存在: " + reference);
        }

        return inputs.get(key);
    }

    private Object resolveSharedStateReference(
        String reference,
        String prefix,
        ExecutionContext context
    ) {
        String key = reference.substring(prefix.length());
        if (key.isBlank()) {
            throw new IllegalArgumentException("共享状态引用缺少 key");
        }

        Map<String, Object> sharedState =
            context != null ? context.getSharedState() : null;
        if (sharedState == null || !sharedState.containsKey(key)) {
            throw new IllegalArgumentException("共享状态不存在: " + reference);
        }

        return sharedState.get(key);
    }

    private Object resolveNodeOutputReference(
        String reference,
        ExecutionContext context
    ) {
        int outputIndex = reference.indexOf(OUTPUT_SEGMENT);
        if (
            outputIndex <= 0 ||
            outputIndex >= reference.length() - OUTPUT_SEGMENT.length()
        ) {
            throw new IllegalArgumentException(
                "节点输出引用格式错误，期望 nodeId.output.key: " + reference
            );
        }

        String nodeId = reference.substring(0, outputIndex);
        String path = reference.substring(outputIndex + OUTPUT_SEGMENT.length());
        return resolveNodeOutputPath(reference, nodeId, path, context);
    }

    private Object resolveLegacyNodesReference(
        String reference,
        ExecutionContext context
    ) {
        String rest = reference.substring(LEGACY_NODES_PREFIX.length());
        int dotIndex = rest.indexOf('.');
        if (dotIndex <= 0 || dotIndex >= rest.length() - 1) {
            throw new IllegalArgumentException(
                "历史节点引用格式错误，期望 nodes.nodeId.key: " + reference
            );
        }

        String nodeId = rest.substring(0, dotIndex);
        String path = rest.substring(dotIndex + 1);
        return resolveNodeOutputPath(reference, nodeId, path, context);
    }

    private Object resolveNodeOutputPath(
        String reference,
        String nodeId,
        String path,
        ExecutionContext context
    ) {
        Map<String, Object> nodeOutput =
            context != null ? context.getNodeOutput(nodeId) : null;
        if (nodeOutput == null) {
            throw new IllegalArgumentException(
                "上游节点输出不存在: nodeId=" + nodeId
            );
        }

        return resolvePath(nodeOutput, path, reference);
    }

    private Object resolvePath(Object root, String path, String reference) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("引用路径缺少字段: " + reference);
        }

        Object current = root;
        for (String segment : path.split("\\.")) {
            if (segment.isBlank()) {
                throw new IllegalArgumentException(
                    "引用路径包含空字段: " + reference
                );
            }

            if (current instanceof Map<?, ?> map) {
                if (!map.containsKey(segment)) {
                    throw new IllegalArgumentException(
                        "上游输出字段不存在: " + reference
                    );
                }
                current = map.get(segment);
                continue;
            }

            if (current instanceof List<?> list) {
                int index;
                try {
                    index = Integer.parseInt(segment);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                        "数组引用下标不是数字: " + reference
                    );
                }
                if (index < 0 || index >= list.size()) {
                    throw new IllegalArgumentException(
                        "数组引用下标越界: " + reference
                    );
                }
                current = list.get(index);
                continue;
            }

            throw new IllegalArgumentException(
                "引用路径无法继续解析: " + reference
            );
        }

        return current;
    }

    private Object resolveLegacySpel(String expression, ExecutionContext context) {
        try {
            String spelExpression = expression.substring(2, expression.length() - 1);
            EvaluationContext evaluationContext = buildEvaluationContext(context);
            Expression exp = PARSER.parseExpression(spelExpression);
            return exp.getValue(evaluationContext);
        } catch (Exception e) {
            log.warn(
                "历史 SpEL 表达式解析失败: expression={}, error={}",
                expression,
                e.getMessage()
            );
            throw new IllegalArgumentException(
                "历史 SpEL 表达式解析失败: " + expression
            );
        }
    }

    /**
     * 构建 SpEL 评估上下文
     */
    private EvaluationContext buildEvaluationContext(ExecutionContext context) {
        StandardEvaluationContext evaluationContext = new StandardEvaluationContext();

        // 注册 inputs
        evaluationContext.setVariable("inputs", context.getInputs());

        // 注册 sharedState
        evaluationContext.setVariable("sharedState", context.getSharedState());

        // 注册所有节点输出（两种方式访问）
        // 1. 整体注册为 nodeOutputs map，支持 #nodeOutputs['llm-1']['response'] 语法
        evaluationContext.setVariable("nodeOutputs", context.getNodeOutputs());
        // 2. 逐个注册（仅对合法变量名有效，如 start, end）
        for (Map.Entry<String, Map<String, Object>> entry : context.getNodeOutputs().entrySet()) {
            evaluationContext.setVariable(entry.getKey(), entry.getValue());
        }

        return evaluationContext;
    }
}
