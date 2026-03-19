# ai-agent-infrastructure 模块蓝图

## 模块职责
技术实现层，为 domain 层定义的端口（Ports）提供具体适配器（Adapters）。包含数据库持久化（MyBatis Plus）、缓存（Redisson）、向量存储（Milvus）、对象存储（MinIO）、工作流节点执行器、SSE 流式推送等全部基础设施实现。

## 子包结构

| 子包 | 职责 |
|------|------|
| `infrastructure/agent/` | Agent 持久化：AgentMapper、AgentPO、AgentRepositoryImpl |
| `infrastructure/auth/` | 认证实现：JwtTokenService、RedisSlidingWindowRateLimiter |
| `infrastructure/chat/` | 对话持久化：ConversationMapper、MessageMapper、MybatisConversationRepository |
| `infrastructure/config/` | 基础设施配置：MinIOConfig、RedisListenerConfig、WebSocketConfig、WorkflowConfig |
| `infrastructure/dashboard/` | 仪表盘持久化：DashboardMapper、DashboardRepositoryImpl |
| `infrastructure/email/` | 邮件服务：EmailServiceImpl |
| `infrastructure/knowledge/` | 知识库实现：MinIOFileStorageService、文档解析/分块适配器、MySQL 仓储 |
| `infrastructure/llm/` | LLM 模型配置持久化 |
| `infrastructure/memory/` | 向量存储：MilvusVectorStoreAdapter、MilvusVectorStoreConfig |
| `infrastructure/meta/` | 元数据持久化：NodeTemplateMapper、SysConfigFieldDefMapper |
| `infrastructure/redis/` | Redis 封装：IRedisService、RedissonService、RedisConfig |
| `infrastructure/swarm/` | Swarm 基础设施：LLM 调用、SSE 推送、工具执行、持久化 |
| `infrastructure/writing/` | 动态写作持久化：PO、Mapper、RepositoryImpl、聚合查询 |
| `infrastructure/user/` | 用户持久化：UserMapper、UserRepositoryImpl、RedisVerificationCodeRepository |
| `infrastructure/workflow/` | **工作流核心实现**（见下方详细说明） |

## workflow 子包详细

| 子包 | 职责 |
|------|------|
| `workflow/executor/` | 节点执行器策略（共 7 种）：Start/End/LLM/Condition/Http/Tool/Knowledge NodeExecutorStrategy，通过 NodeExecutorFactory 注册发现 |
| `workflow/graph/` | 图构建：WorkflowGraphFactoryImpl、NodeConfigConverter、JSON DTO |
| `workflow/condition/` | 条件求值：StructuredConditionEvaluator（EXPRESSION/LLM 双模式） |
| `workflow/expression/` | 表达式解析：ExpressionResolver（SpEL + 变量引用） |
| `workflow/stream/` | SSE 流：RedisSseStreamPublisher、RedisSseStreamPublisherFactory |
| `workflow/event/` | Redis Pub/Sub：RedisSseListener、RedisSsePublisher |
| `workflow/adapter/` | 端口适配：RedisHumanReviewQueueAdapter、RedisWorkflowCancellationAdapter |
| `workflow/repository/` | 持久化：RedisCheckpointRepository、RedisExecutionRepository |
| `workflow/persistence/` | 执行日志持久化：WorkflowNodeExecutionLogRepositoryImpl |
| `workflow/listener/` | 审计监听：WorkflowAuditListener |

## 当前基础设施约定

### Milvus / VectorStore
- `MilvusVectorStoreAdapter` 同时服务两类集合：
  - `agent_knowledge_base`：知识库检索
  - `agent_chat_memory`：长期记忆
- 知识库 metadata 标准键名已统一为下划线：
  - `dataset_id`
  - `document_id`
  - `agent_id`
  - `chunk_index`
- 为兼容历史数据，检索和删除 filter 会同时匹配下划线与驼峰字段名。
- 如果后续补充新 metadata 字段，优先继续使用下划线风格，否则会再次出现“写入成功但过滤不到”的问题。

### 工作流执行器
- `KnowledgeNodeExecutorStrategy` 已能正确拿到工作流解析后的 `query`，知识库“返回 0 条”时通常先查 VectorStore metadata/filter，而不是先怀疑 query 传值。
- `RedisHumanReviewQueueAdapter` 是待审核 executionId 的唯一正式访问入口；Interfaces 层不应再直接硬编码 Redis key。

### 动态写作持久化
- `infrastructure/writing/` 对应 `domain/writing` 的仓储实现，当前包含：
  - `WritingSession` 持久化
  - `WritingAgent` 持久化
  - `WritingTask` 持久化
  - `WritingResult` 持久化
  - `WritingDraft` 持久化
- 写作相关 PO 中存在 JSON 字段时，统一通过 `JacksonTypeHandler` 映射，避免把结构化扩展字段拆成过多表。
- 写作与 swarm 的数据库职责边界：
  - `swarm_*` 负责运行时 workspace / agent / message / tool_call
  - `writing_*` 负责写作业务数据沉淀与聚合查询
- 当前初始化脚本已按方案 A 重建 swarm + writing 结构，部署时优先以这两份脚本为准：
  - `docker/init/mysql/20260318_rebuild_swarm_writing_schema.sql`
  - `src/main/resources/docker/init/mysql/20260318_rebuild_swarm_writing_schema.sql`
- 本次 schema 修复重点之一是补齐 `swarm_workspace_agent` 需要的字段，避免再次出现查询 `description` 列时报错。

## 上下游依赖
- 上游：实现 `ai-agent-domain` 定义的端口接口
- 下游：被 Spring 容器自动注入到 application 和 interfaces 层
