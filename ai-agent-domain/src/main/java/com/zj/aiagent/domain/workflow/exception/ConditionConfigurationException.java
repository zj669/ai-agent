package com.zj.aiagent.domain.workflow.exception;

/**
 * 条件分支配置异常
 * 当条件节点的分支配置校验失败时抛出，例如：
 * - 无 default 分支
 * - 多个 default 分支
 * - 分支列表为空
 */
public class ConditionConfigurationException extends RuntimeException {

    public ConditionConfigurationException(String message) {
        super(message);
    }

    public ConditionConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
