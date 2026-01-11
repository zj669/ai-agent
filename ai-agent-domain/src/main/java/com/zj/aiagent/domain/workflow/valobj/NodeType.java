package com.zj.aiagent.domain.workflow.valobj;

/**
 * 节点类型枚举
 */
public enum NodeType {
    /**
     * 开始节点
     */
    START,

    /**
     * 结束节点
     */
    END,

    /**
     * LLM 大模型节点
     */
    LLM,

    /**
     * HTTP 请求节点
     */
    HTTP,

    /**
     * 条件路由节点
     */
    CONDITION,

    /**
     * 并行节点
     */
    PARALLEL,

    /**
     * MCP 工具节点
     */
    TOOL
}
