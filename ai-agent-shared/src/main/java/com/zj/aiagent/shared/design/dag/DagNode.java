package com.zj.aiagent.shared.design.dag;

import java.util.Set;

/**
 * DAG节点接口 - 所有节点的基础接口
 *
 * @param <C> Context类型 - 执行上下文
 * @param <R> Result类型 - 节点执行结果
 */
public interface DagNode<C extends DagContext, R> {

    /**
     * 获取节点唯一标识
     * 
     * @return 节点ID（全局唯一）
     */
    String getNodeId();

    /**
     * 获取节点名称（用于日志和监控）
     * 
     * @return 节点名称
     */
    default String getNodeName() {
        return getNodeId();
    }

    /**
     * 获取节点依赖的其他节点ID列表
     * 
     * @return 依赖节点ID集合，空集合表示无依赖
     */
    Set<String> getDependencies();

    /**
     * 执行节点逻辑
     * 
     * @param context 执行上下文
     * @return 节点执行结果
     * @throws DagNodeExecutionException 节点执行异常
     */
    R execute(C context) throws DagNodeExecutionException;

    /**
     * 节点执行前的回调（可选）
     * 用于资源准备、参数校验等
     * 
     * @param context 执行上下文
     */
    default void beforeExecute(C context) {
        // 默认空实现
    }

    /**
     * 节点执行后的回调（可选）
     * 用于资源清理、日志记录等
     * 
     * @param context   执行上下文
     * @param result    执行结果
     * @param exception 执行异常（如果有）
     */
    default void afterExecute(C context, R result, Exception exception) {
        // 默认空实现
    }

    /**
     * 是否允许并行执行
     * 
     * @return true表示可以与其他无依赖节点并行执行，默认为true
     */
    default boolean isParallelizable() {
        return true;
    }

    /**
     * 获取节点超时时间（毫秒）
     * 
     * @return 超时时间，0或负数表示无超时限制
     */
    default long getTimeoutMillis() {
        return 0;
    }
}
