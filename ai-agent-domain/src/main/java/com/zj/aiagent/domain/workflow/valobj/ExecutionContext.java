package com.zj.aiagent.domain.workflow.valobj;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 执行上下文值对象
 * 存储节点输入输出和提供 SpEL 解析能力
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionContext {

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    /**
     * 全局输入参数
     */
    @Builder.Default
    private Map<String, Object> inputs = new ConcurrentHashMap<>();

    /**
     * 节点输出结果 (nodeId -> outputs)
     */
    @Builder.Default
    private Map<String, Map<String, Object>> nodeOutputs = new ConcurrentHashMap<>();

    /**
     * 共享状态数据
     */
    @Builder.Default
    private Map<String, Object> sharedState = new ConcurrentHashMap<>();

    // --- 核心方法 ---

    /**
     * 设置全局输入
     */
    public void setInputs(Map<String, Object> inputs) {
        this.inputs = new ConcurrentHashMap<>(inputs);
    }

    /**
     * 存储节点输出
     */
    public void setNodeOutput(String nodeId, Map<String, Object> outputs) {
        this.nodeOutputs.put(nodeId, new HashMap<>(outputs));
    }

    /**
     * 获取节点输出
     */
    public Map<String, Object> getNodeOutput(String nodeId) {
        return nodeOutputs.getOrDefault(nodeId, new HashMap<>());
    }

    /**
     * 解析 SpEL 表达式
     * 支持:
     * - #{inputs.key}
     * - #{nodeId.output.key}
     * - #{sharedState.key}
     */
    public Object resolve(String expression) {
        if (expression == null || !expression.startsWith("#{") || !expression.endsWith("}")) {
            return expression; // 非表达式，直接返回
        }

        String spelExpression = expression.substring(2, expression.length() - 1);
        EvaluationContext context = buildEvaluationContext();
        Expression exp = PARSER.parseExpression(spelExpression);
        return exp.getValue(context);
    }

    /**
     * 批量解析输入参数
     */
    public Map<String, Object> resolveInputs(Map<String, Object> inputMappings) {
        Map<String, Object> resolved = new HashMap<>();

        if (inputMappings == null) {
            return resolved;
        }

        for (Map.Entry<String, Object> entry : inputMappings.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                resolved.put(entry.getKey(), resolve((String) value));
            } else {
                resolved.put(entry.getKey(), value);
            }
        }

        return resolved;
    }

    /**
     * 构建 SpEL 评估上下文
     */
    private EvaluationContext buildEvaluationContext() {
        StandardEvaluationContext context = new StandardEvaluationContext();

        // 注册 inputs
        context.setVariable("inputs", inputs);

        // 注册 sharedState
        context.setVariable("sharedState", sharedState);

        // 注册所有节点输出
        for (Map.Entry<String, Map<String, Object>> entry : nodeOutputs.entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }

        return context;
    }

    /**
     * 创建快照（用于检查点）
     */
    public ExecutionContext snapshot() {
        return ExecutionContext.builder()
                .inputs(new HashMap<>(this.inputs))
                .nodeOutputs(new HashMap<>(this.nodeOutputs))
                .sharedState(new HashMap<>(this.sharedState))
                .build();
    }
}
