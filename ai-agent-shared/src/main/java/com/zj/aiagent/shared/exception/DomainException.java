package com.zj.aiagent.shared.exception;

/**
 * 领域异常基类
 * 
 * 用于封装领域层的业务规则违反、不变性约束破坏等异常
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
