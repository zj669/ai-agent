package com.zj.aiagemt.service.dag.exception;

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
