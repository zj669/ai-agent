package com.zj.aiagent.domain.workflow.valobj;

/**
 * 工作流执行模式
 * 定义工作流执行的不同运行模式
 */
public enum ExecutionMode {
    /**
     * 标准模式 - 正常执行，持久化所有数据，INFO 级别日志
     */
    STANDARD,

    /**
     * 调试模式 - 启用详细日志（DEBUG/TRACE），持久化数据，用于开发调试
     */
    DEBUG,

    /**
     * 干运行模式 - 模拟执行，不持久化数据，不调用真实外部服务，用于测试验证
     */
    DRY_RUN;

    /**
     * 根据 code 获取枚举
     */
    public static ExecutionMode fromCode(String code) {
        for (ExecutionMode mode : values()) {
            if (mode.name().equalsIgnoreCase(code)) {
                return mode;
            }
        }
        return STANDARD;
    }
}
