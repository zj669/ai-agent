# Backend Development Guidelines

> Project-specific conventions for the AI Agent Platform backend (Spring Boot 3.4.9 / Java 21 / Maven).

This index is the entry point for backend specs. The repository follows a strict DDD layered architecture (`interfaces → application → domain ← infrastructure`, with `shared` underneath). Specs are organized **by layer** plus a small number of cross-cutting and topic-specific guides.

---

## Layered Specs

| Spec | Maven Module(s) | Purpose |
|------|-----------------|---------|
| [Domain Layer](./domain-layer.md) | `ai-agent-domain`, `ai-agent-shared` | Aggregate roots, value objects, domain services, repository interfaces, ports, state machines. Must remain framework-pure. |
| [Application Layer](./application-layer.md) | `ai-agent-application` | Use-case orchestration, application services, commands/DTOs, transaction boundaries, cross-domain coordination (e.g. `SchedulerService`). |
| [Infrastructure Layer](./infrastructure-layer.md) | `ai-agent-infrastructure` | Adapters for domain ports: MyBatis Plus repos, `IRedisService`, Milvus, MinIO, SSE pub/sub, Spring AI dynamic models. |
| [Interfaces Layer](./interfaces-layer.md) | `ai-agent-interfaces` | REST controllers, SSE endpoints, WebSocket (`/ws` STOMP), Spring config beans, auth filter chain. |

---

## Cross-Cutting Specs

| Spec | Scope | Highlights |
|------|-------|------------|
| [Error Handling](./error-handling.md) | All layers | Exception hierarchy, layer responsibilities, `GlobalExceptionHandler` (`@RestControllerAdvice`), unified `Response<T>` shape. |
| [Logging Guidelines](./logging-guidelines.md) | All layers | SLF4J + Lombok `@Slf4j`, log levels, MDC, sensitive-data rules, domain-layer SLF4J-only constraint. |
| [Quality Guidelines](./quality-guidelines.md) | All layers | Code reuse first (must-use wrappers like `IRedisService`), naming conventions, domain purity, common pitfalls, forbidden patterns. |

---

## Topic-Specific Specs

| Spec | Scope | Highlights |
|------|-------|-----------|
| [Database Guidelines](./database-guidelines.md) | Infrastructure depth | MySQL 8 + MyBatis Plus, schema in `docker/init/mysql/01_init_schema.sql`, **no Flyway/Liquibase**, snake_case tables, logical-delete & timestamp conventions. |
| [Directory Structure](./directory-structure.md) | Repo-wide | Maven module map, DDD layer ↔ module mapping, domain bounded-context list. |

---

## Reading Order

When starting work in this codebase:

1. [Directory Structure](./directory-structure.md) — get the lay of the land.
2. The **layer spec** that matches your work area (domain / application / infrastructure / interfaces).
3. [Quality Guidelines](./quality-guidelines.md) — mandatory reuse rules and forbidden patterns.
4. Topic-specific specs as the work demands (Database / Error Handling / Logging).
5. For end-to-end feature work that crosses layers (HTTP → service → port → adapter → external service → SSE back), also consult the project's [Cross-Layer Specs](../cross-layer/index.md) and the [Cross-Layer Thinking Guide](../guides/cross-layer-thinking-guide.md).

---

## Provenance & Maintenance

These specs were initially populated from the real codebase via the
`cc-codex-spec-bootstrap` pipeline (GitNexus + ABCoder MCP code intelligence). Each
file:line reference inside them was verified against the codebase at that point.
**When the code changes, update the spec the same PR** — stale specs mislead the next AI agent or contributor.

---

**Language**: All spec documentation is written in **English**.
