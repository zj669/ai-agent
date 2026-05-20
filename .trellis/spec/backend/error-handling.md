# Error Handling

> Backend exception contracts for the Spring Boot / DDD modules.

---

## Scope

This guide covers exception flow across `ai-agent-domain`,
`ai-agent-application`, `ai-agent-infrastructure`, `ai-agent-interfaces`, and
`ai-agent-shared`.

The project uses exceptions for failed invariants and technical failures, then
translates those exceptions at the HTTP boundary into `Response<T>` JSON.

Error handling must keep these boundaries clear:

- Domain code raises business exceptions or Java standard exceptions.
- Application services may validate orchestration state and rethrow meaningful
  exceptions.
- Infrastructure adapters wrap technical failures when the caller needs a
  domain-level or application-level message.
- Interfaces code owns HTTP status mapping and client response shape.

---

## Exception Hierarchy

There is no single project-wide base exception today. The current hierarchy is
small and explicit:

| Type | Layer | Purpose |
| --- | --- | --- |
| `AuthenticationException` | Domain | User/auth business failures with `ErrorCode` values. |
| `ConditionConfigurationException` | Domain | Invalid workflow condition branch configuration. |
| `IllegalArgumentException` | Domain/Application/Infrastructure | Invalid caller input or malformed config. |
| `IllegalStateException` | Domain/Application | Invalid lifecycle transition or missing state. |
| `SecurityException` | Application | Ownership or permission failures. |
| `OptimisticLockingFailureException` | Infrastructure/Application boundary | Concurrent workflow review/update conflicts. |
| `RuntimeException` wrappers | Infrastructure | Technical serialization, Redis, vector, or storage failures with cause preserved. |

### Real Example: Domain Exception With Error Code

File:
`ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/exception/AuthenticationException.java`

```java
public class AuthenticationException extends RuntimeException {

    private final ErrorCode errorCode;

    public AuthenticationException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public enum ErrorCode {
        RATE_LIMITED("操作过于频繁，请稍后再试"),
        INVALID_CREDENTIALS("用户名或密码错误"),
        USER_DISABLED("用户已被禁用"),
        INVALID_REFRESH_TOKEN("Refresh Token 无效或已过期");
    }
}
```

Use this pattern when the caller needs stable classification, not just a string.
Keep the error code close to the exception class that owns the business concept.

### Real Example: Domain Configuration Exception

File:
`ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/exception/ConditionConfigurationException.java`

```java
public class ConditionConfigurationException extends RuntimeException {

    public ConditionConfigurationException(String message) {
        super(message);
    }

    public ConditionConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

The corresponding validator throws it from
`ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/condition/StructuredConditionEvaluator.java`
when branch lists are empty, missing `default`, or define multiple defaults.

---

## Layer Responsibilities

### Domain Layer

Domain objects and services enforce business invariants and lifecycle rules.
They should not know HTTP status codes, controller DTOs, or response wrappers.

Examples:

- `UserAuthenticationDomainService` throws `AuthenticationException` for rate
  limits, invalid verification codes, duplicate email, weak passwords, disabled
  users, and invalid credentials.
- `Execution.start` throws `IllegalStateException` when a workflow graph has a
  cycle.
- `Execution.resume` and `Execution.reject` throw `IllegalStateException` or
  `IllegalArgumentException` when resume/reject state does not match the paused
  node.

### Application Layer

Application services orchestrate repositories, ports, and domain objects. They
may add context, lock before mutation, or translate a low-level conflict into a
known framework exception already mapped by the HTTP boundary.

Example:
`ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java`
checks `expectedVersion` during review resume and throws
`OptimisticLockingFailureException` on version conflict.

### Infrastructure Layer

Infrastructure code is allowed to catch technical exceptions from clients,
serialization, Redis, Milvus, MinIO, Spring AI, or HTTP libraries. It must either:

- return a deliberate fallback documented by the port contract, or
- wrap and rethrow with the original cause.

Example:
`RedisSsePublisher.publish` catches `JsonProcessingException`, logs the local
SSE publishing context, and throws `RuntimeException("Failed to serialize SSE payload", e)`.

### Interfaces Layer

Controllers and advice classes translate exceptions into HTTP status and JSON.
Do not duplicate status mapping in every controller. Prefer throwing an
exception and letting `GlobalExceptionHandler` return the client shape.

Controllers may return `Response.error(...)` directly only for local auth checks
that do not fit a thrown exception yet, as seen in `UserController`.

---

## Global Exception Handler

The project-wide HTTP mapping is in:
`ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/advice/GlobalExceptionHandler.java`

It uses:

- `@RestControllerAdvice`
- one `@ExceptionHandler` method per exception category
- `@ResponseStatus` for simple mappings
- `ResponseEntity<Response<Void>>` where the handler must compute status at runtime
- `Response.error(code, message)` as the JSON body

### Real Example: Authentication Exception Mapping

File:
`ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/advice/GlobalExceptionHandler.java`

```java
@ExceptionHandler(AuthenticationException.class)
public ResponseEntity<Response<Void>> handleAuthenticationException(
    AuthenticationException e
) {
    log.warn("Authentication error: {}", e.getMessage());
    HttpStatus httpStatus = switch (e.getErrorCode()) {
        case INVALID_CREDENTIALS, USER_DISABLED, INVALID_REFRESH_TOKEN -> HttpStatus.UNAUTHORIZED;
        case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
        default -> HttpStatus.BAD_REQUEST;
    };
    return ResponseEntity.status(httpStatus)
        .body(Response.error(httpStatus.value(), e.getMessage()));
}
```

Rules for extending this pattern:

- Add a handler when a new exception family needs stable HTTP behavior.
- Keep business classification in the domain exception, not in a string parser.
- Use `WARN` for expected client/business failures.
- Use `ERROR` only for unexpected server failures or technical faults.
- Preserve the cause when wrapping exceptions below this layer.

### Real Example: Validation and Catch-All Mapping

File:
`ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/advice/GlobalExceptionHandler.java`

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public Response<Void> handleValidationException(MethodArgumentNotValidException e) {
    String message = e.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(FieldError::getDefaultMessage)
        .collect(Collectors.joining("; "));
    log.warn("Validation error: {}", message);
    return Response.error(400, message);
}

@ExceptionHandler(Exception.class)
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public Response<Void> handleException(Exception e) {
    log.error("Unexpected error", e);
    return Response.error(500, "服务器内部错误");
}
```

Catch-all handlers must not leak exception messages to clients. They may log the
stack trace server-side and return a stable generic message.

---

## Error Codes & Response Shape

The uniform wrapper is:
`ai-agent-shared/src/main/java/com/zj/aiagent/shared/response/Response.java`

```java
public class Response<T> {
    private int code;
    private String message;
    private T data;
    private boolean success;

    public static <T> Response<T> success(T data) {
        return Response.<T>builder()
                .code(200)
                .message("success")
                .data(data)
                .success(true)
                .build();
    }

    public static <T> Response<T> error(int code, String message) {
        return Response.<T>builder()
                .code(code)
                .message(message)
                .success(false)
                .build();
    }
}
```

Error responses should be shaped as:

```json
{
  "code": 400,
  "message": "Illegal argument message",
  "data": null,
  "success": false
}
```

Success responses should use `Response.success(data)` unless the endpoint has a
documented exception. Review endpoints currently return raw DTOs/empty `200` in
some places; do not broaden that inconsistency without checking consumers.

---

## Async/SSE Exception Special Cases

SSE and async request failures can occur after the HTTP response is partially
written. In that state the handler cannot safely return JSON.

`GlobalExceptionHandler` therefore returns `void` for:

- `AsyncRequestNotUsableException`
- `AsyncRequestTimeoutException`

This is intentional. Do not replace these handlers with `Response.error(...)`.

---

## Anti-Patterns

| Anti-pattern | Why it is wrong | Replacement |
| --- | --- | --- |
| Swallowing exceptions silently | Operators lose the failure reason and workflow state becomes misleading. | Log with context and rethrow, or document a deliberate fallback. |
| Returning `null` for failed invariants | Forces every caller to guess whether `null` means missing, invalid, or failed. | Throw a typed exception or return `Optional` for legitimate absence. |
| Throwing generic `RuntimeException` from domain rules | HTTP mapping cannot distinguish user errors from server errors. | Use `AuthenticationException`, `IllegalArgumentException`, or a new domain exception. |
| Logging full stack traces at every layer | Produces duplicate logs and hides the boundary that owns the failure. | Log where context is added; let `GlobalExceptionHandler` log unexpected top-level failures. |
| Leaking internal exception messages to clients | Technical messages may expose secrets, SQL, filesystem paths, or provider details. | Return stable public messages from the handler. |
| Returning `Response.error(...)` deep below controllers | Couples domain/application code to HTTP response shape. | Throw exceptions below interfaces; translate once at the boundary. |
| Mapping SSE disconnects to JSON | The response stream may already be committed. | Keep the current `void` async handlers. |

---

## Checklist

- [ ] Does the exception originate in the layer that owns the invariant?
- [ ] Is the exception type specific enough for `GlobalExceptionHandler`?
- [ ] Does the infrastructure wrapper preserve the original cause?
- [ ] Does the client receive `Response<T>` with `code`, `message`, `data`, and `success`?
- [ ] Are expected business failures logged at `WARN`, not `ERROR`?
- [ ] Are unexpected failures logged once with stack trace?
- [ ] Are SSE disconnects/async timeouts handled without trying to write JSON?
- [ ] Did you avoid adding HTTP imports to domain/application code?
