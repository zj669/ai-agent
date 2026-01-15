package com.zj.aiagent.domain.workflow.valobj;

/**
 * 工作流执行模式
 * 定义工作流执行的不同运行模式
 */
public enum ExecutionMode {
    /**
     * 标准模式 - 正常执行，持久化所有数据，INFO 级别日志
     */
    STANDARD("standard", "标准模式"),

    /**
     * 调试模式 - 启用详细日志（DEBUG/TRACE），持久化数据，用于开发调试
     */
    DEBUG("debug", "调试模式"),

    /**
     * 干运行模式 - 模拟执行，不持久化数据，不调用真实外部服务，用于测试验证
     */
    DRY_RUN("dry_run", "干运行模式");

    private final String code;
    private final String description;

    ExecutionMode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据 code 获取枚举
     */
    public static ExecutionMode fromCode(String code) {
        for (ExecutionMode mode : values()) {
            if (mode.code.equalsIgnoreCase(code)) {
                return mode;
            }
        }
        return STANDARD; // 默认返回标准模式
    }
}
