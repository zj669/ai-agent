# Quality Guidelines

> Backend quality rules for the DDD / Ports and Adapters codebase.

---

## Scope

These rules apply to backend modules:

- `ai-agent-shared`
- `ai-agent-domain`
- `ai-agent-application`
- `ai-agent-infrastructure`
- `ai-agent-interfaces`

They are based on current code patterns and the project-level requirements in
`CLAUDE.md`.

---

## Code Reuse First

Before writing backend code, search for existing components and state the reuse
decision. This is mandatory because the project already has wrappers, domain
ports, repositories, workflow status logic, schema tables, and node executor
strategies.

Required architecture alignment output before coding:

```markdown
**Architecture Alignment Check**
- **Found Components**: ...
- **Found Related Entities**: ...
- **Reuse Strategy**: [Complete Reuse / Extend Existing / Create Related]
```

Search order:

1. Search service wrappers and ports before using a framework client.
2. Search domain services before implementing business logic.
3. Search repositories and PO classes before adding persistence code.
4. Search `docker/init/mysql/01_init_schema.sql` before adding or changing a
   table.
5. Search node executors and workflow state transitions before adding workflow
   behavior.

Use existing implementations when possible. Extend them when the behavior is a
near match. Create new code only when there is no correct owner.

---

## Encapsulated Services

Do not scatter raw technical clients through application logic. Prefer existing
project wrappers and ports.

### Redis

The current Redis wrapper is named `IRedisService`, implemented by
`RedissonService`.

File:
`ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/redis/IRedisService.java`

```java
public interface IRedisService {
    <T> void setValue(String key, T value);
    <T> void setValue(String key, T value, long expired);
    <T> T getValue(String key);
    <T> RQueue<T> getQueue(String key);
    RLock getLock(String key);
}
```

File:
`ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/user/repository/RedisVerificationCodeRepository.java`

```java
@Repository
@RequiredArgsConstructor
public class RedisVerificationCodeRepository implements IVerificationCodeRepository {

    private final IRedisService redisService;

    public void save(Email email, String code, long expirySeconds) {
        String key = RedisKeyConstants.Email.VERIFICATION_CODE_PREFIX + email.getValue();
        redisService.setString(key, code, expirySeconds, TimeUnit.SECONDS);
    }
}
```

New code should follow this shape. Direct `StringRedisTemplate` usage exists in
specialized auth infrastructure (`RedisSlidingWindowRateLimiter`,
`JwtTokenService`), but it should not be copied into application services or new
general Redis consumers.

### HTTP

No `HttpService` class currently exists. The current HTTP-node implementation
uses configured Spring clients:

- `HttpNodeExecutorStrategy` injects `WebClient.Builder`.
- `ConditionNodeExecutorStrategy` injects `RestClient.Builder`.
- `RestClientConfig` centralizes Apache HttpClient timeouts, connection pool,
  retry strategy, request timing, and header filtering.
- `WebClientConfig` provides the primary `WebClient.Builder`.

File:
`ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/HttpNodeExecutorStrategy.java`

```java
private final WebClient.Builder webClientBuilder;

String bodyResult = webClient.method(method)
    .uri(url)
    .headers(h -> headers.forEach((key, value) -> h.set(key, value.toString())))
    .exchangeToMono(clientResponse -> {
        statusCodeHolder[0] = clientResponse.statusCode().value();
        return clientResponse.bodyToMono(String.class);
    })
    .block(requestTimeout);
```

Do not introduce ad hoc `new RestTemplate()`, unconfigured `WebClient.create()`,
or duplicated timeout/retry code. If HTTP access expands beyond node executors,
create or extend one project-level HTTP adapter instead of duplicating clients.

---

## Naming Conventions

Use these names for new backend code.

| Concept | Pattern | Example | Notes |
| --- | --- | --- | --- |
| Aggregate root / entity | `Name.java` | `Agent.java`, `User.java`, `Execution.java` | Domain object with business behavior. |
| Value object | `Name.java` | `Email.java`, `Credential.java`, `ExecutionStatus.java` | Immutable where practical; owns validation/meaning. |
| Repository port | `NameRepository.java` | `AgentRepository.java`, `DashboardRepository.java` | Prefer no `I` prefix for new ports. |
| Legacy repository port | Existing `INameRepository.java` only | `IUserRepository.java`, `IVerificationCodeRepository.java`, `IMcpServerRepository.java` | Do not rename casually; do not create new `I*` ports unless matching an existing legacy module. |
| Repository implementation | `NameRepositoryImpl.java` | `AgentRepositoryImpl.java`, `HumanReviewRepositoryImpl.java` | Lives in infrastructure. |
| Domain service | `NameDomainService.java` | `SwarmDomainService.java` | Business logic that does not naturally belong to one entity. |
| Application service | `NameApplicationService.java` or established module service name | `AgentApplicationService.java`, `SchedulerService.java` | Use case orchestration. |
| Infrastructure service/adapter | `NameService`, `NameAdapter`, `NameStrategy` | `RedissonService`, `MilvusVectorStoreAdapter`, `HttpNodeExecutorStrategy` | Technical implementation of ports or runtime strategy. |
| Controller | `NameController.java` | `AgentController.java`, `WorkflowController.java` | Interfaces layer only. |
| Request DTO | `VerbNameRequest.java` or nested request | `CreateWorkspaceRequest.java`, `StartExecutionRequest` | Input from HTTP/API boundary. |
| Response/DTO | `NameDTO.java`, `NameSummaryDTO.java`, `NameViewDTO.java` | `ExecutionDTO.java`, `WritingSessionSummaryDTO.java` | Output/application data transfer. |
| Persistence object | `NamePO.java` or existing `NameDO.java` | `AgentPO.java`, `UserPO.java`, `MessageDO.java` | MyBatis/MyBatis Plus mapping only. |
| Mapper | `NameMapper.java` + optional XML | `AgentMapper.java` | Infrastructure persistence only. |
| Enum/status | `NameStatus.java`, `NameType.java` | `ExecutionStatus.java`, `NodeType.java` | Exhaustive state rules must be updated with new values. |

### Real Example: Preferred Repository Naming

Files:

- `ai-agent-domain/src/main/java/com/zj/aiagent/domain/agent/repository/AgentRepository.java`
- `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/agent/repository/AgentRepositoryImpl.java`

```java
public interface AgentRepository {
    void save(Agent agent);
    Optional<Agent> findById(Long id);
}

@Repository
@RequiredArgsConstructor
public class AgentRepositoryImpl implements AgentRepository {
    private final AgentMapper agentMapper;
}
```

This is the preferred new-code convention: domain port without `I`, infrastructure
implementation with `Impl`.

---

## Domain Purity

Domain entities and value objects should be POJOs with business logic only.

Rules for new code:

- No HTTP, controller, servlet, or Spring Web imports in domain.
- No MyBatis, Redis, Milvus, MinIO, Spring AI, or WebClient imports in domain.
- No `Response<T>` in domain or application service internals.
- Domain repository interfaces may expose domain concepts, not persistence
  objects.
- Domain entities may throw business exceptions, but must not translate to HTTP.
- Domain logging may use SLF4J via Lombok, but not Logback APIs.

Current drift to avoid expanding:

- `ai-agent-domain/pom.xml` currently includes `spring-context` and
  `spring-data-commons` for some services/ports.
- Some domain services use `@Service`.
- Some ports use Spring `Pageable`.

Do not treat those legacy dependencies as permission to add framework imports to
entities or value objects. Keep new entities pure and isolate framework details
behind application/infrastructure boundaries.

---

## Database Hygiene

The schema owner is:
`docker/init/mysql/01_init_schema.sql`

There is no Flyway migration pipeline in this project. The SQL file is applied
on first MySQL container startup. Changes after data exists require a deliberate
manual migration plan.

Rules:

- Search the schema before adding tables or columns.
- Use `snake_case` table and column names.
- Use existing timestamp conventions in the target table family:
  `create_time/update_time` or `created_at/updated_at`.
- Use logical delete consistently when the table family already does:
  `deleted`, `is_deleted`, or module-specific existing convention.
- Add indexes for frequent user, agent, execution, status, and time filters.
- Do not create a duplicate table because the current table name is not what you
  expected.
- Keep PO/Mapper/domain conversion aligned with schema changes.

### Real Example: Existing Workflow Tables

File:
`docker/init/mysql/01_init_schema.sql`

```sql
CREATE TABLE IF NOT EXISTS `workflow_execution` (
  `execution_id` varchar(36) NOT NULL COMMENT '执行ID (UUID)',
  `agent_id` bigint(20) NOT NULL COMMENT 'Agent ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `status` varchar(20) NOT NULL DEFAULT 'RUNNING',
  PRIMARY KEY (`execution_id`)
);

CREATE TABLE IF NOT EXISTS `workflow_node_execution_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `execution_id` varchar(36) NOT NULL COMMENT '工作流执行ID',
  `node_id` varchar(50) NOT NULL COMMENT '节点ID',
  PRIMARY KEY (`id`)
);
```

Before adding workflow persistence, extend these tables or their repositories
unless a new table has a clear separate lifecycle.

### Real Example: Refactor Notes Prevent Duplicate Tables

File:
`docker/init/mysql/01_init_schema.sql`

```sql
-- 【重构 04-02】writing_agent 表已删除（2026-04-02）
-- 数据已迁移至 swarm_workspace_agent（session_id + sort_order）
-- 如需历史数据，执行迁移后删除此表：
--   DROP TABLE writing_agent;
```

Respect these notes. Do not recreate `writing_agent`; use
`swarm_workspace_agent.session_id` and `sort_order`.

---

## State Machine Discipline

Workflow status handling is a central correctness boundary.

Current states:
`PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED`, `SKIPPED`, `PAUSED`,
`CANCELLED`, `PAUSED_FOR_REVIEW`.

Rules:

- When adding a state, update every switch, predicate, persistence mapping, DTO,
  frontend consumer, and test that interprets status.
- Conditional branching must handle `SKIPPED`.
- Convergence nodes must not be skipped while any predecessor may still run.
- Do not mutate `WorkflowGraph` after creation; build it through
  `WorkflowGraphFactoryImpl` from `graphJson`.
- Do not bypass `Execution.advance`, `Execution.resume`, and `Execution.reject`
  when changing workflow lifecycle state.

### Real Example: SKIPPED-aware Pruning

File:
`ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java`

```java
if (currentStatus != ExecutionStatus.PENDING) {
    return;
}

nodeStatuses.put(nodeId, ExecutionStatus.SKIPPED);

boolean allPredecessorsSkipped = predecessors
    .stream()
    .allMatch(pred ->
        nodeStatuses.get(pred.getNodeId()) == ExecutionStatus.SKIPPED
    );
if (allPredecessorsSkipped) {
    skipNodeRecursively(successor.getNodeId());
}
```

This prevents a convergence node from being skipped just because one branch was
pruned.

---

## Spring AI Discipline

Spring AI auto-configuration is disabled in
`ai-agent-interfaces/src/main/resources/application.yml`.

Rules:

- Do not rely on auto-injected `ChatClient`, `ChatModel`, or embedding model
  beans.
- Use user-configured model settings through the existing model ports/adapters.
- Keep provider API keys out of logs and response DTOs.
- When using Spring AI classes in infrastructure, keep them behind domain ports.

---

## Common Pitfalls Checklist

Extracted and adapted from `CLAUDE.md`.

- [ ] Do not bypass domain layer. Application code should use domain ports and
      repositories instead of reaching around them.
- [ ] Do not create framework dependencies in domain entities.
- [ ] Do not duplicate functionality. Search existing services before coding.
- [ ] Do not use auto-wired Spring AI beans; models are user-configured.
- [ ] Do not modify `WorkflowGraph` after creation.
- [ ] Do not forget `SKIPPED` nodes in conditional branching.
- [ ] Do not use `app/frontend/`; active frontend code is `ai-agent-foward/`.
- [ ] Do not assume Flyway exists; schema is in
      `docker/init/mysql/01_init_schema.sql`.
- [ ] Do not create duplicate tables or bridge layers that the schema comments
      explicitly removed.
- [ ] Do not log or return API keys, tokens, credentials, or provider secrets.

---

## Forbidden Patterns

| Forbidden pattern | Why |
| --- | --- |
| Direct `RedisTemplate` / `StringRedisTemplate` in new application code | Bypasses `IRedisService`, duplicates key/expiry conventions, and spreads Redis API knowledge. |
| New `RestTemplate` / raw `WebClient.create()` in business logic | Bypasses configured timeouts, retry, request IDs, and header filtering. |
| New `I*Repository` ports | Current preferred convention is `AgentRepository`; new `I*` names extend legacy inconsistency. |
| Returning `Response<T>` from domain/application internals | Couples business logic to HTTP response shape. |
| Domain entities importing Spring Web, MyBatis, Redis, Milvus, MinIO, or Spring AI | Breaks domain purity and makes business logic framework-dependent. |
| Creating a table without searching `01_init_schema.sql` and PO classes | High risk of duplicate tables and divergent persistence paths. |
| Recreating `writing_agent` | Schema comments state it was removed and migrated to `swarm_workspace_agent`. |
| Adding workflow states without updating `Execution` pruning/completion rules | Causes stuck or incorrectly completed workflows. |
| Ignoring `SKIPPED` in branch/convergence logic | Breaks conditional workflows with merge nodes. |
| Logging credentials, tokens, verification codes, full prompts, or full SSE payloads | Creates security exposure and noisy logs. |
| Catching `Exception` and returning `null` | Hides failure semantics and forces callers to guess. |
| Swallowing infrastructure exceptions without explicit fallback behavior | Produces false success and corrupts operational state. |
| Directly injecting Spring AI chat/model beans for user-facing calls | Auto-config is disabled; model selection must come from user LLM config. |

---

## Review Checklist

- [ ] Architecture alignment check was written before implementation.
- [ ] Existing wrappers, ports, services, repositories, and schema were searched.
- [ ] New names follow the naming table or intentionally match an existing
      module legacy pattern.
- [ ] Domain entities/value objects remain framework-clean.
- [ ] HTTP errors are translated at the interfaces boundary.
- [ ] Redis/HTTP/Spring AI clients are behind project wrappers or adapters.
- [ ] Database changes align SQL, PO, mapper, repository, and DTO behavior.
- [ ] Workflow state changes cover `SKIPPED`, pause/resume, failure, and
      convergence nodes.
- [ ] Logs use SLF4J placeholders and do not include secrets.
- [ ] Tests or focused validation cover the affected state machine or boundary.
