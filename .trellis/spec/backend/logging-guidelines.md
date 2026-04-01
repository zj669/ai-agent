# Logging Guidelines

> How logging is done in the AI Agent Platform backend.

---

## Overview

- **Library**: SLF4J API + Logback (via Spring Boot)
- **Structured Logging**: Logstash Logback Encoder (`logstash-logback-encoder:7.3`) for JSON output
- **Annotation**: Lombok `@Slf4j` on all classes that need logging
- **Output**: Console (dev), JSON (production) via Logback configuration

---

## Log Framework Setup

Every class that needs logging uses Lombok's `@Slf4j`:

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentApplicationService {
    public Long createAgent(CreateAgentCmd cmd) {
        log.info("[AgentService] Creating agent: name={}, userId={}", cmd.getName(), cmd.getUserId());
        // ...
    }
}
```

**⚠️ NEVER** use `System.out.println()` or `java.util.logging` — always use `@Slf4j` + `log.xxx()`.

---

## Log Levels

| Level | When to Use | Example |
|-------|-------------|---------|
| `ERROR` | System failures, unrecoverable errors, data corruption | `log.error("Unexpected error", e)` |
| `WARN` | Recoverable issues, degraded behavior, auth failures | `log.warn("Authentication error: {}", e.getMessage())` |
| `INFO` | Business milestones, service lifecycle, key operations | `log.info("[AgentService] Loaded initial graph template")` |
| `DEBUG` | Detailed flow tracing, async timeouts, intermediate states | `log.debug("Async request timed out: {}", e.getMessage())` |
| `TRACE` | Low-level details (rarely used) | Serialized payloads, raw SQL |

---

## Log Message Format Convention

### Module Tag Pattern

Prefix log messages with a bracketed module/class tag for easy grep:

```java
log.info("[AgentService] Creating agent: name={}", cmd.getName());
log.info("[SchedulerService] Execution started: executionId={}", executionId);
log.error("[AgentService] Failed to load initial graph template", e);
log.info("[SwarmAPI] Create workspace request received: userId={}, name={}", userId, name);
```

### Parameter Placeholders

- **Always use SLF4J `{}` placeholders** — NEVER string concatenation
- **Pass exception as the last argument** (SLF4J auto-extracts stack trace)

```java
// ✅ Correct
log.info("[Module] Operation completed: id={}, status={}", id, status);
log.error("[Module] Operation failed: id={}", id, exception);

// ❌ Wrong — string concatenation
log.info("[Module] Operation completed: id=" + id);

// ❌ Wrong — exception not last arg
log.error("[Module] Failed: " + exception.getMessage());
```

---

## What to Log

### MUST Log

| Event | Level | Example |
|-------|-------|---------|
| Service initialization / template loading | `INFO` | `[AgentService] Loaded initial graph template` |
| Key business operations | `INFO` | `[SchedulerService] Execution started: executionId=xxx` |
| External API calls (request/response summary) | `INFO` / `DEBUG` | `[SwarmAPI] Create workspace: userId={}, llmConfigId={}` |
| Authentication failures | `WARN` | `Authentication error: Invalid credentials` |
| Unexpected exceptions (catch-all) | `ERROR` | With full stack trace |
| Workflow node execution results | `INFO` | Node ID, status, duration |
| SSE/streaming lifecycle events | `DEBUG` | Connection opened, closed, timeout |

### SHOULD Log

| Event | Level |
|-------|-------|
| Repository save/delete operations | `DEBUG` |
| Cache hit/miss ratios | `DEBUG` |
| Configuration values on startup | `INFO` |

---

## What NOT to Log

| Data Type | Reason |
|-----------|--------|
| ❌ User passwords (even hashed) | Security |
| ❌ JWT tokens | Security |
| ❌ Complete request/response bodies with PII | Privacy (GDPR) |
| ❌ API keys / Secret keys | Credential exposure |
| ❌ Full `graphJson` in INFO level | Too verbose — use DEBUG |
| ❌ Email verification codes | Security |

---

## Structured Logging (Production)

Production uses `logstash-logback-encoder` for JSON formatting:

```xml
<!-- logback-spring.xml -->
<appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
</appender>
```

This outputs structured JSON logs compatible with ELK/Grafana stacks.

---

## Controller Layer Logging Pattern

Controllers log the **API entry point** with request parameters:

```java
@PostMapping
public Response<WorkspaceDefaultsDTO> createWorkspace(@RequestBody CreateWorkspaceRequest request) {
    Long userId = UserContext.getUserId();
    log.info("[SwarmAPI] Create workspace request received: userId={}, name={}, llmConfigId={}",
        userId, request.getName(), request.getLlmConfigId());
    return Response.success(workspaceService.createWorkspace(userId, request));
}
```

---

## Exception Logging in GlobalExceptionHandler

```java
// WARN for expected business errors
log.warn("Authentication error: {}", e.getMessage());

// DEBUG for async/SSE timeouts (noise reduction)
log.debug("Async request timed out: {}", e.getMessage());

// ERROR for unexpected/catch-all with full stack trace
log.error("Unexpected error", e);
```

---

## Common Mistakes

1. **❌ Using `System.out.println`** — Use `@Slf4j` + `log.xxx()`
2. **❌ Logging sensitive data** — No passwords, tokens, API keys
3. **❌ String concatenation in log messages** — Use `{}` placeholders
4. **❌ Missing module tag** — Always use `[ModuleName]` prefix
5. **❌ Logging full exception message without stack** — Pass exception object as last arg
6. **❌ Too verbose INFO logging** — Use DEBUG for detailed traces, keep INFO for business milestones
