package com.zj.aiagent.domain.workflow.valobj;

/**
 * SSE 事件类型枚举
 * 定义事件生命周期
 */
public enum SseEventType {
    /**
     * 节点开始执行
     */
    START,

    /**
     * 流式更新（追加内容）
     */
    UPDATE,

    /**
     * 节点执行完成
     */
    FINISH,

    /**
     * 错误
     */
    ERROR
}
