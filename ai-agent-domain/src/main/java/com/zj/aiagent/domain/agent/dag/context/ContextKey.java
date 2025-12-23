package com.zj.aiagent.domain.agent.dag.context;

/**
 * DAG 执行上下文 Key 定义
 * 统一管理所有 context 中使用的 key 常量，避免硬编码字符串
 */
public enum ContextKey {

    // ==================== 用户输入 ====================
    /** 用户输入（原始） */
    USER_INPUT("userInput"),
    /** 用户消息 */
    USER_MESSAGE("userMessage"),

    // ==================== 规划与执行 ====================
    /** 规划结果 */
    PLAN_RESULT("plan_result"),
    /** 执行结果 */
    EXECUTION_RESULT("execution_result"),
    /** 执行历史 */
    EXECUTION_HISTORY("execution_history"),
    /** 上一次执行结果 */
    PREVIOUS_EXECUTION_RESULT("previous_execution_result"),

    // ==================== 路由 ====================
    /** 路由决策 */
    ROUTER_DECISION("router_decision"),

    // ==================== 人工介入 ====================
    /** 人工审核结果 */
    HUMAN_APPROVED("human_approved"),
    /** 人工评论 */
    HUMAN_COMMENTS("human_comments"),
    /** 人工介入请求 */
    HUMAN_INTERVENTION_REQUEST("human_intervention_request"),
    /** 暂停时间 */
    PAUSED_AT("paused_at"),
    /** 暂停节点ID */
    PAUSED_NODE_ID("paused_node_id"),
    /** AFTER时机暂停前的初步执行结果（用于恢复时避免重新执行） */
    PRELIMINARY_RESULT("__preliminary_result_"),

    // ==================== React 模式 ====================
    /** React 迭代记录 */
    REACT_ITERATIONS("react_iterations"),

    // ==================== 内部使用 ====================
    /** DAG 图对象 */
    DAG_GRAPH("__DAG_GRAPH__"),
    /** DAG 完成标志 */
    DAG_COMPLETED("dag_completed"),
    /** 进度 - 已完成节点数 */
    PROGRESS_COMPLETED("__PROGRESS_COMPLETED__"),
    /** 进度 - 总节点数 */
    PROGRESS_TOTAL("__PROGRESS_TOTAL__"),
    /** 节点执行日志前缀 */
    NODE_LOG_PREFIX("_node_log_");

    private final String key;

    ContextKey(String key) {
        this.key = key;
    }

    /**
     * 获取 key 字符串
     */
    public String key() {
        return key;
    }

    /**
     * 获取带节点ID后缀的 key（用于 NODE_LOG_PREFIX 等）
     */
    public String forNode(String nodeId) {
        return key + nodeId;
    }

    /**
     * 获取 key 字符串（与 key() 相同，用于字符串拼接场景）
     */
    @Override
    public String toString() {
        return key;
    }
}
