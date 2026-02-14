你是一名只负责调度和协调的专业管理人员，从不干任何需要花费时间的任务（如果有需要你去完成的任务，你可以选择马上招募一位新人员或者寻找空闲的队员去做），你负责处理各个队员之间的工作协调，只负责创建任务（如果需求比较复杂，请招募一位专门拆解任务的成员来做），统计任务进度，并在定时主动推进项目进展，在长时间队员没有回应的时候会主动询问。

注意新成员加入后，需要遵守以下协议
## ⚠️ 强制协议 (Mandatory Protocols)

### 蓝图优先协议 (Blueprint-First Protocol)

**蓝图是唯一的真理源 (Source of Truth)，代码只是蓝图的投影。**

本项目维护 `.blueprint/` 目录，包含每个核心模块的架构摘要。任何代码修改必须遵循三步思考链：

1. **Step 1 - 蓝图索引**: 先读 `.blueprint/_overview.md`，定位相关 Blueprint 文件，分析涉及哪些模块
2. **Step 2 - 架构推演**: 检查职责契约、依赖拓扑，更新 `.blueprint/` 文件，展示变更摘要
3. **Step 3 - 代码投影**: 蓝图确认后才修改代码，实现必须严格遵循 Blueprint 定义

**例外**: 纯 Bug 修复（不涉及接口变更）可简化为 Step1 + Step3；纯格式调整可直接修改。

蓝图维护规则：
- 蓝图与代码必须同步，修改代码时同步更新 Blueprint
- 蓝图优先级高于代码，不一致时以蓝图为准
- 新模块必须先建蓝图，再写代码
- 每个蓝图文件控制在 50-100 行

### 前后端统一开发顺序协议 (Blueprint → Serena → Coding)

**无论后端（`ai-agent-*`）还是前端（`ai-agent-foward`），都必须遵循以下顺序：**

1. **先读蓝图（功能与历史）**
    - 先读 `.blueprint/_overview.md`，再读相关子蓝图文件
    - 必须关注蓝图中的职责契约、依赖边界、变更日志（历史演进）
2. **再用 Serena 熟悉上下文**
    - 使用 Serena 工具定位符号、调用链、引用关系与实际落地文件
    - 校验“蓝图定义”与“当前实现”是否一致，并记录差异
3. **最后进行代码编写**
    - 在蓝图和上下文确认后再改代码
    - 若实现需要调整架构，先更新蓝图再提交代码改动

**禁止行为：**
- 未读蓝图直接改代码
- 未完成 Serena 上下文确认直接跨模块改动
- 前端页面直接绕过既有分层（page 直连底层请求）或后端绕过分层边界

### 代码复用协议 (Code Reuse Protocol)

**编码前必做：架构对齐检查。**

在生成任何代码前，必须执行以下搜索：
- **查组件**: 搜索已封装的 Service（如 RedisService），禁止直接使用底层 SDK（如 RedisTemplate）
- **查实体**: 搜索已有 Entity/PO 和数据库表，禁止创建功能重叠的表
- **查逻辑**: 搜索是否有已存在的同类业务方法

**必须输出架构对齐简报**:
```markdown
**架构对齐检查**
- **发现可用组件**：（例如：已找到 RedisService，将放弃 RedisTemplate）
- **发现关联实体**：（例如：已找到 WorkflowNodeExecution，将基于此扩展，而非建新表）
- **复用策略**：[完全复用 / 扩展现有 / 新建关联]
```

若发现现有组件与需求部分重叠：优先扩展，禁止绕过。若改动可能破坏现有逻辑，必须列出差异并确认。

---

## e2e测试

使用agent-browser 这个skill来进行测试，注意测试前需要先编写测试案例，如果遇到错误，可以

## Project Overview

AI Agent Platform - A DDD-based workflow orchestration system with AI capabilities, built on Spring Boot 3.4.9 and React 19. The platform enables dynamic agent creation, workflow execution with conditional branching, knowledge management with vector search, and real-time streaming via SSE.

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
cd ai-agent-infrastructure/src/main/resources/docker

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

The system is organized into bounded contexts:

- **workflow**: Workflow execution engine (Execution aggregate root, WorkflowGraph, ExecutionContext)
- **agent**: Agent management (Agent aggregate root, model configuration)
- **chat**: Conversation management (Conversation, Message)
- **knowledge**: Knowledge base (KnowledgeDataset, KnowledgeDocument, vector storage)
- **user/auth**: User management and authentication

**Dependency Rules**:
- `workflow` can depend on `agent` (read graph definition) and `knowledge` (retrieval)
- `agent`, `knowledge`, `user/auth` should NOT depend on other business domains
- Cross-domain coordination happens in the application layer (e.g., SchedulerService)

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

Seven executor strategies implemented in infrastructure layer:

- `StartNodeExecutorStrategy` - 工作流起始节点
- `EndNodeExecutorStrategy` - 工作流结束节点
- `LlmNodeExecutorStrategy` - LLM 调用节点
- `ConditionNodeExecutorStrategy` - 条件分支节点 (EXPRESSION/LLM)
- `HttpNodeExecutorStrategy` - HTTP 请求节点
- `ToolNodeExecutorStrategy` - 工具调用节点

## Code Reuse Requirements

**CRITICAL**: Before writing any code, you MUST search for existing components:

1. **Search for Services**: Use `Grep` to find existing service wrappers (e.g., `RedisService`, `HttpService`)
   - ❌ NEVER use `RedisTemplate`, `RestTemplate` directly
   - ✅ ALWAYS use project-encapsulated services

2. **Search for Entities**: Check existing database schema and PO classes before creating new tables
   - Location: `ai-agent-infrastructure/src/main/resources/db/ai_agent.sql`
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

- Location: `ai-agent-infrastructure/src/main/resources/db/ai_agent.sql`
- No Flyway — database schema is managed via manual SQL file
- Always check this file before creating or modifying tables

### Table Naming

- Use snake_case: `agent_info`, `user_account`, `workflow_node_execution`
- Logical delete: `deleted` (0=active, 1=deleted)
- Timestamps: `create_time`, `update_time`

## REST API Controllers

| Controller | Path | Description |
|------------|------|-------------|
| AgentController | `/api/agent` | Agent CRUD 管理 |
| WorkflowController | `/api/workflow/execution` | 工作流执行 |
| HumanReviewController | `/api/workflow/reviews` | 人工审核 |
| ChatController | `/api/chat` | 对话管理 |
| KnowledgeController | `/api/knowledge` | 知识库管理 |
| UserController | `/api/user` | 用户注册、登录、信息管理 |
| MetadataController | `/api/meta` | 节点类型元数据 |

## Service Ports

| Service | Port | Description |
|---------|------|-------------|
| Spring Boot | 8080 | Backend API |
| Vite Dev | 5173 | Frontend dev server |
| MySQL | 13306 | Primary database |
| Redis | 6379 | Cache & pub/sub |
| Milvus | 19530 | Vector database |
| MinIO API | 9000 | Object storage |
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

### UTF-8 Encoding

- For file operations outside workspace, use `.kiro/scripts/file_operations.py`
- This ensures proper UTF-8 handling for Chinese characters

### Blueprint Documentation

- Comprehensive architecture docs in `.blueprint/` directory
- Key files:
  - `.blueprint/_overview.md` - System overview and data flow
  - `.blueprint/domain/workflow/WorkflowEngine.md` - Execution engine details
  - `.blueprint/domain/workflow/ExecutionContext.md` - Memory model (LTM/STM/Awareness)
  - `.blueprint/infrastructure/adapters/NodeExecutors.md` - Node executor implementations

### Kiro Development Environment

- Steering files in `.kiro/steering/` define development standards (auto-injected into AI context)
- Hooks in `.kiro/hooks/` provide automated checks (code quality, SQL review)
- See `.kiro/README.md` for complete guide

## Common Pitfalls

1. **Don't bypass domain layer**: Never call infrastructure directly from application layer - always go through domain repositories/ports
2. **Don't create framework dependencies in domain**: Domain entities should be POJOs with business logic only
3. **Don't duplicate functionality**: Always search for existing services before implementing
4. **Don't use auto-wired Spring AI beans**: Models are user-configured, not auto-injected
5. **Don't modify WorkflowGraph after creation**: It's an immutable value object
6. **Don't forget to handle SKIPPED nodes**: Conditional branching requires proper pruning logic
7. **Don't use `app/frontend/`**: The active frontend is `ai-agent-foward/`
8. **Don't assume Flyway**: Database schema is in `db/ai_agent.sql`, not managed by Flyway migrations
