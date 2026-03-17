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

## 上下游依赖
- 上游：实现 `ai-agent-domain` 定义的端口接口
- 下游：被 Spring 容器自动注入到 application 和 interfaces 层
