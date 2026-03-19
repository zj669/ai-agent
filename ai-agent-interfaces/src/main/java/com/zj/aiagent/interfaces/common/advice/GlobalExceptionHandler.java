package com.zj.aiagent.interfaces.common.advice;

import com.zj.aiagent.domain.user.exception.AuthenticationException;
import com.zj.aiagent.shared.response.Response;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

/**
 * 全局异常处理器
 *
 * 统一处理控制器层抛出的异常，返回标准响应格式
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理异步请求超时 (SSE/Websocket)
     * 避免返回 JSON 导致 HttpMessageNotWritableException
     */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public void handleAsyncRequestTimeoutException(
        AsyncRequestTimeoutException e
    ) {
        // SSE 超时通常由 SseEmitter.onTimeout 处理，这里只需静默吞掉异常，避免全局异常拦截器尝试写入 JSON
        log.debug("Async request timed out: {}", e.getMessage());
    }

    /**
     * 处理认证相关异常
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Response<Void>> handleAuthenticationException(
        AuthenticationException e
    ) {
        log.warn("Authentication error: {}", e.getMessage());
        HttpStatus httpStatus = switch (e.getErrorCode()) {
            case INVALID_CREDENTIALS, USER_DISABLED -> HttpStatus.UNAUTHORIZED;
            case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(httpStatus)
            .body(Response.error(httpStatus.value(), e.getMessage()));
    }

    /**
     * 处理参数校验异常 (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<Void> handleValidationException(
        MethodArgumentNotValidException e
    ) {
        String message = e
            .getBindingResult()
            .getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining("; "));
        log.warn("Validation error: {}", message);
        return Response.error(400, message);
    }

    /**
     * 处理约束违规异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<Void> handleConstraintViolation(
        ConstraintViolationException e
    ) {
        log.warn("Constraint violation: {}", e.getMessage());
        return Response.error(400, e.getMessage());
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        return Response.error(400, e.getMessage());
    }

    /**
     * 处理非法状态异常
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<Void> handleIllegalState(IllegalStateException e) {
        log.warn("Illegal state: {}", e.getMessage());
        return Response.error(400, e.getMessage());
    }

    /**
     * 处理乐观锁冲突
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Response<Void> handleOptimisticLockingFailure(
        OptimisticLockingFailureException e
    ) {
        log.warn("Optimistic locking conflict: {}", e.getMessage());
        return Response.error(409, "审核状态已变化，请刷新后重试");
    }

    /**
     * 处理权限相关异常
     */
    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Response<Void> handleSecurityException(SecurityException e) {
        log.warn("Forbidden: {}", e.getMessage());
        return Response.error(403, e.getMessage());
    }

    /**
     * 兜底处理未知异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Response<Void> handleException(Exception e) {
        log.error("Unexpected error", e);
        return Response.error(500, "服务器内部错误");
    }
}
