package com.zj.aiagemt.common.design.dag;

import java.util.Set;

/**
 * 条件节点 - 根据执行结果决定下一步执行路径
 *
 * @param <C> Context类型
 */
public interface ConditionalDagNode<C extends DagContext> extends DagNode<C, NodeRouteDecision> {

    /**
     * 评估条件，决定下一步执行哪些节点
     * 
     * @param context 执行上下文
     * @return 路由决策
     */
    @Override
    NodeRouteDecision execute(C context) throws DagNodeExecutionException;

    /**
     * 获取所有可能的下游节点
     * 这些节点不会被视为依赖，而是条件路由的候选节点
     * 
     * @return 候选下游节点ID集合
     */
    Set<String> getCandidateNextNodes();
}
