# 项目结构与架构规范 (Project Structure & Architecture)

## 架构概述

本项目采用 **领域驱动设计 (DDD)** 和 **六边形架构 (Hexagonal Architecture)** 的理念，实现清晰的分层和职责分离。

## 后端架构 (Backend Architecture)

### 分层结构

```
┌─────────────────────────────────────────┐
│         Interfaces Layer                │  接口层 (REST API, WebSocket)
│    (ai-agent-interfaces)                │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│       Application Layer                 │  应用层 (用例编排, DTO)
│    (ai-agent-application)               │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│         Domain Layer                    │  领域层 (核心业务逻辑)
│      (ai-agent-domain)                  │  ← 不依赖其他模块
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│     Infrastructure Layer                │  基础设施层 (数据库, 缓存)
│   (ai-agent-infrastructure)             │
└─────────────────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│         Shared Kernel                   │  共享内核 (工具, 常量)
│      (ai-agent-shared)                  │
└─────────────────────────────────────────┘
```

### 模块职责

#### 1. ai-agent-shared (共享内核)
**职责**: 提供跨模块的通用能力，无框架依赖。

**包含内容**:
- `constants/` - 常量定义
- `context/` - 上下文管理
- `design/` - 设计模式抽象
- `model/` - 通用模型
- `response/` - 统一响应格式

**依赖**: 无

**命名规范**:
- 常量类: `XxxConstants.java`
- 工具类: `XxxUtil.java`

#### 2. ai-agent-domain (领域层)
**职责**: 核心业务逻辑，保持纯净，不依赖框架。

**包含内容**:
```
domain/
├── agent/              # Agent 聚合根
│   ├── entity/        # 实体 (Agent, AgentConfig)
│   ├── valobj/        # 值对象 (ModelConfig, PromptTemplate)
│   ├── repository/    # 仓储接口
│   ├── service/       # 领域服务
│   └── exception/     # 领域异常
├── user/              # User 聚合根
├── workflow/          # Workflow 聚合根
├── chat/              # Chat 聚合根
└── knowledge/         # Knowledge 聚合根
```

**关键概念**:
- **Aggregate Root (聚合根)**: 领域对象的入口，如 `Agent`, `User`
- **Entity (实体)**: 有唯一标识的对象
- **Value Object (值对象)**: 无标识，通过属性值判断相等性
- **Repository (仓储)**: 只定义接口，实现在 infrastructure 层
- **Domain Service (领域服务)**: 跨实体的业务逻辑
- **Port (端口)**: 对外部依赖的抽象接口 (如 `ChatModelPort`, `EmbeddingPort`)

**依赖**: 仅依赖 `ai-agent-shared`

**命名规范**:
- 实体: `Agent.java`, `User.java`
- 值对象: `Email.java`, `Credential.java`
- 仓储接口: `AgentRepository.java`
- 领域服务: `AgentDomainService.java`
- 端口接口: `ChatModelPort.java`

#### 3. ai-agent-application (应用层)
**职责**: 编排领域对象完成用例，处理事务和事件。

**包含内容**:
```
application/
├── agent/
│   ├── cmd/           # 命令对象 (CreateAgentCmd)
│   ├── dto/           # 数据传输对象 (AgentDTO)
│   ├── service/       # 应用服务 (AgentApplicationService)
│   └── event/         # 领域事件 (AgentCreatedEvent)
├── chat/
├── knowledge/
└── workflow/
```

**关键概念**:
- **Application Service (应用服务)**: 用例的入口，协调领域对象
- **Command (命令)**: 表示用户意图的对象
- **DTO (数据传输对象)**: 跨层传输的数据结构
- **Event (事件)**: 领域事件的发布和监听

**依赖**: `ai-agent-domain`, `ai-agent-shared`

**命名规范**:
- 应用服务: `AgentApplicationService.java`
- 命令: `CreateAgentCmd.java`, `UpdateAgentCmd.java`
- DTO: `AgentDTO.java`, `AgentListDTO.java`
- 事件: `AgentCreatedEvent.java`

#### 4. ai-agent-infrastructure (基础设施层)
**职责**: 实现领域层定义的接口，提供技术能力。

**包含内容**:
```
infrastructure/
├── agent/
│   ├── po/            # 持久化对象 (AgentPO)
│   ├── mapper/        # MyBatis Mapper
│   └── repository/    # 仓储实现 (AgentRepositoryImpl)
├── auth/              # JWT 实现
├── chat/              # ChatModel 适配器
├── knowledge/         # Milvus, MinIO 适配器
├── memory/            # Redis 实现
├── config/            # 配置类
└── redis/             # Redis 工具
```

**关键概念**:
- **PO (Persistence Object)**: 数据库表映射对象
- **Mapper**: MyBatis 数据访问接口
- **Repository Impl**: 实现领域层的 Repository 接口
- **Adapter (适配器)**: 实现领域层的 Port 接口

**依赖**: `ai-agent-domain`, `ai-agent-shared`

**命名规范**:
- PO: `AgentPO.java`, `UserPO.java`
- Mapper: `AgentMapper.java` + `AgentMapper.xml`
- 仓储实现: `AgentRepositoryImpl.java`
- 适配器: `OpenAiChatModelAdapter.java`

#### 5. ai-agent-interfaces (接口层)
**职责**: 对外暴露 API，处理 HTTP 请求和响应。

**包含内容**:
```
interfaces/
├── agent/
│   ├── controller/    # REST 控制器
│   ├── dto/           # 请求/响应 DTO
│   └── converter/     # DTO 转换器
├── chat/
├── knowledge/
├── config/            # Spring 配置
│   ├── WebConfig.java
│   ├── SecurityConfig.java
│   └── ThreadPoolConfig.java
└── AiAgentApplication.java  # 启动类
```

**依赖**: 所有模块

**命名规范**:
- 控制器: `AgentController.java`
- 请求 DTO: `CreateAgentRequest.java`
- 响应 DTO: `AgentResponse.java`

### 数据库设计

#### 表命名规范
- 使用下划线分隔: `agent_info`, `user_account`
- 逻辑删除字段: `deleted` (0=未删除, 1=已删除)
- 时间戳字段: `create_time`, `update_time`

#### 迁移管理
- 使用 Flyway 管理数据库版本
- 迁移文件位置: `ai-agent-infrastructure/src/main/resources/db/migration/`
- 命名格式: `V{version}__{description}.sql`
  - 例如: `V1_0__init_schema.sql`

## 前端架构 (Frontend Architecture)

### Feature-Based 结构

```
src/
├── app/                    # 应用层
│   ├── routes.tsx         # 路由配置
│   └── App.tsx            # 根组件
├── features/               # 功能模块 (按业务领域)
│   ├── auth/
│   │   ├── api/           # API 请求
│   │   ├── components/    # UI 组件
│   │   ├── hooks/         # 业务逻辑 Hook
│   │   ├── pages/         # 页面
│   │   ├── types/         # 类型定义
│   │   └── routes.tsx     # 模块路由
│   ├── agent/
│   ├── orchestration/
│   ├── chat/
│   ├── knowledge/
│   └── human-review/
└── shared/                 # 共享资源
    ├── components/        # 通用组件
    ├── hooks/             # 通用 Hook
    ├── services/          # 基础服务
    └── utils/             # 工具函数
```

### 分层职责

#### Page (页面层)
- **职责**: 容器组件，负责布局和路由参数
- **禁止**: 直接包含业务逻辑和 API 调用
- **示例**: `AgentListPage.tsx`

```typescript
// ✅ Good
export default function AgentListPage() {
  const { agents, loading, deleteAgent } = useAgentList();
  return <AgentGrid data={agents} onDelete={deleteAgent} />;
}

// ❌ Bad
export default function AgentListPage() {
  const [agents, setAgents] = useState([]);
  useEffect(() => {
    fetch('/api/agents').then(...); // 不应直接调用 API
  }, []);
}
```

#### Hook (业务逻辑层)
- **职责**: 封装所有业务逻辑、状态管理、副作用
- **位置**: `features/xxx/hooks/useXxx.ts`
- **示例**: `useAgentList.ts`

```typescript
export function useAgentList() {
  const [agents, setAgents] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchAgents = async () => {
    setLoading(true);
    const data = await agentService.fetchAgentList();
    setAgents(data);
    setLoading(false);
  };

  useEffect(() => { fetchAgents(); }, []);

  return { agents, loading, deleteAgent };
}
```

#### Component (组件层)
- **Smart Component**: 连接业务逻辑
- **Dumb Component**: 纯展示组件，通过 Props 接收数据

#### API Layer (API 层)
- **职责**: 封装所有 HTTP 请求
- **位置**: `features/xxx/api/xxxService.ts`
- **禁止**: 在组件中直接使用 `apiClient`

```typescript
// ✅ Good - features/agent/api/agentService.ts
export const agentService = {
  fetchAgentList: () => apiClient.get('/api/agents'),
  createAgent: (data) => apiClient.post('/api/agents', data),
};

// ❌ Bad - 在组件中直接调用
apiClient.get('/api/agents');
```

### 命名规范

#### 文件命名
- 组件/页面: `PascalCase` (例: `AgentCard.tsx`, `LoginPage.tsx`)
- Hook: `camelCase`, 以 `use` 开头 (例: `useAgentList.ts`)
- 工具/服务: `camelCase` (例: `apiClient.ts`, `dateUtils.ts`)

#### 组件命名
- 与文件名保持一致
- 使用描述性名称: `AgentSelectionModal` 而非 `Modal1`

### 依赖规则

#### 禁止循环依赖
- Feature A 不应导入 Feature B 的内部组件
- 跨模块通信使用: URL 参数、全局状态、事件总线

#### 代码复用
- 模块内复用: 放在 `features/xxx/components/`
- 跨模块复用: 下沉到 `shared/components/`

#### 导入路径
- 模块内: 相对路径 `../../components/Button`
- 跨模块: 别名 `@/shared/...` (需配置 tsconfig)

## 代码规范

### 后端规范

#### 包命名
- 全小写，使用点分隔: `com.zj.aiagent.domain.agent`

#### 类命名
- 类名: `PascalCase`
- 接口: 不使用 `I` 前缀，如 `AgentRepository` 而非 `IAgentRepository`
- 实现类: 添加 `Impl` 后缀，如 `AgentRepositoryImpl`

#### 方法命名
- `camelCase`
- 查询方法: `findById`, `listAll`, `queryByCondition`
- 命令方法: `create`, `update`, `delete`, `execute`
- 布尔方法: `isValid`, `hasPermission`, `canExecute`

#### 注释规范
- 公共 API 必须有 Javadoc
- 复杂业务逻辑添加行内注释
- 中文注释可接受，但代码必须使用英文

### 前端规范

#### 变量命名
- `camelCase`: 变量、函数
- `PascalCase`: 组件、类型
- `UPPER_SNAKE_CASE`: 常量

#### 类型定义
- 优先使用 `interface` 而非 `type`
- 导出类型: `export interface AgentDTO { ... }`

#### 组件规范
- 使用函数组件和 Hooks
- Props 类型定义: `interface XxxProps { ... }`
- 避免过大的组件，及时拆分

## 测试规范

### 后端测试
- 单元测试: `src/test/java` 下，与源码包结构一致
- 测试类命名: `XxxTest.java`
- 测试方法命名: `should_xxx_when_yyy()`

### 前端测试
- 组件测试: `Xxx.test.tsx`
- Hook 测试: `useXxx.test.ts`

## Git 规范

### 分支命名
- 功能分支: `feature/xxx`
- 修复分支: `fix/xxx`
- 重构分支: `refactor/xxx`

### Commit 消息
- 格式: `<type>: <subject>`
- Type: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`
- 示例: `feat: add agent creation API`

## 部署结构

### Docker 服务
- MySQL: 端口 13306
- Redis: 端口 6379
- Milvus: 端口 19530
- MinIO: 端口 9000 (API), 9001 (Console)

### 应用部署
- 后端: Spring Boot JAR, 端口 8080
- 前端: Nginx 静态文件服务
- 健康检查: `/actuator/health`
