package com.zj.aiagent.infrastructure.workflow.expression;

import com.zj.aiagent.domain.workflow.port.ExpressionResolverPort;
import com.zj.aiagent.domain.workflow.valobj.ExecutionContext;
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
 * 表达式解析器实现
 *
 * 提供 SpEL 表达式解析能力，实现 ExpressionResolverPort 端口
 * 支持 #{inputs.key}, #{nodeId.output.key}, #{sharedState.key} 格式
 *
 * 注意：此类位于 infrastructure 层，保持 domain 层纯净
 */
@Slf4j
@Component
public class ExpressionResolver implements ExpressionResolverPort {

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    /**
     * 解析 SpEL 表达式
     * 支持:
     * - #{inputs.key}
     * - #{nodeId.output.key}
     * - #{sharedState.key}
     *
     * @param expression 表达式字符串
     * @param context    执行上下文
     * @return 解析结果，解析失败时返回原始表达式
     */
    @Override
    public Object resolve(String expression, ExecutionContext context) {
        if (expression == null || !expression.startsWith("#{") || !expression.endsWith("}")) {
            return expression; // 非表达式，直接返回
        }

        try {
            String spelExpression = expression.substring(2, expression.length() - 1);
            EvaluationContext evaluationContext = buildEvaluationContext(context);
            Expression exp = PARSER.parseExpression(spelExpression);
            return exp.getValue(evaluationContext);
        } catch (Exception e) {
            log.warn("SpEL 表达式解析失败: expression={}, error={}", expression, e.getMessage());
            return expression;
        }
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
                resolved.put(entry.getKey(), resolve((String) value, context));
            } else {
                resolved.put(entry.getKey(), value);
            }
        }

        return resolved;
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

        // 注册所有节点输出
        for (Map.Entry<String, Map<String, Object>> entry : context.getNodeOutputs().entrySet()) {
            evaluationContext.setVariable(entry.getKey(), entry.getValue());
        }

        return evaluationContext;
    }
}
