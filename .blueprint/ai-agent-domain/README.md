# ai-agent-domain 模块蓝图

## 模块职责
核心业务逻辑层，包含实体（聚合根）、值对象、领域服务、仓储接口和端口定义。严格保持纯净——不依赖 Spring、MyBatis 或任何框架，仅依赖 shared 模块。

## 领域边界

| 子域 | 包路径 | 核心概念 |
|------|--------|----------|
| **workflow** | `domain/workflow/` | Execution（聚合根）、WorkflowGraph、Node、Edge、ExecutionContext、条件分支/剪枝 |
| **agent** | `domain/agent/` | Agent（聚合根）、AgentVersion、AgentStatus、GraphValidator |
| **chat** | `domain/chat/` | Conversation、Message、MessageRole/Status、Citation、ThoughtStep |
| **knowledge** | `domain/knowledge/` | KnowledgeDataset、KnowledgeDocument、ChunkingConfig、DocumentStatus、KnowledgeRetrievalService（检索端口）、DocumentReaderPort、TextSplitterPort、FileStorageService |
| **user** | `domain/user/` | User、Email（值对象）、Credential（值对象）、UserAuthenticationDomainService |
| **auth** | `domain/auth/` | ITokenService、RateLimiter、RateLimiterFactory |
| **memory** | `domain/memory/` | VectorStore（端口，双集合：knowledgeStore + memoryStore）、Document、SearchRequest |
| **swarm** | `domain/swarm/` | Swarm 多智能体协作实体、仓储接口、领域服务 |
| **llm** | `domain/llm/` | LLM 模型配置实体、仓储接口 |
| **dashboard** | `domain/dashboard/` | DashboardStats（值对象）、DashboardRepository |

## 关键端口（Ports）

| 端口 | 位置 | 职责 |
|------|------|------|
| `ExecutionRepository` | workflow/port | 执行实例持久化 |
| `CheckpointRepository` | workflow/port | 断点续跑存储 |
| `StreamPublisher` | workflow/port | SSE 流式推送 |
| `NodeExecutorStrategy` | workflow/port | 节点执行策略接口 |
| `ConditionEvaluatorPort` | workflow/port | 条件表达式求值 |
| `HumanReviewQueuePort` | workflow/port | 人工审核队列 |
| `WorkflowCancellationPort` | workflow/port | 工作流取消信号 |
| `ConversationRepository` | chat/port | 对话持久化 |
| `VectorStore` | memory/port | 向量存储抽象（双集合：agent_knowledge_base 知识检索 + agent_chat_memory 长期记忆） |
| `FileStorageService` | knowledge/port | 文件存储抽象 |
| `KnowledgeRetrievalService` | knowledge/service | 知识检索接口（retrieve by agentId / by datasetId） |
| `DocumentReaderPort` | knowledge/port | 文档解析端口 |
| `TextSplitterPort` | knowledge/port | 文本分块端口 |

## 上下游依赖
- 上游：仅依赖 `ai-agent-shared`
- 下游：被 application 层编排调用，被 infrastructure 层实现端口
