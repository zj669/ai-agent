package com.zj.aiagemt.common.auth.exception;

/**
 * 认证异常
 * 
 * <p>
 * 当认证过程中发生错误时抛出此异常，包括但不限于：
 * <ul>
 * <li>认证信息格式错误</li>
 * <li>认证信息验证失败</li>
 * <li>Token过期或无效</li>
 * </ul>
 * 
 * @author zj
 * @since 2025-12-20
 */
public class AuthenticationException extends RuntimeException {

    /**
     * 构造认证异常
     * 
     * @param message 异常消息
     */
    public AuthenticationException(String message) {
        super(message);
    }

    /**
     * 构造认证异常
     * 
     * @param message 异常消息
     * @param cause   原因异常
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
