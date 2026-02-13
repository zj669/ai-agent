package com.zj.aiagent.domain.workflow.port;

import com.zj.aiagent.domain.workflow.valobj.ExecutionContext;

import java.util.Map;

/**
 * 表达式解析器端口
 *
 * 提供 SpEL 表达式解析能力，支持从 ExecutionContext 中解析变量引用
 * 实现类位于 Infrastructure 层
 */
public interface ExpressionResolverPort {

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
    Object resolve(String expression, ExecutionContext context);

    /**
     * 批量解析输入参数
     *
     * @param inputMappings 输入参数映射
     * @param context       执行上下文
     * @return 解析后的参数映射
     */
    Map<String, Object> resolveInputs(Map<String, Object> inputMappings, ExecutionContext context);
}
