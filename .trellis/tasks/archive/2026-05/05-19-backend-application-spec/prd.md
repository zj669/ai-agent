# Backend Application Layer Spec

## Goal

Produce `.trellis/spec/backend/application-layer.md` documenting **application layer conventions** with real code from `ai-agent-application/`.

## Project Context

DDD layered Spring Boot 3.4.9 / Java 21. Application layer **orchestrates use cases** — it composes domain services, coordinates cross-aggregate flows, publishes events, manages transactions. It depends on `domain` and `shared`; it MAY use Spring, but NEVER reaches into infrastructure adapters directly (always through domain ports).

**Key orchestrators** (verify in code): `SchedulerService` (workflow execution), `ChatApplicationService` (conversation lifecycle + SSE completion), `AgentApplicationService`, `KnowledgeApplicationService`.

**Workflow execution canonical flow**:
1. `WorkflowController` → `SchedulerService.startExecution` → `Execution.start()`
2. `SchedulerService.hydrateMemory` → VectorStore (LTM) + ConversationRepository (STM)
3. `NodeExecutorStrategy` → `StreamPublisher` → SSE to frontend
4. `Execution.advance(nodeId, result)` → next ready nodes
5. `SchedulerService.onExecutionComplete` → `ChatApplicationService.completeAssistantMessage`

**Patterns to document**:
- Application services (`*ApplicationService`)
- Commands (`CreateAgentCmd`, `UpdateAgentCmd`), DTOs (`AgentDTO`, `AgentListDTO`)
- Transaction boundaries (`@Transactional` usage)
- Event publishing / handling
- Cross-domain coordination (where it lives, why not in domain)
- Hand-off to infrastructure: ALWAYS via domain-defined ports

## Files You Own

Exclusively yours:
- `.trellis/spec/backend/application-layer.md` **(create new)**

Required sections:
1. **Purpose & Boundaries** — what belongs here vs in domain vs in infrastructure
2. **Application Services** — naming, structure, real example
3. **Commands & DTOs** — input/output contracts, validation point
4. **Transaction Management** — where `@Transactional` goes
5. **Orchestration Patterns** — multi-aggregate flows (SchedulerService canonical case)
6. **Event Handling** — pub/sub patterns if present
7. **Dependency Direction** — application → domain ports ONLY
8. **Anti-patterns** — common mistakes (bypassing domain, leaking infra concerns)

## Tools Available

### GitNexus MCP
- `gitnexus_query({query: "scheduler workflow execution"})` to map orchestration
- `gitnexus_context({name: "SchedulerService"})` for full surface
- `gitnexus_cypher({query: "MATCH (c:Class) WHERE c.name ENDS WITH 'ApplicationService' RETURN c.name, c.file"})`
- `gitnexus_impact({target: "ChatApplicationService", direction: "upstream"})` to see who calls it

### ABCoder MCP
Java parse skipped. Use GitNexus + direct reads.

### Workflow
1. Enumerate `*ApplicationService`, `*Cmd` classes
2. Pick 1-2 canonical examples (SchedulerService, ChatApplicationService)
3. Read full source
4. Write spec with file paths + code blocks

## Rules

- ONLY modify `.trellis/spec/backend/application-layer.md`
- DO NOT modify source code or other spec files
- DO NOT run git commands

## Acceptance Criteria

- [ ] File exists, 100+ lines
- [ ] 2+ real Application Service examples with file paths
- [ ] Transaction boundary section with real `@Transactional` example
- [ ] Workflow orchestration documented using SchedulerService
- [ ] No placeholder text
- [ ] Only `application-layer.md` modified

## Technical Notes

- Application package: `ai-agent-application/src/main/java/com/zj/aiagent/application/`
- May use Spring stereotypes (`@Service`, `@Transactional`)
- Critical: domain layer is pure; infrastructure access is through ports defined in domain
