package com.zj.aiagent.shared.exception;

/**
 * 技术异常
 * 
 * 用于封装技术层面的异常
 * 例如：数据库连接失败、API调用超时、文件读写失败等
 */
public class TechnicalException extends RuntimeException {

    public TechnicalException(String message) {
        super(message);
    }

    public TechnicalException(String message, Throwable cause) {
        super(message, cause);
    }
}
