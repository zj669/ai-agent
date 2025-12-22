package com.zj.aiagent.interfaces.common.advice;

import com.zj.aiagent.interfaces.common.Response;
import com.zj.aiagent.shared.exception.IdempotentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author zj
 * @since 2025-12-22
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理幂等异常 - 重复请求
     */
    @ExceptionHandler(IdempotentException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Response<Void> handleIdempotentException(IdempotentException e) {
        log.warn("幂等异常: {}", e.getMessage());
        return Response.fail("429", e.getMessage());
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数异常: {}", e.getMessage());
        return Response.fail("400", e.getMessage());
    }

    /**
     * 处理未知异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Response<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Response.fail("500", "系统繁忙，请稍后再试");
    }
}
