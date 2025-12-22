package com.zj.aiagent.shared.exception;

/**
 * 幂等异常
 * <p>
 * 当请求被幂等机制拦截时抛出此异常
 *
 * @author zj
 * @since 2025-12-22
 */
public class IdempotentException extends RuntimeException {

    public IdempotentException() {
        super("请勿重复提交");
    }

    public IdempotentException(String message) {
        super(message);
    }

    public IdempotentException(String message, Throwable cause) {
        super(message, cause);
    }
}
