# Logging Guidelines

> Logging conventions for backend code.

---

## Scope

The backend uses SLF4J as the logging facade. Spring Boot provides Logback at
runtime from the application side, but domain code must not depend on Logback or
any Logback-specific API.

Most classes use Lombok `@Slf4j`. Keep that convention unless a class cannot use
Lombok for a concrete reason.

---

## Facade & Idiom

Use:

- `lombok.extern.slf4j.Slf4j`
- parameterized SLF4J messages with `{}` placeholders
- structured identifiers in message fields, such as `executionId`, `nodeId`,
  `documentId`, `datasetId`, `agentId`, and `userId`

Do not use:

- `System.out.println`
- `Throwable#printStackTrace()`
- `java.util.logging`
- Logback classes in domain/application code
- string concatenation for normal log fields

### Real Example: Standard `@Slf4j` Service Logging

File:
`ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java`

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {

    public void startExecution(...) {
        log.info(
            "[Scheduler] Starting execution for agent: {}, version: {}, mode: {}",
            agentId,
            versionId,
            mode
        );
    }
}
```

This is the default style: prefix the subsystem, use placeholders, and pass
fields as arguments.

### Real Example: Progress Logs With Stable IDs

File:
`ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/AsyncDocumentProcessor.java`

```java
log.info(
    "文档处理进度: documentId={}, progress={}/{}",
    document.getDocumentId(),
    processedCount,
    chunks.size()
);
```

Long-running background jobs must include stable identifiers and progress
numbers so failures can be correlated with persisted state.

---

## Log Levels

| Level | Use for | Examples in this project |
| --- | --- | --- |
| `DEBUG` | Developer diagnostics, verbose request/response details, cache or vector hit counts. | `RedisVerificationCodeRepository` logs code existence and removal at debug level. |
| `INFO` | Lifecycle events and successful state transitions. | Workflow start, document processing milestones, RestClient request/response timing. |
| `WARN` | Expected but abnormal recoverable behavior. | Validation/auth failures, disabled Milvus fallback, memory hydration failure that does not stop workflow. |
| `ERROR` | Failed operation requiring attention, especially when the current layer handles or wraps the failure. | Unexpected global exceptions, failed HTTP node execution, failed SSE publish serialization. |

Guidance:

- Use `INFO` sparingly for events operators would search for.
- Use `WARN` when the request can continue but quality or security is affected.
- Use `ERROR` with the exception object when stack trace matters.
- Do not log and rethrow at every layer. Log at the layer that has unique
  context, then let upstream handlers own their own boundary logs.

---

## Structured Logging & MDC

The current codebase mostly uses structured message fields rather than a
centralized MDC interceptor. New request/execution boundary code should move
toward MDC while preserving the existing explicit identifiers in critical
messages.

Correlation keys:

| Key | Source |
| --- | --- |
| `traceId` | HTTP ingress header or generated request ID. |
| `executionId` | Workflow execution lifecycle, SSE stream, Redis channel, node logs. |
| `nodeId` | Workflow node executor and review operations. |
| `userId` | Authenticated user from `UserContext`. |
| `agentId` | Agent-scoped workflow or memory operations. |
| `conversationId` | Chat-linked workflow execution. |

When adding MDC:

```java
try {
    MDC.put("traceId", traceId);
    MDC.put("userId", String.valueOf(userId));
    // invoke downstream work
} finally {
    MDC.clear();
}
```

MDC must be set at the boundary and cleared in `finally`. For async execution,
copy or rebuild context explicitly; do not assume worker threads inherit MDC.

### Real Example: Execution ID in Logs and Payload

File:
`ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java`

```java
log.info(
    "[Scheduler] Starting execution: {}, mode: {}",
    execution.getExecutionId(),
    mode
);

payload.put("executionId", executionId);
publisher.publishEvent("workflow_resumed", payload);
```

Workflow code must make `executionId` visible in logs and emitted events.

### Real Example: SSE Channel Logging

File:
`ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/event/RedisSsePublisher.java`

```java
String channel = CHANNEL_PREFIX + payload.getExecutionId();
String message = objectMapper.writeValueAsString(payload);
redisService.publish(channel, message);

log.debug("[SSE-Pub] Published to {}: {}", channel, message);
```

This is useful for local debugging, but full SSE payloads can be large or
sensitive. Keep such logs at `DEBUG`; do not promote payload bodies to `INFO`.

---

## Sensitive Data

Never log:

- credentials or password hashes
- JWT access or refresh tokens
- API keys, provider secrets, or Authorization headers
- email verification codes
- full LLM prompts, completions, or SSE payloads at `INFO`
- full uploaded document content or vector chunks
- raw request/response bodies from third-party HTTP calls

Mask or omit sensitive values. If the value is needed for debugging, log a
stable id, length, hash, status, or last four characters only.

### Real Example: Filtering Authorization Headers

File:
`ai-agent-interfaces/src/main/java/com/zj/aiagent/config/RestClientConfig.java`

```java
Arrays.stream(request.getHeaders())
    .filter(header ->
        !header
            .getName()
            .toLowerCase()
            .contains("authorization")
    )
    .forEach(header ->
        log.debug("Request Header: {}: {}", header.getName(), header.getValue())
    );
```

This is the correct direction: sensitive headers are excluded before debug
logging.

### Existing Deviation To Avoid

File:
`ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/auth/token/JwtTokenService.java`

`invalidateToken` logs the full token when blacklisting it. Do not copy this
pattern. New token logs should identify only the token id (`jti`), user id,
expiration, or a masked token suffix.

---

## Domain Layer Constraint

Domain code may use the SLF4J API through Lombok `@Slf4j`, but it must not
declare or import Logback runtime APIs.

Current evidence:

- `ai-agent-domain/pom.xml` declares `lombok` but does not declare Logback.
- Domain classes such as
  `ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/service/UserAuthenticationDomainService.java`
  use `@Slf4j`.
- Runtime logging configuration belongs to `ai-agent-interfaces`, where
  `application.yml` points to `classpath:logback-spring.xml`.

Domain entities and value objects should be quiet by default. If a domain service
logs, it should log business events or rejected invariants with identifiers, not
transport details.

---

## Anti-Patterns

| Anti-pattern | Why it is wrong | Replacement |
| --- | --- | --- |
| `System.out.println(...)` | Bypasses log levels, appenders, formatting, and production routing. | Use `log.debug/info/warn/error`. |
| `printStackTrace()` | Bypasses structured logging and loses correlation fields. | Use `log.error("message", e)`. |
| String concatenation in logs | Eagerly builds strings and obscures fields. | Use SLF4J placeholders. |
| Logging credentials/tokens/API keys | Creates credential leakage in persistent logs. | Mask or omit; log ids and status only. |
| Logging full SSE or LLM payloads at `INFO` | Payloads can be huge and sensitive. | Log length, ids, or short previews at `DEBUG`. |
| Logging then rethrowing at every layer | Produces duplicate stack traces and noisy alerts. | Log where context is added; otherwise rethrow. |
| Importing Logback classes in domain | Couples pure/domain code to runtime logging implementation. | Use SLF4J facade only. |
| Logging recoverable user mistakes as `ERROR` | Inflates operational alerts. | Use `WARN` for expected bad input/auth/business failures. |

---

## Checklist

- [ ] Does the class use `@Slf4j` or an existing project logging wrapper?
- [ ] Are log fields passed as `{}` arguments?
- [ ] Does every workflow log include `executionId` when available?
- [ ] Does every node executor log include `nodeId` when available?
- [ ] Does async code carry or rebuild MDC/correlation context explicitly?
- [ ] Are secrets, tokens, headers, verification codes, prompts, and document
      bodies omitted or masked?
- [ ] Is the level correct for the operator impact?
- [ ] Is the exception logged only where useful context is added?
- [ ] Did you avoid `System.out.println` and `printStackTrace()`?
