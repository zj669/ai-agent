# Directory Structure

> How the backend code is organized in this Spring Boot DDD repository.

This file is repo-wide: it shows the **Maven module map** (which corresponds to DDD layers), the **bounded contexts** inside `ai-agent-domain`, and how to navigate to the layer-specific spec for any code area.

---

## Repository Root

```
ai-agent/
├── ai-agent-shared/          # Layer 0: Pure utilities (no Spring/MyBatis)
├── ai-agent-domain/          # Layer 1: Business core (depends only on shared)
├── ai-agent-application/     # Layer 2: Use-case orchestration
├── ai-agent-infrastructure/  # Layer 3: Adapters for ports (DB, Redis, Milvus, ...)
├── ai-agent-interfaces/      # Layer 4: REST / SSE / WebSocket + Spring Boot entry
├── ai-agent-foward/          # React 19 frontend (see ../frontend/)
├── docker/                   # docker-compose + MySQL init schema
├── docs/                     # Long-form architecture notes (.blueprint mirror)
├── .blueprint/               # Module-by-module blueprint docs
└── .trellis/                 # This workflow + specs you're reading
```

`ai-agent-foward/` is the **active** frontend. `app/frontend/` (if present) is a legacy skeleton — ignore it.

---

## DDD Layer ↔ Maven Module

Dependency direction (strict):

```
interfaces → application → domain ← infrastructure
                            ↑
                          shared
```

| Module | Layer | Allowed Frameworks | Spec |
|--------|-------|---------------------|------|
| `ai-agent-shared` | Layer 0 (utilities) | Lombok, Hutool, Guava, Commons Lang3, Fastjson, SLF4J API | (covered by Domain spec) |
| `ai-agent-domain` | Layer 1 (business core) | Same as shared + ports as Java interfaces. **No Spring, MyBatis, Redisson, Milvus SDK, etc.** | [domain-layer.md](./domain-layer.md) |
| `ai-agent-application` | Layer 2 (orchestration) | Spring stereotypes (`@Service`, `@Transactional`), event publishing | [application-layer.md](./application-layer.md) |
| `ai-agent-infrastructure` | Layer 3 (adapters) | MyBatis Plus, Redisson, Milvus SDK, MinIO SDK, Spring AI, any 3rd-party SDK | [infrastructure-layer.md](./infrastructure-layer.md), [database-guidelines.md](./database-guidelines.md) |
| `ai-agent-interfaces` | Layer 4 (delivery) | `@RestController`, `SseEmitter`, Spring Security, STOMP/SockJS, `@Configuration` beans | [interfaces-layer.md](./interfaces-layer.md) |

> Reality note: domain currently has some Spring/Jackson/Security imports leaking in (technical debt; see Anti-Patterns in `domain-layer.md`). The dependency rule is the **direction we move toward** when modifying domain code, not a claim about today's state.

---

## Bounded Contexts inside `ai-agent-domain`

Located under `ai-agent-domain/src/main/java/com/zj/aiagent/domain/`:

| Context | Purpose | Cross-layer notes |
|---------|---------|--------------------|
| `workflow` | Workflow execution engine. `Execution` aggregate root, `WorkflowGraph`, `ExecutionContext`, 7 `NodeExecutorStrategy` impls (Start/End/LLM/Condition/Http/Tool/Knowledge). | May depend on `agent`, `knowledge`, `memory`. |
| `agent` | Agent management — model configuration, MCP tool binding. | No cross-domain deps. |
| `chat` | Conversation management — `Conversation`, `Message`, SSE-backed streaming. | Cooperates with `workflow` for streaming. |
| `knowledge` | Knowledge base — `KnowledgeDataset`, `KnowledgeDocument`, vector storage via Milvus. | Read by workflow's `KnowledgeNodeExecutorStrategy`. |
| `user` | User account / profile. REST prefix is `/client/user` (**not** `/api/user` — see `interfaces-layer.md`). | — |
| `auth` | Auth + token handling. | — |
| `swarm` | Multi-agent workspace: groups, graph, SSE (`/api/swarm/**`). | — |
| `writing` | Writing-assistant flows. | — |
| `mcp` | MCP server registry + tool metadata. | Used by `agent` for tool binding. |
| `llm` | LLM provider configuration. | Used by `workflow` LLM nodes. |
| `dashboard` | Aggregated metrics. | Read-only across domains. |
| `memory` | LTM (vector store) + STM (recent conversation) ports. | Hydrated into `ExecutionContext` by `SchedulerService`. |

---

## Where Things Live (Quick Lookup)

| What you're looking for | Where |
|--------------------------|-------|
| Aggregate roots, value objects | `ai-agent-domain/src/main/java/com/zj/aiagent/domain/<ctx>/` |
| Domain ports (interfaces) | Same as above, files like `*Repository.java`, `*Port.java`, `VectorStore.java`, `StreamPublisher.java` |
| Application services / orchestration | `ai-agent-application/src/main/java/com/zj/aiagent/application/<ctx>/` |
| Adapters / repository impls | `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/<area>/` |
| PO classes + Mappers (MyBatis Plus) | `ai-agent-infrastructure/.../persistence/` |
| REST controllers | `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/<ctx>/` |
| SSE / WebSocket endpoints | `ai-agent-interfaces/.../` (e.g. `WorkflowController`, `ChatController`, `/ws` STOMP) |
| Spring `@Configuration` beans | `ai-agent-interfaces/.../config/` |
| DB schema (single source of truth) | `docker/init/mysql/01_init_schema.sql` |
| Application entry point | `ai-agent-interfaces` main class |

---

## Module Boundaries — Rules of Thumb

When adding a new feature:

1. **Start in domain.** Model the entity / VO / port first. No framework imports.
2. **Wire orchestration in application.** Add or extend a `*ApplicationService`. Transactions live here.
3. **Provide adapters in infrastructure.** Implement domain ports. Use existing wrappers (`IRedisService`, `HttpService`) over raw `RedisTemplate` / `RestTemplate`.
4. **Expose in interfaces.** Add `@RestController` or extend an existing one. Translate exceptions via `GlobalExceptionHandler`. Use `SseEmitter` for streaming.
5. **Update the spec.** When the convention you established differs from any spec file, update that file in the same PR.

---

## When NOT to Create New Tables / Files

- **Search before creating tables.** `WorkflowNodeExecution` already exists — don't create `WorkflowExecution` as a parallel concept (see `quality-guidelines.md`).
- **Search before adding utility services.** `IRedisService`, HTTP wrappers, and structured-logging helpers already exist; reusing them avoids ramp drift.
- **Search before defining new exceptions.** A small set of typed exceptions is wired into `GlobalExceptionHandler` already; extend rather than reinvent.

See [Code Reuse Thinking Guide](../guides/code-reuse-thinking-guide.md) for the discipline behind this.

---

## See Also

- [Backend Index](./index.md) — full spec list and reading order.
- [Cross-Layer Thinking Guide](../guides/cross-layer-thinking-guide.md) — when your work touches 3+ layers.
- [Cross-Layer Specs](../cross-layer/index.md) — end-to-end data flow contracts (when written).
