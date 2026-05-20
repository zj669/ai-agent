你是一名只负责调度和协调的专业管理人员，从不干任何需要花费时间的任务（如果有需要你去完成的任务，你可以选择马上招募一位新人员或者寻找空闲的队员去做），你负责处理各个队员之间的工作协调，只负责创建任务（如果需求比较复杂，请招募一位专门拆解任务的成员来做），统计任务进度，并在定时主动推进项目进展，在长时间队员没有回应的时候会主动询问。

1. 使用基本命令如文件操作等，去操控工作目录外的数据请使用Desktop Commander去执行
2. 涉及网络搜索的时候使用duckduckgo进行搜索


这个 team 模式下，成员是 mailbox/in-process 机制，不是每个都能用 resume 续跑， 后续统一使用发消息催办 + TaskList/TaskGet 看 owner 状态来跟踪，不再用 resume 误判。
---

## Project Overview

AI Agent Platform - A DDD-based workflow orchestration system with AI capabilities, built on Spring Boot 3.4.9 and React 19. The platform enables dynamic agent creation, workflow execution with conditional branching, knowledge management with vector search, and real-time streaming via SSE.

## Runtime Entry (Read This First)

The canonical runtime entry for day-to-day work is the workspace skill:
`.claude/skills/ai-agent-workspace/SKILL.md`. It carries the current business
domain map, `Meet -> See` routes, `.env` discipline, safety rules, and rolling
work logs under `logs/YYYY-MM.md`. Prefer routing through that skill; this
CLAUDE.md stays as a stable architectural reference.

## Build & Run Commands

### Backend (Java 21 + Maven)

```bash
# Build all modules
mvn clean install

# Build without tests (faster)
mvn clean install -DskipTests

# Build specific module with dependencies
mvn clean install -pl ai-agent-interfaces -am

# Run application with local profile
mvn spring-boot:run -pl ai-agent-interfaces -Dspring-boot.run.profiles=local

# Run all tests
mvn test

# Run specific module tests
mvn test -pl ai-agent-domain

# Run specific test class
mvn test -Dtest=UserServiceTest
```

### Frontend (React 19 + Vite)

The main frontend lives in `ai-agent-foward/` (NOT `app/frontend/` which is a legacy skeleton).

```bash
cd ai-agent-foward

# Install dependencies
npm install

# Start dev server (http://localhost:5173)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

### Docker Services

```bash
cd docker

# Start all services (MySQL, Redis, Milvus, MinIO, etcd)
docker-compose up -d

# View service status
docker-compose ps

# View logs
docker-compose logs -f [service-name]

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

## Architecture

### DDD Layered Architecture

The project follows **Domain-Driven Design** with strict dependency rules:

```
interfaces → application → domain ← infrastructure
                            ↑
                          shared
```

**Critical Rule**: The `domain` layer must remain pure - it depends ONLY on `shared` and has NO dependencies on Spring, MyBatis, or any framework.

### Module Structure

- **ai-agent-shared**: Common utilities, constants, design patterns
  - **Dependencies**: Lombok, Hutool, Guava, Commons Lang3, Fastjson, SLF4J API
  - **Critical**: NO framework dependencies (no Spring, no MyBatis)
- **ai-agent-domain**: Core business logic - entities, value objects, domain services, repository interfaces, ports
  - **Dependencies**: ONLY `shared` module
  - **Critical**: NO Spring, MyBatis, or infrastructure dependencies
- **ai-agent-application**: Use case orchestration - application services, DTOs, commands, events
- **ai-agent-infrastructure**: Technical implementations - database (MyBatis Plus), Redis (Redisson), Milvus, MinIO, external service adapters
- **ai-agent-interfaces**: REST controllers, WebSocket, Spring configuration, application entry point

### Domain Boundaries

The system is organized into bounded contexts. Actual subpackages under
`ai-agent-domain/src/main/java/com/zj/aiagent/domain/`:

**Core domains** (first-class routing in the workspace skill):

- **workflow**: Workflow execution engine (Execution aggregate root, WorkflowGraph, ExecutionContext, 7 NodeExecutorStrategy implementations)
- **agent**: Agent management (Agent aggregate root, model configuration, MCP tool binding)
- **chat**: Conversation management (Conversation, Message, SSE-backed streaming)
- **knowledge**: Knowledge base (KnowledgeDataset, KnowledgeDocument, vector storage via Milvus)
- **user** + **auth**: User management and authentication (注意 REST 前缀是 `/client/user`，不是 `/api/user`)

**Supporting / extension domains** (present in the codebase, 尚未在工作区 skill 单独建入口):

- **swarm**: Multi-agent workspace, groups, graph, SSE (`/api/swarm/**`)
- **writing**: Writing-assistant flows (`/api/writing`)
- **mcp**: MCP server registry and tool metadata (`/api/mcp`)
- **llm**: LLM provider configuration (`/api/llm-config`)
- **dashboard**: Aggregated metrics (`/api/dashboard`)
- **memory**: Conversation memory ports used by workflow's ExecutionContext

**Dependency Rules**:
- `workflow` can depend on `agent` (read graph definition), `knowledge` (retrieval), and `memory` (LTM/STM)
- `agent`, `knowledge`, `user`/`auth`, `dashboard` should NOT depend on other business domains
- Cross-domain coordination happens in the application layer (e.g., `SchedulerService`, `ChatApplicationService`)

### Frontend Architecture

The frontend (`ai-agent-foward/`) uses a flat structure (NOT feature-based):

```
ai-agent-foward/src/
├── components/        # UI 组件
├── hooks/             # 业务逻辑 Hook
├── pages/             # 页面
├── services/          # API 请求
├── stores/            # Zustand 状态管理
├── styles/            # 全局样式
├── types/             # 类型定义
└── index.tsx          # 入口
```

Key frontend dependencies:
- **React**: 19.2.1
- **Vite**: 6.2.0
- **TypeScript**: 5.8.2
- **Ant Design**: 6.1.1 (UI 组件库)
- **@xyflow/react**: 12.10.0 (工作流画布)
- **Zustand**: 5.0.9 (状态管理)
- **Tailwind CSS**: 4.1.18
- **Zod**: 4.2.1 (Schema 验证)
- **react-hook-form**: 7.69.0 (表单管理)
- **Axios**: HTTP 客户端

## Key Technical Patterns

### Workflow Execution Flow

1. **Start**: `WorkflowController` → `SchedulerService.startExecution` → `Execution.start()` → returns ready nodes
2. **Memory Hydration**: `SchedulerService.hydrateMemory` → VectorStore (LTM) + ConversationRepository (STM) → ExecutionContext
3. **Node Execution**: `NodeExecutorStrategy` → StreamPublisher → RedisSsePublisher (Redis Pub/Sub) → SSE → Frontend
4. **Advance**: `Execution.advance(nodeId, result)` → updates node status, handles conditional branching, returns next ready nodes
5. **Completion**: `SchedulerService.onExecutionComplete` → extracts final response → `ChatApplicationService.completeAssistantMessage`

### Conditional Branching

- Condition nodes support two modes: `EXPRESSION` (SpEL) and `LLM` (semantic understanding)
- After condition node completes, `Execution.pruneUnselectedBranches` marks unselected branches as `SKIPPED`
- Convergence nodes (multiple predecessors) are only skipped if ALL predecessors are SKIPPED

### State Management

- **Execution Status**: `PENDING → RUNNING → PAUSED/PAUSED_FOR_REVIEW → SUCCEEDED/FAILED/CANCELLED`
- **Node Status**: `PENDING → RUNNING → SUCCEEDED/FAILED/SKIPPED`
- Checkpoints stored in Redis for pause/resume; final results persisted to MySQL
- Human review triggers `PAUSED_FOR_REVIEW` state, requires `resumeExecution` to continue

### Ports & Adapters

Domain layer defines ports (interfaces), infrastructure layer provides adapters:

- `ChatModelPort` → `OpenAiChatModelAdapter` (Spring AI integration)
- `VectorStore` → `MilvusVectorStoreAdapter` (Milvus SDK)
- `StreamPublisher` → `RedisSsePublisher` (Redis Pub/Sub)
- `ExecutionRepository` → `ExecutionRepositoryImpl` (Redis + MySQL)

### Node Executor Types

Seven executor strategies implemented in `ai-agent-infrastructure/.../workflow/executor/`, resolved at runtime by `NodeExecutorFactory`:

- `StartNodeExecutorStrategy` - 工作流起始节点
- `EndNodeExecutorStrategy` - 工作流结束节点
- `LlmNodeExecutorStrategy` - LLM 调用节点
- `ConditionNodeExecutorStrategy` - 条件分支节点 (EXPRESSION / LLM)
- `HttpNodeExecutorStrategy` - HTTP 请求节点
- `ToolNodeExecutorStrategy` - 工具调用节点（MCP 工具）
- `KnowledgeNodeExecutorStrategy` - 知识库召回节点

## Code Reuse Requirements

**CRITICAL**: Before writing any code, you MUST search for existing components:

1. **Search for Services**: Use `Grep` to find existing service wrappers (e.g., `RedisService`, `HttpService`)
   - ❌ NEVER use `RedisTemplate`, `RestTemplate` directly
   - ✅ ALWAYS use project-encapsulated services

2. **Search for Entities**: Check existing database schema and PO classes before creating new tables
   - Location: `docker/init/mysql/01_init_schema.sql`
   - ❌ NEVER create duplicate tables (e.g., if `WorkflowNodeExecution` exists, don't create `WorkflowExecution`)

3. **Search for Business Logic**: Look for similar methods in domain services before implementing

**Required Output Format** before coding:
```markdown
**Architecture Alignment Check**
- **Found Components**: (e.g., Found RedisService, will use instead of RedisTemplate)
- **Found Related Entities**: (e.g., Found WorkflowNodeExecution, will extend instead of creating new table)
- **Reuse Strategy**: [Complete Reuse / Extend Existing / Create Related]
```

## Naming Conventions

### Backend (Java)

- **Entities**: `Agent.java`, `User.java` (aggregate roots)
- **Value Objects**: `Email.java`, `Credential.java`
- **Repository Interfaces**: `AgentRepository.java` (NOT `IAgentRepository`)
- **Repository Implementations**: `AgentRepositoryImpl.java`
- **Domain Services**: `AgentDomainService.java`
- **Application Services**: `AgentApplicationService.java`
- **Commands**: `CreateAgentCmd.java`, `UpdateAgentCmd.java`
- **DTOs**: `AgentDTO.java`, `AgentListDTO.java`
- **PO (Persistence Objects)**: `AgentPO.java`, `UserPO.java`
- **Mappers**: `AgentMapper.java` + `AgentMapper.xml`
- **Controllers**: `AgentController.java`

### Frontend (TypeScript/React)

- **Components/Pages**: `PascalCase` (e.g., `AgentCard.tsx`, `LoginPage.tsx`)
- **Hooks**: `camelCase` with `use` prefix (e.g., `useAgentList.ts`)
- **Services**: `camelCase` (e.g., `apiClient.ts`, `agentService.ts`)
- **Constants**: `UPPER_SNAKE_CASE`

## Database Management

### Schema File

- Location: `docker/init/mysql/01_init_schema.sql` (mounted into the MySQL container via `docker/docker-compose.yml` at `/docker-entrypoint-initdb.d`)
- No Flyway — schema is applied only on the **first** MySQL container start; changing it after data exists requires either `docker-compose down && docker volume rm <mysql_volume>` (destroys data) or a manual migration
- Always check this file before creating or modifying tables

### Table Naming

- Use snake_case: `agent_info`, `user_account`, `workflow_node_execution`
- Logical delete: `deleted` (0=active, 1=deleted)
- Timestamps: `create_time`, `update_time`

## REST API Controllers

| Controller | Path | Description |
|------------|------|-------------|
| AgentController | `/api/agent` | Agent CRUD 管理 |
| WorkflowController | `/api/workflow/execution` | 工作流执行、SSE 订阅 |
| HumanReviewController | `/api/workflow/reviews` | 人工审核恢复 |
| ChatController | `/api/chat` | 对话管理，SSE 流式消息 |
| KnowledgeController | `/api/knowledge` | 知识库管理 |
| UserController | `/client/user` | 注册 / 登录 / 刷新 token / 个人信息（前缀 `/client`，不是 `/api`）|
| MetadataController | `/api/meta` | 节点类型元数据 |
| DashboardController | `/api/dashboard` | 看板聚合数据 |
| LlmConfigController | `/api/llm-config` | LLM provider 配置 |
| McpServerController | `/api/mcp` | MCP server 注册与工具元数据 |
| WritingController | `/api/writing` | 写作助手相关接口 |
| SwarmWorkspace / Group / Graph / Agent / Sse Controllers | `/api/swarm/**` | 多 Agent 工作空间、群组、图结构、SSE 流 |

## Service Ports

| Service | Port | Description |
|---------|------|-------------|
| Spring Boot | 8080 | Backend API |
| Vite Dev | 5173 | Frontend dev server |
| MySQL | 13306 | Primary database |
| Redis | 6379 | Cache & pub/sub |
| Milvus | 19530 | Vector database |
| MinIO API | 9002 (host) → 9000 (container) | Object storage, 注意 host 端口是 9002，不是 9000 |
| MinIO Console | 9001 | MinIO admin UI |
| etcd | 2379 | Milvus dependency |

## Health Checks

```bash
# Application health
curl http://localhost:8080/actuator/health

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# Application info
curl http://localhost:8080/actuator/info
```

## Important Notes

### Spring AI Configuration

- Spring AI auto-configuration is DISABLED
- Models are created dynamically by users via `ChatModelPort`
- Do NOT rely on auto-injected `ChatClient` or `ChatModel` beans

### Protobuf Version

- Project uses Milvus SDK which requires protobuf 3.25.3
- Version is managed in parent POM to avoid conflicts

### Blueprint Documentation

Long-form architecture docs mirror the Maven module layout under `.blueprint/`:

- `.blueprint/README.md` — top-level map
- `.blueprint/ai-agent-domain/src/...` — domain model (entities, value objects, services, ports)
- `.blueprint/ai-agent-application/src/...` — application service orchestration
- `.blueprint/ai-agent-infrastructure/src/...` — adapters and technical implementations
- `.blueprint/ai-agent-interfaces/src/...` — REST / SSE controllers and configuration
- `.blueprint/ai-agent-foward/src/...` — frontend structure

Short-form runtime routing index: `.claude/skills/ai-agent-workspace/`.

### Trellis Workflow (`.trellis/`)

- `.trellis/spec/backend/`, `.trellis/spec/frontend/`, `.trellis/spec/guides/` hold executable specs (directory structure, quality guidelines, error handling, logging, code reuse thinking)
- `.trellis/tasks/` tracks in-progress task PRDs; completed ones move to `.trellis/tasks/archive/`
- `.trellis/config.yaml` and `.trellis/workflow.md` describe the flow
- Registered slash commands include `/trellis:start`, `/trellis:before-backend-dev`, `/trellis:check-backend`, `/trellis:finish-work`, `/trellis:break-loop`, `/trellis:record-session`

## Common Pitfalls

1. **Don't bypass domain layer**: Never call infrastructure directly from application layer - always go through domain repositories/ports
2. **Don't create framework dependencies in domain**: Domain entities should be POJOs with business logic only
3. **Don't duplicate functionality**: Always search for existing services before implementing
4. **Don't use auto-wired Spring AI beans**: Models are user-configured, not auto-injected
5. **Don't modify WorkflowGraph after creation**: It's an immutable value object
6. **Don't forget to handle SKIPPED nodes**: Conditional branching requires proper pruning logic
7. **Don't use `app/frontend/`**: The active frontend is `ai-agent-foward/`
8. **Don't assume Flyway**: Database schema is in `docker/init/mysql/01_init_schema.sql`, not managed by Flyway migrations

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **ai-agent** (10398 symbols, 22664 relationships, 300 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> If any GitNexus tool warns the index is stale, run `npx gitnexus analyze` in terminal first.

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `gitnexus_impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `gitnexus_detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `gitnexus_query({query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `gitnexus_context({name: "symbolName"})`.

## Never Do

- NEVER edit a function, class, or method without first running `gitnexus_impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `gitnexus_rename` which understands the call graph.
- NEVER commit changes without running `gitnexus_detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/ai-agent/context` | Codebase overview, check index freshness |
| `gitnexus://repo/ai-agent/clusters` | All functional areas |
| `gitnexus://repo/ai-agent/processes` | All execution flows |
| `gitnexus://repo/ai-agent/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->
