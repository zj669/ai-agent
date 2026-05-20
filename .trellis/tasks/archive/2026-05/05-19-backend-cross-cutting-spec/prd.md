# Backend Cross-Cutting Spec

## Goal

Rewrite three template files to capture the **cross-cutting concerns** of the backend, using real codebase patterns:
1. `.trellis/spec/backend/error-handling.md` (currently 51-line template)
2. `.trellis/spec/backend/logging-guidelines.md` (currently 51-line template)
3. `.trellis/spec/backend/quality-guidelines.md` (currently 51-line template)

## Project Context

DDD layered Spring Boot 3.4.9 / Java 21. Cross-cutting concerns span ALL layers but are best documented separately so the layer specs stay focused.

### Error Handling
- Exceptions originate in domain (business invariants) or infrastructure (technical failures)
- Application services may wrap and re-throw
- Interfaces layer translates to HTTP status + JSON body
- Look for: `@RestControllerAdvice`, custom exception base class, error code enum, `Result<T>` or unified response wrapper

### Logging
- SLF4J facade everywhere
- Lombok `@Slf4j` annotation is the canonical idiom
- Structured logging with MDC for trace ID / execution ID / user ID
- Critical: domain layer can use SLF4J API ONLY (no logback dependency in domain POM)
- Log levels: DEBUG (dev), INFO (lifecycle events), WARN (recoverable), ERROR (failure with stack)

### Quality
- Cited in CLAUDE.md: **Code Reuse Requirements** — search for existing services first
  - ❌ NEVER use `RedisTemplate`/`RestTemplate` directly
  - ✅ Use `RedisService`, `HttpService`
- ❌ NEVER create duplicate tables (e.g., don't create `WorkflowExecution` if `WorkflowNodeExecution` exists)
- Naming: `AgentRepository` (NOT `IAgentRepository`), `AgentRepositoryImpl`
- Domain entities are POJOs with business logic only
- No framework imports in domain layer
- WorkflowGraph is immutable after creation
- Conditional branching MUST handle SKIPPED nodes
- Don't use auto-wired Spring AI beans — models are user-configured

## Files You Own

Exclusively yours (all 3 are rewrites of existing template files):
- `.trellis/spec/backend/error-handling.md`
- `.trellis/spec/backend/logging-guidelines.md`
- `.trellis/spec/backend/quality-guidelines.md`

### error-handling.md sections
1. **Exception Hierarchy** — base classes, custom domain exceptions, real examples
2. **Layer Responsibilities** — where exceptions are thrown vs translated
3. **Global Exception Handler** — `@RestControllerAdvice` pattern with code example
4. **Error Codes & Response Shape** — uniform JSON structure
5. **Anti-patterns** — swallowing exceptions, returning null vs throwing, generic `RuntimeException`

### logging-guidelines.md sections
1. **Facade & Idiom** — SLF4J + `@Slf4j` Lombok
2. **Log Levels** — DEBUG/INFO/WARN/ERROR — when to use each
3. **Structured Logging & MDC** — trace ID, execution ID, user ID
4. **Sensitive Data** — never log credentials, tokens, full SSE payloads
5. **Domain Layer Constraint** — only SLF4J API allowed
6. **Anti-patterns** — `System.out.println`, `printStackTrace()`, logging then re-throwing

### quality-guidelines.md sections
1. **Code Reuse First** — mandatory search-before-write protocol (cite CLAUDE.md)
2. **Encapsulated Services** — `RedisService`, `HttpService` over raw templates
3. **Naming Conventions** — comprehensive table (Entities, VOs, Repositories, Services, DTOs, POs, Mappers, Controllers)
4. **Domain Purity** — no framework imports
5. **Database Hygiene** — search before creating tables, snake_case, logical delete
6. **State Machine Discipline** — handle all statuses including SKIPPED
7. **Common Pitfalls Checklist** — extracted from CLAUDE.md "Common Pitfalls" section
8. **Forbidden Patterns** — list with WHY for each

## Tools Available

### GitNexus MCP
- `gitnexus_cypher({query: "MATCH (c:Class) WHERE c.name CONTAINS 'Exception' RETURN c.name, c.file LIMIT 30"})`
- `gitnexus_cypher({query: "MATCH (c:Class) WHERE c.name = 'GlobalExceptionHandler' RETURN c.file"})` (or similar)
- `gitnexus_context({name: "RedisService"})` to confirm it exists and find usage
- `gitnexus_query({query: "error response wrapper"})`

### ABCoder MCP
Java parse skipped. Use GitNexus + direct reads.

### Workflow
1. Find exception base classes and global handler
2. Confirm `RedisService` / `HttpService` wrappers exist and inspect surface
3. Read 2-3 services to extract logging conventions
4. Pull "Common Pitfalls" from `/home/zj669/repo/ai-agent/CLAUDE.md` and adapt

## Rules

- ONLY modify the 3 listed files
- DO NOT modify source code
- DO NOT modify other spec files (including `domain-layer.md`, `application-layer.md`, etc.)
- DO NOT run git commands

## Acceptance Criteria

- [ ] All 3 files rewritten, each 80+ lines
- [ ] Each file has at least 2 real code examples with file paths
- [ ] `error-handling.md` includes the global exception handler pattern
- [ ] `logging-guidelines.md` covers domain layer SLF4J-only constraint
- [ ] `quality-guidelines.md` includes naming conventions table + forbidden patterns
- [ ] No placeholder text in any file
- [ ] Only the 3 listed files modified

## Technical Notes

- SLF4J + Logback (default Spring Boot)
- Lombok `@Slf4j` is project standard
- Reference: `/home/zj669/repo/ai-agent/CLAUDE.md` "Code Reuse Requirements", "Common Pitfalls", "Naming Conventions" sections
