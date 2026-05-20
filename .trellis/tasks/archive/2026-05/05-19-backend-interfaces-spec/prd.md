# Backend Interfaces Layer Spec

## Goal

Produce `.trellis/spec/backend/interfaces-layer.md` documenting the **interfaces (delivery) layer** — REST controllers, SSE streaming, WebSocket, Spring configuration. Use real code from `ai-agent-interfaces/`.

## Project Context

The interfaces layer is the **outermost delivery shell**. It translates external protocols (HTTP, SSE, WebSocket) into application service calls. It may freely use Spring annotations. It MUST NOT contain business logic — only request validation, mapping, error translation, and response shaping.

**Controllers to enumerate** (verify each — CLAUDE.md authoritative listing):
| Controller | Path | Purpose |
|---|---|---|
| `AgentController` | `/api/agent` | Agent CRUD |
| `WorkflowController` | `/api/workflow/execution` | Workflow run + SSE |
| `HumanReviewController` | `/api/workflow/reviews` | Review resume |
| `ChatController` | `/api/chat` | Conversation, SSE messages |
| `KnowledgeController` | `/api/knowledge` | KB CRUD |
| `UserController` | `/client/user` | **Note: `/client` prefix, NOT `/api`** |
| `MetadataController` | `/api/meta` | Node type metadata |
| `DashboardController` | `/api/dashboard` | Aggregated metrics |
| `LlmConfigController` | `/api/llm-config` | LLM provider config |
| `McpServerController` | `/api/mcp` | MCP registry |
| `WritingController` | `/api/writing` | Writing assistant |
| `Swarm*Controller` | `/api/swarm/**` | Multi-agent workspace |

**Critical path quirk**: `UserController` uses `/client/user`, NOT `/api/user`. Spec MUST call this out.

**SSE patterns**:
- `WorkflowController` and `ChatController` both stream via SSE backed by Redis Pub/Sub
- Stream emitter publishes to Redis channel, controller subscribes and forwards to `SseEmitter`

## Files You Own

Exclusively yours:
- `.trellis/spec/backend/interfaces-layer.md` **(create new)**

Required sections:
1. **Purpose & Boundaries** — what belongs (mapping, validation) vs what does NOT (business logic)
2. **Controller Naming & Layout** — `*Controller`, package structure
3. **Path Conventions** — `/api/**` standard, `/client/user` exception
4. **REST Controller Pattern** — annotations, response shape, real example
5. **SSE / Streaming** — show `WorkflowController` or `ChatController` pattern + Redis Pub/Sub bridge
6. **Error Translation** — exception → HTTP status mapping
7. **Spring Configuration Beans** — where `@Configuration` classes live
8. **Auth / Security** — interceptor or filter chain
9. **Anti-patterns** — business logic in controllers, raw `RedisTemplate` use

## Tools Available

### GitNexus MCP
- `gitnexus_cypher({query: "MATCH (c:Class) WHERE c.name ENDS WITH 'Controller' RETURN c.name, c.file"})`
- `gitnexus_context({name: "WorkflowController"})` for full SSE flow
- `gitnexus_query({query: "SSE stream publisher"})` to map streaming pipeline

### ABCoder MCP
Java parse skipped. Use GitNexus + direct reads.

### Workflow
1. Enumerate `*Controller` classes
2. Pick canonical SSE example (WorkflowController or ChatController)
3. Cross-verify the `/client/user` prefix anomaly
4. Read configuration package for bean definitions

## Rules

- ONLY modify `.trellis/spec/backend/interfaces-layer.md`
- DO NOT modify source code or other spec files
- DO NOT run git commands

## Acceptance Criteria

- [ ] File exists, 100+ lines
- [ ] Full controller table (paths verified against actual `@RequestMapping`)
- [ ] At least 1 REST controller code example
- [ ] At least 1 SSE controller code example with Redis Pub/Sub bridge
- [ ] `/client/user` prefix anomaly explicitly documented
- [ ] No placeholder text
- [ ] Only `interfaces-layer.md` modified

## Technical Notes

- Interfaces path: `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/`
- Application entry: `ai-agent-interfaces` (Spring Boot main)
- Server port: 8080
- Actuator endpoints exposed at `/actuator/**`
