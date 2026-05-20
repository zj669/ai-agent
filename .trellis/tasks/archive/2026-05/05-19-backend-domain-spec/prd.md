# Backend Domain Layer Spec

## Goal

Produce `.trellis/spec/backend/domain-layer.md` documenting the **domain layer conventions** of this Spring Boot DDD codebase, with real code examples from `ai-agent-domain/` and `ai-agent-shared/`.

## Project Context

AI Agent Platform on Spring Boot 3.4.9 / Java 21 / Maven, strict DDD layered:

```
interfaces → application → domain ← infrastructure
                            ↑
                          shared
```

**Critical constraint**: domain layer is **framework-pure**. Dependencies allowed: ONLY `shared` module. Forbidden: Spring, MyBatis Plus, Redisson, Milvus SDK, MinIO SDK.

**Bounded contexts** under `ai-agent-domain/src/main/java/com/zj/aiagent/domain/`: `workflow`, `agent`, `chat`, `knowledge`, `user`, `auth`, `swarm`, `writing`, `mcp`, `llm`, `dashboard`, `memory`.

**Patterns to document** (verify each exists before writing):
- Aggregate roots: `Execution`, `Agent`, `Conversation`, `KnowledgeDataset`, `WorkflowGraph`
- Value objects: `Email`, `Credential`
- Domain services: `*DomainService`
- Repository interfaces: `AgentRepository` (NO `I` prefix), impls live in infrastructure
- Ports: `ChatModelPort`, `VectorStore`, `StreamPublisher`, `ExecutionRepository`
- Status enums / state machines: `PENDING → RUNNING → SUCCEEDED/FAILED/SKIPPED/PAUSED_FOR_REVIEW`

## Files You Own (DO NOT touch others)

Exclusively yours:
- `.trellis/spec/backend/domain-layer.md` **(create new)**

Required sections (omit any not validated by code):
1. **Purpose & Dependency Rules** — what imports are allowed/forbidden
2. **Aggregate Roots** — full code example for one (e.g., `Execution`)
3. **Value Objects** — immutability pattern, real example
4. **Domain Services** — when on aggregate vs separate service
5. **Repository Interfaces** — naming, methods, where impls live
6. **Ports & Adapters** — enumerate real ports, explain DIP
7. **State Machines** — Execution/Node status as canonical example
8. **Anti-patterns** — what NOT to do

## Tools Available

### GitNexus MCP (architecture)
| Tool | Use |
|------|-----|
| `gitnexus_query({query: "..."})` | Find execution flows by concept |
| `gitnexus_context({name: "Execution"})` | 360° symbol view |
| `gitnexus_impact({target: "X", direction: "downstream"})` | Blast radius |
| `gitnexus_cypher({query: "MATCH (c:Class) WHERE c.file CONTAINS 'domain' RETURN c.name LIMIT 50"})` | Direct query |

### ABCoder MCP
Java AST parse was skipped (LSP overhead). Use GitNexus + direct file reads for Java.

### Workflow
1. `gitnexus_cypher` to enumerate domain classes
2. `gitnexus_context` on aggregates
3. Read source for full code blocks
4. Write spec with real file paths

## Rules

- ONLY modify `.trellis/spec/backend/domain-layer.md`
- DO NOT modify source code
- DO NOT modify other spec files
- DO NOT run git commands
- READ any file for analysis

## Acceptance Criteria

- [ ] File exists, 100+ lines substantive content
- [ ] 3+ real code examples with file paths from `ai-agent-domain/`
- [ ] Each required section validated by codebase (omit if absent)
- [ ] At least 1 anti-pattern with reasoning
- [ ] No placeholder text
- [ ] Only `domain-layer.md` modified

## Technical Notes

- Java 21, Spring Boot 3.4.9 (but absent in this layer)
- GitNexus indexed at commit d29e610 (10K+ nodes, 22K+ edges)
- Domain path: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/`
- Shared path: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/`
