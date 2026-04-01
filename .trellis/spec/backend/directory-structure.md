# Directory Structure

> DDD layered architecture for the AI Agent Platform backend (Java 21 + Spring Boot 3.4.9 + Spring AI 1.0.1).

---

## Overview

The backend follows a strict **Domain-Driven Design (DDD)** multi-module Maven architecture. Dependency direction is unidirectional:

```
interfaces → application → domain ← infrastructure
                            ↑
                          shared
```

**Critical Rule**: The `domain` layer is pure — it has ZERO dependencies on Spring, MyBatis, or any framework. Only depends on `shared`.

---

## Top-Level Module Layout

```
ai-agent/                          # Parent POM (Maven multi-module)
├── ai-agent-shared/               # Common utilities, design patterns, constants
├── ai-agent-domain/               # Core business logic (PURE, NO framework deps)
├── ai-agent-application/          # Use case orchestration, DTOs, commands
├── ai-agent-infrastructure/       # Technical implementations, adapters
├── ai-agent-interfaces/           # REST controllers, configs, entry point
└── ai-agent-foward/               # Frontend (React 19 + Vite)
```

---

## Module Details

### ai-agent-shared

Shared utilities with NO framework dependencies (NO Spring, NO MyBatis).

```
shared/
├── constants/               # Business constants
├── context/                 # UserContext (ThreadLocal-based)
│   └── UserContext.java
├── design/                  # Reusable design patterns
│   ├── dag/                 # DAG execution framework
│   │   ├── DagContext.java
│   │   ├── DagNode.java
│   │   ├── ConditionalDagNode.java
│   │   ├── NodeRouteDecision.java
│   │   └── DagNodeExecutionException.java
│   └── ruletree/            # Strategy/rule tree pattern
│       ├── AbstractStrategyRouter.java
│       ├── StrategyHandler.java
│       └── factory/
├── model/                   # Shared data models
├── response/                # Unified API response wrapper
│   └── Response.java        # Response<T> with success()/error()
└── util/                    # Common utilities
```

**Dependencies**: Lombok, Hutool, Guava, Commons Lang3, Fastjson, SLF4J API

### ai-agent-domain

Core business logic. **MUST remain pure** — POJO entities with business logic, NO annotations from Spring/MyBatis.

```
domain/
├── agent/                   # Agent bounded context
│   ├── entity/              # Aggregate roots & entities
│   │   ├── Agent.java       # Aggregate root
│   │   └── AgentVersion.java
│   ├── repository/          # Repository interfaces (ports)
│   │   └── AgentRepository.java
│   ├── service/             # Domain services
│   │   └── GraphValidator.java
│   └── valobj/              # Value objects & enums
│       ├── AgentStatus.java
│       └── AgentSummary.java
├── workflow/                # Workflow bounded context
│   ├── entity/              # Execution, WorkflowGraph, Node, Edge
│   ├── config/              # NodeConfig, RetryPolicy, HumanReviewConfig
│   ├── service/             # WorkflowGraphFactory
│   ├── valobj/              # ExecutionStatus, Checkpoint, ExecutionContext
│   └── port/                # Ports (ChatModelPort, StreamPublisher, etc.)
├── chat/                    # Chat bounded context
├── knowledge/               # Knowledge bounded context
├── user/                    # User bounded context
│   ├── entity/
│   ├── repository/          # IUserRepository, IVerificationCodeRepository
│   ├── service/             # UserAuthenticationDomainService
│   ├── valobj/              # Email, Credential, UserStatus
│   └── exception/           # AuthenticationException
├── auth/                    # Auth bounded context
│   └── service/             # ITokenService, RateLimiter
├── llm/                     # LLM configuration
├── memory/                  # Memory (LTM/STM)
├── swarm/                   # Multi-agent swarm
├── dashboard/               # Dashboard projections
└── writing/                 # Writing sessions
```

**Dependencies**: ONLY `ai-agent-shared`

### ai-agent-application

Use case orchestration layer. Coordinates domain objects to fulfill business use cases.

```
application/
├── agent/
│   ├── cmd/                 # Command objects (CreateAgentCmd, UpdateAgentCmd)
│   ├── dto/                 # DTOs (AgentDetailResult, AgentRequest)
│   └── service/             # AgentApplicationService, MetadataApplicationService
├── workflow/
│   └── SchedulerService.java  # Workflow execution orchestrator
├── chat/
│   ├── ChatApplicationService.java
│   ├── event/               # Application events (MessageAppendedEvent)
│   └── listener/            # Event listeners
├── user/
│   ├── UserApplicationService.java
│   └── dto/                 # UserLoginResponse, TokenRefreshResponse
├── knowledge/
├── llm/
├── swarm/
├── dashboard/
└── writing/
```

**Dependencies**: `domain`, `shared`, Spring Framework

### ai-agent-infrastructure

Technical implementations — adapters for domain ports.

```
infrastructure/
├── agent/
│   ├── mapper/              # MyBatis Plus mappers (AgentMapper, AgentVersionMapper)
│   ├── po/                  # Persistence objects (AgentPO, AgentVersionPO)
│   └── repository/          # AgentRepositoryImpl (implements domain interface)
├── workflow/
│   ├── adapter/             # Port adapters (RedisHumanReviewQueueAdapter)
│   ├── executor/            # Node executor strategies (LlmNodeExecutor, etc.)
│   ├── mapper/              # MyBatis mappers
│   ├── persistence/         # Repository implementations
│   ├── stream/              # RedisSseStreamPublisher
│   └── template/            # PromptTemplateResolver
├── config/                  # Infrastructure configs (Redis, MinIO, WebSocket)
├── redis/                   # Redis utilities (RedisService wrapper)
├── auth/
│   ├── token/               # JwtTokenService
│   └── redis/               # RedisSlidingWindowRateLimiter
├── user/
├── chat/
├── knowledge/
├── llm/
├── memory/
├── swarm/
├── email/
├── meta/
└── dashboard/
```

**Dependencies**: `domain`, `shared`, Spring, MyBatis Plus, Redisson, Milvus SDK, MinIO SDK

### ai-agent-interfaces

API layer — REST controllers, interceptors, Spring Boot entry point.

```
interfaces/
├── AiAgentApplication.java         # @SpringBootApplication entry point
├── config/                          # Global configurations
│   ├── ThreadPoolConfig.java
│   ├── EmbeddingModelConfig.java
│   ├── MybatisPlusConfig.java
│   ├── DataSourceConfig.java
│   └── WebClientConfig.java
├── common/
│   ├── interceptor/                 # Auth interceptors (JWT, Debug strategies)
│   ├── config/                      # WebMvcConfig
│   └── advice/                      # GlobalExceptionHandler
├── agent/                           # AgentController
├── workflow/                        # WorkflowController, HumanReviewController
├── chat/                            # ChatController
├── knowledge/web/                   # KnowledgeController
├── user/                            # UserController
├── llm/                             # LlmConfigController
├── swarm/                           # SwarmWorkspaceController, SwarmSseController
├── meta/                            # MetadataController
├── dashboard/web/                   # DashboardController
└── writing/                         # WritingController
```

**Dependencies**: `application`, `infrastructure`, `domain`, `shared`, Spring Boot starters

---

## Module Organization Rules

### Adding a New Bounded Context (e.g., `notification`)

1. **domain**: Create `domain/notification/` with `entity/`, `repository/`, `service/`, `valobj/`
2. **infrastructure**: Create `infrastructure/notification/` with `mapper/`, `po/`, `repository/`
3. **application**: Create `application/notification/` with `service/`, `dto/`, `cmd/`
4. **interfaces**: Create `interfaces/notification/` with controller

### Within Each Bounded Context

| Sub-package | Contains | Example |
|-------------|----------|---------|
| `entity/` | Aggregate roots, entities | `Agent.java`, `Execution.java` |
| `valobj/` | Value objects, enums | `AgentStatus.java`, `Email.java` |
| `repository/` | Repository interfaces (ports) | `AgentRepository.java` |
| `service/` | Domain services | `GraphValidator.java` |
| `port/` | Other ports (non-repository) | `ChatModelPort.java` |
| `config/` | Domain configuration VOs | `RetryPolicy.java` |
| `exception/` | Domain-specific exceptions | `AuthenticationException.java` |

---

## Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| Entity / Aggregate Root | `{Name}.java` | `Agent.java`, `Execution.java` |
| Value Object | `{Name}.java` | `Email.java`, `Credential.java` |
| Enum | `{Name}Status.java` / `{Name}Type.java` | `AgentStatus.java`, `NodeType.java` |
| Repository Interface | `{Name}Repository.java` | `AgentRepository.java` |
| Repository Impl | `{Name}RepositoryImpl.java` | `AgentRepositoryImpl.java` |
| Domain Service | `{Name}DomainService.java` | `UserAuthenticationDomainService.java` |
| Application Service | `{Name}ApplicationService.java` | `AgentApplicationService.java` |
| Command | `{Name}Cmd.java` or nested in `{Name}Command.java` | `CreateAgentCmd` |
| DTO | `{Name}DTO.java` or `{Name}Result.java` | `AgentDetailResult.java` |
| PO (Persistence Object) | `{Name}PO.java` | `AgentPO.java` |
| Mapper (MyBatis) | `{Name}Mapper.java` | `AgentMapper.java` |
| Controller | `{Name}Controller.java` | `AgentController.java` |
| Port (Domain) | `{Name}Port.java` | `ChatModelPort.java` |
| Adapter (Infrastructure) | `{Name}Adapter.java` | `OpenAiChatModelAdapter.java` |
| Config | `{Name}Config.java` | `RedisListenerConfig.java` |

---

## Domain Boundary Dependencies

```
workflow ──→ agent (read graph definition)
workflow ──→ knowledge (retrieval)
agent, knowledge, user/auth → NO cross-domain deps
```

Cross-domain coordination MUST happen in the **application layer** (e.g., `SchedulerService`).

---

## Examples

| Well-structured module | Path |
|------------------------|------|
| Agent (complete DDD) | `domain/agent/`, `infrastructure/agent/`, `application/agent/`, `interfaces/agent/` |
| User/Auth | `domain/user/`, `domain/auth/`, `infrastructure/user/`, `infrastructure/auth/` |
| Workflow | `domain/workflow/`, `infrastructure/workflow/`, `application/workflow/` |
