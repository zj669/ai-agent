# ai-agent-domain 模块蓝图

## 模块职责
核心业务逻辑层，包含实体（聚合根）、值对象、领域服务、仓储接口和端口定义。当前 domain 源码整体保持领域对象和端口定义为主，不应在新增领域逻辑里引入 Spring/MyBatis 等基础设施细节。

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
| **writing** | `domain/writing/` | WritingSession、WritingTask、WritingResult、WritingDraft，以及对应仓储接口 |
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
| `WritingSessionRepository` | writing/repository | 写作会话持久化，记录本次创作目标、上下文和状态 |
| `WritingTaskRepository` | writing/repository | 主 Agent 拆解出的写作任务持久化 |
| `WritingResultRepository` | writing/repository | 子 Agent 执行结果持久化 |
| `WritingDraftRepository` | writing/repository | 主 Agent 汇总后的阶段草稿 / 最终草稿持久化 |

## 当前写作协作模型约定

- `writing` 子域是业务数据层，`swarm` 子域是运行时协作层，两者分工不同：
  - `swarm` 负责 workspace、运行中的 agent、消息与工具调用
  - `writing` 负责沉淀写作过程数据，供聚合查询和前端协作面板展示
- 当前标准写作链路为：
  - `WritingSession` 记录本次创作任务
  - `swarm_workspace_agent.session_id/sort_order` 记录本轮协作涉及的 Agent 与展示排序
  - `WritingTask` 记录主 Agent 拆分出的任务
  - `WritingResult` 记录子 Agent 的阶段产出
  - `WritingDraft` 记录主 Agent 汇总后的草稿
- 当前代码和初始化 SQL 已没有 `WritingAgent` 领域实体/仓储和 `writing_agent` 表；历史文档中出现该概念时应按过期设计处理。
- 子 Agent 不应直接操作 `WritingSession` 和 `WritingDraft`，避免写作总控权从主 Agent 泄漏到执行节点。

## 上下游依赖
- 上游：领域模型应只依赖 `ai-agent-shared`
- 下游：被 application 层编排调用，被 infrastructure 层实现端口
