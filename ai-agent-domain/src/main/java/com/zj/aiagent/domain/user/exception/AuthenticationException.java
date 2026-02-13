package com.zj.aiagent.domain.user.exception;

/**
 * 用户认证相关业务异常
 */
public class AuthenticationException extends RuntimeException {

    private final ErrorCode errorCode;

    public AuthenticationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AuthenticationException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + ": " + detail);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public enum ErrorCode {
        RATE_LIMITED("操作过于频繁，请稍后再试"),
        TOO_MANY_LOGIN_ATTEMPTS("登录失败次数过多，账号已被锁定15分钟"),
        EMAIL_SEND_FAILED("邮件发送失败"),
        INVALID_VERIFICATION_CODE("验证码无效或已过期"),
        EMAIL_ALREADY_REGISTERED("该邮箱已被注册"),
        INVALID_CREDENTIALS("用户名或密码错误"),
        USER_DISABLED("用户已被禁用"),
        WEAK_PASSWORD("密码强度不足，至少需要8位"),
        PASSWORD_MISMATCH("两次输入的密码不一致"),
        USER_NOT_FOUND("用户不存在"),
        INVALID_REFRESH_TOKEN("Refresh Token 无效或已过期");

        private final String message;

        ErrorCode(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
