package com.zj.aiagent.domain.agent.dag.exception;

/**
 * 节点配置异常
 */
public class NodeConfigException extends RuntimeException {

    public NodeConfigException(String message) {
        super(message);
    }

    public NodeConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
