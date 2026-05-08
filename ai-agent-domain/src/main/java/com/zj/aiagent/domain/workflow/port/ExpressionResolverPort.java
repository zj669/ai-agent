package com.zj.aiagent.domain.workflow.port;

import com.zj.aiagent.domain.workflow.valobj.ExecutionContext;

import java.util.Map;

/**
 * 工作流值引用解析器端口
 *
 * 标准引用格式：
 * - inputs.key
 * - nodeId.output.key
 * - sharedState.key
 *
 * 实现可兼容历史 SpEL，但新增图不得继续依赖 SpEL 作为节点入参契约。
 * 实现类位于 Infrastructure 层
 */
public interface ExpressionResolverPort {

    /**
     * 解析工作流值引用。
     *
     * @param expression 表达式字符串
     * @param context    执行上下文
     * @return 解析结果；引用解析失败时抛出异常
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
