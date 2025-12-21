package com.zj.aiagent.shared.exception;

/**
 * 业务异常
 * 
 * 用于封装业务规则验证失败的异常
 * 例如：用户名已存在、余额不足、权限不足等
 */
public class BusinessException extends DomainException {

    private final String errorCode;

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
