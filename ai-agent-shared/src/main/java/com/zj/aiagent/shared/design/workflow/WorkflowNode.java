package com.zj.aiagent.shared.design.workflow;

/**
 * 工作流节点接口
 * 
 * 借鉴 LangGraph 设计：
 * - 节点接收 State，返回 StateUpdate
 * - 节点不关心路由，路由由边决定
 */
public interface WorkflowNode {

    /**
     * 获取节点ID
     */
    String getNodeId();

    /**
     * 获取节点名称（用于日志和监控）
     */
    default String getNodeName() {
        return getNodeId();
    }

    /**
     * 获取节点类型
     */
    String getNodeType();

    /**
     * 执行节点
     * 
     * @param state 当前工作流状态
     * @return 状态更新（包含更新的字段和控制信号）
     */
    StateUpdate execute(WorkflowState state);

    /**
     * 节点执行前回调
     */
    default void beforeExecute(WorkflowState state) {
        // 默认空实现
    }

    /**
     * 节点执行后回调
     */
    default void afterExecute(WorkflowState state, StateUpdate result, Exception exception) {
        // 默认空实现
    }

    /**
     * 获取超时时间（毫秒），0表示无超时
     */
    default long getTimeoutMillis() {
        return 0;
    }

}
