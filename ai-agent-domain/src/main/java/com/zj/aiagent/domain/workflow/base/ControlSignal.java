package com.zj.aiagent.domain.workflow.base;

/**
 * 控制信号
 * 
 * 节点执行后的控制指令，告诉调度器下一步动作
 */
public enum ControlSignal {

    /**
     * 继续执行 - 按边定义执行下游节点
     */
    CONTINUE,

    /**
     * 暂停执行 - 等待人工介入或外部事件
     */
    PAUSE,

    /**
     * 结束执行 - 工作流正常结束
     */
    END,

    /**
     * 错误 - 工作流异常终止
     */
    ERROR
}
