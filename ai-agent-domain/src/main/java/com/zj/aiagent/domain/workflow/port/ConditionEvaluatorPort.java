package com.zj.aiagent.domain.workflow.port;

import com.zj.aiagent.domain.workflow.valobj.ConditionBranch;
import com.zj.aiagent.domain.workflow.valobj.ExecutionContext;

import java.util.List;

/**
 * 条件评估器端口接口
 * 负责评估条件分支列表，返回命中的分支
 *
 * 实现类位于 Infrastructure 层（StructuredConditionEvaluator）
 */
public interface ConditionEvaluatorPort {

    /**
     * 评估分支列表，返回命中的分支
     *
     * @param branches 按优先级排序的分支列表（必须包含恰好一个 default 分支）
     * @param context  执行上下文，用于变量引用解析
     * @return 命中的分支；如果没有非 default 分支匹配，则返回 default 分支
     * @throws com.zj.aiagent.domain.workflow.exception.ConditionConfigurationException
     *         当分支配置无效时（无 default、多 default 等）
     */
    ConditionBranch evaluate(List<ConditionBranch> branches, ExecutionContext context);
}
