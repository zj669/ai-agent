package com.zj.aiagent.domain.workflow.valobj;

/**
 * 触发阶段枚举
 */
public enum TriggerPhase {
    /**
     * 执行前（修改输入）
     */
    BEFORE_EXECUTION,

    /**
     * 执行后（修改输出）
     */
    AFTER_EXECUTION
}
