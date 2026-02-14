# 系统全局依赖拓扑 (System Overview)

## 业务域地图

```mermaid
graph TD
    subgraph Frontend["前端层"]
        APP[App Router]
        PAGE[Pages]
        HOOK[Hooks]
        STORE[Zustand Stores]
        SVC[Service + apiClient]
    end

    subgraph Interfaces["接口层"]
        UC[UserController]
        AC[AgentController]
        WC[WorkflowController]
        HRC[HumanReviewController]
        CC[ChatController]
        KC[KnowledgeController]
    end

    subgraph Application["应用层"]
        UAS[UserApplicationService]
        AAS[AgentApplicationService]
        SS[SchedulerService]
        CAS[ChatApplicationService]
        KAS[KnowledgeApplicationService]
    end

    subgraph Domain["领域层"]
        Agent[Agent 聚合根]
        Execution[Execution 聚合根]
        WG[WorkflowGraph 值对象]
        EC[ExecutionContext 值对象]
        User[User 聚合根]
        Conv[Conversation]
        Msg[Message]
        KD[KnowledgeDataset 聚合根]
        KDoc[KnowledgeDocument]
    end

    subgraph Infrastructure["基础设施层"]
        MySQL[(MySQL)]
        Redis[(Redis)]
        Milvus[(Milvus)]
        MinIO[(MinIO)]
    end

    UC --> UAS --> User
    AC --> AAS --> Agent
    WC --> SS --> Execution
    SS --> WG
    SS --> EC
    HRC --> SS
    CC --> CAS --> Conv & Msg
    KC --> KAS --> KD & KDoc

    APP --> PAGE
    PAGE --> HOOK
    HOOK --> STORE
    HOOK --> SVC
    SVC --> UC
    SVC --> AC
    SVC --> WC
    SVC --> HRC
    SVC --> CC
    SVC --> KC

    Execution -.->|LTM 检索| Milvus
    Execution -.->|流式输出/检查点/取消标记| Redis
    Execution -.->|执行日志/审核记录| MySQL
    Agent -->|包含| WG
    KDoc -->|向量化| Milvus
    KDoc -->|文件存储| MinIO
    SS -.->|消息管理| CAS
```

## 核心数据流

1. **用户认证**: UserController → UserApplicationService → UserAuthenticationDomainService → ITokenService
2. **工作流执行**: Controller → SchedulerService → Execution(聚合根) → NodeExecutorStrategy → StreamPublisher
2. **记忆水合**: SchedulerService.hydrateMemory → VectorStore(LTM) + ConversationRepository(STM) → ExecutionContext
3. **人工审核**: HumanReviewController → SchedulerService.resumeExecution → Execution.resume → CheckpointRepository
4. **流式推送**: NodeExecutorStrategy → StreamPublisher → RedisSsePublisher(Redis Pub/Sub) → SSE → 前端
5. **知识检索**: VectorStore(Milvus) → longTermMemories → LLM System Prompt
6. **对话系统**: ChatController → ChatApplicationService → Conversation/Message → ConversationRepository
7. **执行完成**: SchedulerService.onExecutionComplete → extractFinalResponse + buildThoughtSteps → ChatApplicationService.completeAssistantMessage
8. **前端数据链路**: Page → Hook → Service/apiClient → Controller → Application/Domain

## 工作流端口清单 (Domain Ports)

| 端口 | 职责 | 实现层 | 存储 |
|------|------|--------|------|
| ConditionEvaluatorPort | 结构化条件分支评估 | Infrastructure | - |
| NodeExecutorStrategy | 节点执行策略 | Infrastructure | - |
| StreamPublisher | 流式推送 | Infrastructure | Redis Pub/Sub |
| StreamPublisherFactory | 创建 StreamPublisher | Infrastructure | - |
| ExecutionRepository | 执行持久化 | Infrastructure | Redis + MySQL |
| CheckpointRepository | 检查点管理 | Infrastructure | Redis |
| WorkflowNodeExecutionLogRepository | 执行日志 | Infrastructure | MySQL |
| HumanReviewRepository | 审核记录 | Infrastructure | MySQL |
| HumanReviewQueuePort | 审核队列 | Infrastructure | Redis |
| WorkflowCancellationPort | 取消标记 | Infrastructure | Redis |
| VectorStore | 向量检索 (LTM) | Infrastructure | Milvus |

## 模块边界规则

| 领域 | 可以依赖 | 严禁依赖 |
|------|---------|---------|
| workflow | agent(读取图定义), knowledge(检索) | chat, user |
| agent | 无其他领域 | workflow, chat, knowledge |
| chat | agent(获取配置) | workflow内部实现, knowledge |
| knowledge | 无其他领域 | workflow, chat, agent |
| user/auth | 无其他领域 | 所有业务领域 |

注: SchedulerService (应用层) 可以调用 ChatApplicationService 管理消息，这是应用层编排的合法跨域协调。

## 前端边界规则

| 前端层 | 可以依赖 | 严禁依赖 |
|------|---------|---------|
| pages | hooks, components, stores(只读), types | 直接调用 axios/fetch |
| hooks | services, stores, types | 直接操作 DOM 或路由定义 |
| stores | types | 直接发 HTTP 请求 |
| services | apiClient, types | 直接读写页面状态 |
| components | props, types, 少量通用 hook | 直接访问后端接口 |

注: 前端新增页面/接口/状态字段时，必须先更新 `.blueprint/frontend/` 对应文件，再修改 `ai-agent-foward/src` 实现。

## 关键技术决策

- Execution 是工作流执行的聚合根，管理完整生命周期和状态机
- WorkflowGraph 是值对象，提供拓扑排序和环检测
- ExecutionContext 是"智能黑板"，承载 LTM/STM/Awareness 三层记忆模型
- NodeExecutorStrategy 策略模式，支持 START/END/LLM/CONDITION/HTTP/TOOL 六种节点类型
- 条件节点支持 EXPRESSION(SpEL) 和 LLM(语义理解) 两种路由模式
- Redis 临时存储执行上下文、检查点和取消标记；MySQL 持久化最终结果和审计日志
- SSE (Server-Sent Events) 通过 Redis Pub/Sub 实现流式输出，StreamPublisher 抽象推送接口
- 人工审核支持 BEFORE_EXECUTION(审核输入) 和 AFTER_EXECUTION(审核输出) 两个阶段
- ExecutionMode 支持 STANDARD/DEBUG/DRY_RUN 三种运行模式
- 前端统一通过 `apiClient` 访问 `/api`，在请求拦截器内注入 JWT
- 工作流前端采用 SSE 流式消费执行事件，和执行日志面板联动展示

## 蓝图文件索引

```
.blueprint/
├── _overview.md                              ← 本文件
├── domain/
│   ├── workflow/
│   │   ├── WorkflowEngine.md                 # Execution聚合根, WorkflowGraph, Node, Edge, 状态机
│   │   ├── ExecutionContext.md                # 智能黑板, LTM/STM/Awareness, SpEL解析
│   │   ├── NodeExecutor.md                   # 策略接口, StreamPublisher端口, NodeExecutionResult
│   │   └── HumanReview.md                    # 审核记录, 触发阶段, 审核队列
│   ├── agent/AgentService.md
│   ├── chat/ChatService.md
│   ├── knowledge/KnowledgeService.md
│   └── auth/AuthService.md
├── application/
│   ├── SchedulerService.md                   # 工作流编排核心, 记忆水合, 消息管理
│   ├── AgentApplicationService.md
│   └── KnowledgeApplicationService.md
├── infrastructure/
│   ├── adapters/
│   │   ├── NodeExecutors.md                  # 六种执行器实现, 流式推送, 持久化
│   │   └── ExternalServices.md
│   └── persistence/
│       ├── MySQLRepositories.md
│       └── RedisRepositories.md
├── frontend/
│   ├── _overview.md                          # 前端蓝图总览（Workflow Dify 模式）
│   ├── workflow/
│   │   ├── TargetStructure.md                # workflow 目标目录与核心文件职责
│   │   ├── HooksAndStoreSlices.md            # hooks 拆分与 store slices 状态边界
│   │   ├── LayeredModules.md                 # nodes/panel/operator/features/utils 分层
│   │   ├── LegacyMigrationMap.md             # 旧文件删除映射（旧路径 -> 新模块）
│   │   └── MigrationCompletionMatrix.md      # 迁移完成矩阵（目标/状态/完成度/下一步）
│   └── pages/
│       └── WorkflowEditorPage.md             # WorkflowEditorPage 装配层契约
└── interfaces/
    └── Controllers.md
```


## 前端蓝图结构更新 (2026-02-13)

前端蓝图已切换到 workflow 域优先的 Dify 模式，并以 `workflow/*` + `pages/WorkflowEditorPage.md` 作为本轮重构唯一生效蓝图。

### 本轮关键蓝图
- `.blueprint/frontend/_overview.md`
- `.blueprint/frontend/workflow/TargetStructure.md`
- `.blueprint/frontend/workflow/HooksAndStoreSlices.md`
- `.blueprint/frontend/workflow/LayeredModules.md`
- `.blueprint/frontend/workflow/LegacyMigrationMap.md`
- `.blueprint/frontend/pages/WorkflowEditorPage.md`

### 执行约束
- 先蓝图定稿，再 Serena 校验差异，最后实施代码迁移。
- `WorkflowEditorPage` 仅装配，交互逻辑必须下沉 hooks，状态必须收敛到 workflow slices。
- 旧实现清理与去向统一以 `LegacyMigrationMap.md` 为准。
