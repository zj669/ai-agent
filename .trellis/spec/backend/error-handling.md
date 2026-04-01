# Error Handling

> How errors are handled in the AI Agent Platform backend.

---

## Overview

The project uses a **layered error propagation** strategy aligned with DDD:

1. **Domain layer** — throws domain-specific exceptions (pure Java, no Spring)
2. **Application layer** — may catch and rethrow or let exceptions propagate
3. **Infrastructure layer** — wraps technical exceptions (DB, Redis, external API)
4. **Interfaces layer** — `GlobalExceptionHandler` (`@RestControllerAdvice`) catches all and returns standardized `Response<T>`

---

## Unified Response Format

All API responses use `com.zj.aiagent.shared.response.Response<T>`:

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Response<T> {
    private int code;        // HTTP-like status code
    private String message;  // Human-readable message
    private T data;          // Response payload (null on error)
    private boolean success; // true/false flag

    public static <T> Response<T> success(T data) { ... }
    public static <T> Response<T> success() { ... }
    public static <T> Response<T> error(int code, String message) { ... }
}
```

### Success Response Example

```json
{ "code": 200, "message": "success", "data": { ... }, "success": true }
```

### Error Response Example

```json
{ "code": 401, "message": "Token expired", "data": null, "success": false }
```

---

## Error Types

### Domain Exceptions

Defined in `domain/{module}/exception/` — pure Java, NO Spring dependencies.

| Exception | Module | When to Throw |
|-----------|--------|---------------|
| `AuthenticationException` | `domain/user/exception/` | Invalid credentials, disabled user, rate limited |
| `DagNodeExecutionException` | `shared/design/dag/` | Node execution failures in workflow DAG |
| `IllegalArgumentException` | JDK built-in | Invalid business parameters (graph validation, etc.) |
| `IllegalStateException` | JDK built-in | Invalid state transitions |
| `ConcurrentModificationException` | JDK built-in | Optimistic lock violations in domain entities |

### AuthenticationException Pattern

Uses an error code enum for fine-grained control:

```java
// domain/user/exception/AuthenticationException.java
public class AuthenticationException extends RuntimeException {
    private final ErrorCode errorCode;

    public enum ErrorCode {
        INVALID_CREDENTIALS,
        USER_DISABLED,
        RATE_LIMITED,
        // ...
    }
}
```

### Infrastructure Exceptions

Infrastructure wraps technical failures:

| Exception | Source | Handling |
|-----------|--------|----------|
| `OptimisticLockingFailureException` | Spring DAO | Caught in GlobalExceptionHandler → 409 |
| `ConstraintViolationException` | Jakarta Validation | Caught → 400 with field details |
| `MethodArgumentNotValidException` | Spring `@Valid` | Caught → 400 with validation errors |
| `AsyncRequestTimeoutException` | Spring Async | Silently handled (SSE timeout) |

---

## Global Exception Handler

Located at `ai-agent-interfaces/.../common/advice/GlobalExceptionHandler.java`:

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // SSE/WebSocket timeout — silently swallowed
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public void handleAsyncRequestTimeoutException(AsyncRequestTimeoutException e) {
        log.debug("Async request timed out: {}", e.getMessage());
    }

    // Auth errors — mapped to specific HTTP status
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Response<Void>> handleAuthenticationException(AuthenticationException e) {
        log.warn("Authentication error: {}", e.getMessage());
        HttpStatus httpStatus = switch (e.getErrorCode()) {
            case INVALID_CREDENTIALS, USER_DISABLED -> HttpStatus.UNAUTHORIZED;
            case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(httpStatus)
            .body(Response.error(httpStatus.value(), e.getMessage()));
    }

    // Validation errors — extract field messages
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response<Void>> handleValidation(MethodArgumentNotValidException e) { ... }

    // Optimistic lock — 409 Conflict
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Response<Void>> handleOptimisticLock(...) { ... }

    // Catch-all — 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<Void>> handleGeneric(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(500)
            .body(Response.error(500, "Internal server error"));
    }
}
```

---

## Error Handling Patterns

### Pattern 1: Domain Business Rule Violation

Domain entities throw standard Java exceptions:

```java
// domain/workflow/entity/Execution.java
public void advance(String nodeId, NodeExecutionResult result) {
    if (status != ExecutionStatus.RUNNING) {
        throw new IllegalStateException("Cannot advance: execution is " + status);
    }
    // business logic...
}
```

### Pattern 2: Repository Not Found

Return `Optional` and let the caller decide:

```java
// Domain interface
Optional<Agent> findById(Long id);

// Application service
Agent agent = agentRepository.findById(id)
    .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + id));
```

### Pattern 3: Optimistic Lock in Repository

```java
int rows = agentMapper.updateById(po);
if (rows == 0) {
    throw new OptimisticLockingFailureException(
        "Update failed: Agent modified by another user (Optimistic Lock)");
}
```

### Pattern 4: DAG Node Execution Error

```java
// Retryable errors
throw new DagNodeExecutionException("LLM call failed", cause, nodeId, true);

// Non-retryable errors
throw new DagNodeExecutionException("Invalid config", null, nodeId, false);
```

---

## Error Propagation Rules

```
Domain Entity        → throws IllegalArgumentException / IllegalStateException / domain exceptions
  ↓
Repository Impl      → wraps DB errors in Spring exceptions (OptimisticLockingFailureException)
  ↓
Application Service  → may catch, log, rethrow or let propagate
  ↓
Controller           → returns Response<T> via @RestControllerAdvice
  ↓
GlobalExceptionHandler → maps exception → HTTP status code + Response.error(code, message)
```

---

## Common Mistakes

1. **❌ Catching and swallowing exceptions silently** — Always log at minimum
2. **❌ Throwing Spring-specific exceptions from domain layer** — Domain must stay pure
3. **❌ Returning raw exception messages to clients** — Use `GlobalExceptionHandler` to map
4. **❌ Using `@ResponseStatus` on controller methods** — Use `GlobalExceptionHandler` instead
5. **❌ Forgetting to handle SSE/async timeouts** — These must be silently swallowed, not serialized as JSON
6. **❌ Using generic `RuntimeException`** — Use specific domain exceptions with error codes
