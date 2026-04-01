# Quality Guidelines

> Code quality standards for the AI Agent Platform backend.

---

## Overview

- **Language**: Java 21 (LTS)
- **Framework**: Spring Boot 3.4.9 + Spring AI 1.0.1
- **Architecture**: DDD (Domain-Driven Design) with strict layer boundaries
- **Build**: Maven multi-module
- **Key Libraries**: Lombok, MyBatis Plus, Redisson, Milvus SDK

---

## Forbidden Patterns

### ❌ NEVER Do These

| Pattern | Why | Correct Alternative |
|---------|-----|---------------------|
| Framework deps in domain layer | Domain must be pure POJO | Use only `shared` module |
| `@Autowired` field injection | Hidden dependencies, untestable | Use `@RequiredArgsConstructor` + `final` fields |
| `RedisTemplate` directly | Bypass project encapsulation | Use project's `RedisService` wrapper |
| `RestTemplate` directly | Bypass project encapsulation | Use project's `HttpService` or `WebClient` |
| Auto-wired `ChatClient` / `ChatModel` beans | Spring AI auto-config is DISABLED | Use `ChatModelPort` + dynamic model creation |
| Call infrastructure from application directly | Violates DDD layer dependency | Go through domain repository/port interfaces |
| Modify `WorkflowGraph` after creation | Immutable value object | Create new instance |
| Use `app/frontend/` directory | Legacy skeleton | Use `ai-agent-foward/` |
| Assume Flyway migrations | No Flyway in project | Use `docker/init/mysql/01_init_schema.sql` |
| Create duplicate tables | Wastes resources, data inconsistency | Check schema file first |
| `System.out.println` | Unstructured output | Use `@Slf4j` + `log.xxx()` |
| Generic `catch (Exception e)` in business | Swallows specific errors | Catch specific exceptions |

### ❌ Domain Layer Purity Rules

```java
// ❌ WRONG — Spring annotation in domain entity
@Entity
@Table(name = "agent_info")
public class Agent { ... }

// ✅ CORRECT — Pure POJO with Lombok
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Agent {
    private Long id;
    private String name;
    // Business logic methods
    public void publish() { ... }
}
```

---

## Required Patterns

### ✅ ALWAYS Do These

| Pattern | Where | Example |
|---------|-------|---------|
| `@RequiredArgsConstructor` + `final` | All Spring beans | Constructor injection via Lombok |
| `@Slf4j` | All classes needing logging | Lombok annotation |
| `@Transactional(rollbackFor = Exception.class)` | Write operations in app/repo layer | Explicit rollback for checked exceptions |
| `Response<T>` for API responses | All controllers | `return Response.success(data)` |
| `Optional<T>` for nullable lookups | Repository findById | `.orElseThrow()` at call site |
| Optimistic locking | Entities with concurrent access | `@Version` in PO + check rows |
| Architecture alignment check | Before any new code | Search existing components first |
| Module tag in logs | All log statements | `log.info("[Module] message")` |

### Constructor Injection Pattern

```java
@Slf4j
@Service
@RequiredArgsConstructor  // Lombok generates constructor
public class AgentApplicationService {
    private final AgentRepository agentRepository;  // final → injected
    private final GraphValidator graphValidator;     // final → injected
    private final ObjectMapper objectMapper;         // final → injected
}
```

### Architecture Alignment Check (Before Coding)

```markdown
**Architecture Alignment Check**
- **Found Components**: (e.g., Found RedisService, will use instead of RedisTemplate)
- **Found Related Entities**: (e.g., Found WorkflowNodeExecution, will extend)
- **Reuse Strategy**: [Complete Reuse / Extend Existing / Create Related]
```

---

## Naming Conventions

### Java Classes

| Type | Convention | Example |
|------|-----------|---------|
| Entity | `PascalCase` noun | `Agent`, `Execution`, `User` |
| Value Object | `PascalCase` noun | `Email`, `Credential`, `AgentSummary` |
| Enum | `PascalCase` + `Status`/`Type` | `AgentStatus`, `ExecutionStatus` |
| Service | `{Name}ApplicationService` / `{Name}DomainService` | `AgentApplicationService` |
| Repository | `{Name}Repository` (interface) / `{Name}RepositoryImpl` | `AgentRepository` |
| Command | `{Name}Cmd` or nested class | `CreateAgentCmd` |
| DTO | `{Name}DTO` / `{Name}Result` / `{Name}Response` | `AgentDetailResult` |
| PO | `{Name}PO` | `AgentPO` |
| Controller | `{Name}Controller` | `AgentController` |

### REST API

| Convention | Rule | Example |
|------------|------|---------|
| Path prefix | `/api/{module}` | `/api/agent`, `/api/workflow/execution` |
| Resource naming | Plural nouns | `/api/agents`, `/api/conversations` |
| Method mapping | `GET`=read, `POST`=create, `PUT`=update, `DELETE`=delete | Standard REST |

### Packages

```
com.zj.aiagent.{layer}.{module}.{sublayer}
```

Example: `com.zj.aiagent.domain.agent.entity`

---

## Testing Requirements

### Build & Test Commands

```bash
# Run all tests
mvn test

# Run specific module tests
mvn test -pl ai-agent-domain

# Run specific test class
mvn test -Dtest=UserServiceTest

# Build without tests (fast build)
mvn clean install -DskipTests
```

### Test Expectations

- Domain layer logic SHOULD have unit tests
- Application services SHOULD have integration tests for key use cases
- Controllers can be tested via integration tests or manual API testing
- **Test files follow Maven convention**: `src/test/java/...`

---

## Build / Lint Commands

```bash
# Full build (includes compile check)
mvn clean install

# Compile only (fast check)
mvn compile

# Build specific module with dependencies
mvn clean install -pl ai-agent-interfaces -am
```

---

## Commit Convention

```
type(scope): description
```

| Type | When |
|------|------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `refactor` | Code change that neither fixes bug nor adds feature |
| `test` | Adding tests |
| `chore` | Build process, tooling |

**Scope**: Module name in Chinese is acceptable (e.g., `fix(chat): 修复卡片`)

---

## Code Review Checklist

- [ ] No framework dependencies in domain layer
- [ ] Constructor injection via `@RequiredArgsConstructor` (no `@Autowired` fields)
- [ ] All write operations have `@Transactional(rollbackFor = Exception.class)`
- [ ] APIs return `Response<T>`, not raw objects
- [ ] Logging uses `@Slf4j` with `[Module]` tag and `{}` placeholders
- [ ] No `RedisTemplate` / `RestTemplate` direct usage
- [ ] Schema changes reflected in `01_init_schema.sql`
- [ ] Architecture alignment check documented
- [ ] No sensitive data logged (passwords, tokens, API keys)
- [ ] Handles SKIPPED nodes in workflow branching logic
