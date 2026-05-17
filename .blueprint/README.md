# AI Agent Platform 蓝图

## 项目概述

- 类型：AI Agent 编排与执行平台
- 后端：Spring Boot 3.4.9 + Java 21 + Spring AI 1.0.1
- 前端：Vite + React 19 + TypeScript + Ant Design 6
- 核心职责：Agent 管理、工作流 DAG 执行、人工审核、Chat 回写、知识库检索、LLM 配置、MCP 工具、多 Agent 协作与 Writing overview
- 架构风格：DDD/Ports and Adapters 风格；当前 Maven 依赖不是严格纯净分层，`ai-agent-application` 直接依赖 `ai-agent-infrastructure`

## 模块地图

| 模块 | 路径 | 职责 |
|------|------|------|
| `ai-agent-shared` | `ai-agent-shared/` | 公共响应、上下文、常量、工具类 |
| `ai-agent-domain` | `ai-agent-domain/` | 领域实体、值对象、领域服务、仓储接口和端口 |
| `ai-agent-application` | `ai-agent-application/` | 用例编排、DTO、应用服务、跨域协调 |
| `ai-agent-infrastructure` | `ai-agent-infrastructure/` | MyBatis、Redis、Milvus、MinIO、Spring AI、MCP、节点执行器、SSE 等适配 |
| `ai-agent-interfaces` | `ai-agent-interfaces/` | Spring Boot 入口、REST Controller、认证拦截器、配置、异常处理 |
| `ai-agent-foward` | `ai-agent-foward/` | React 前端模块、API adapter、路由、状态和工作流画布 |

## 当前依赖关系

理想风格：

```text
interfaces -> application -> domain <- infrastructure
```

当前代码事实：

```text
interfaces -> application -> domain -> shared
interfaces -> infrastructure
application -> infrastructure
```

因此后续文档不能写成“Application 层完全不依赖 Infrastructure”。如果要继续收敛架构，需要先调整 Maven 依赖和应用服务中的具体基础设施引用。

## 核心模块

1. `domain/workflow`：`Execution` 聚合根、DAG 状态推进、条件剪枝、暂停/恢复/拒绝。
2. `application/workflow/SchedulerService.java`：执行启动、记忆水合、节点调度、人工审核、消息回写。
3. `infrastructure/workflow/executor`：`START / END / LLM / HTTP / KNOWLEDGE / CONDITION / TOOL` 执行器策略。
4. `domain/agent` + `application/agent`：Agent 草稿、发布版本、回滚与 `graphJson`。
5. `domain/knowledge` + `application/knowledge`：数据集、文档、分块、向量化、检索。
6. `application/mcp` + `interfaces/mcp`：MCP server 配置、连接、工具发现。
7. `domain/swarm` + `application/swarm`：多 Agent 工作空间、消息、运行时和 SSE。
8. `domain/writing` + `application/writing`：写作 session/task/result/draft 和 overview 聚合。

## 当前关键链路

### 1. Workflow + Review

- 启动入口：`POST /api/workflow/execution/start`
- 重连入口：`GET /api/workflow/execution/{executionId}/stream`
- 暂停入口：`SchedulerService.checkPause(...)`
- 恢复入口：`SchedulerService.resumeExecution(...)`
- 拒绝入口：`SchedulerService.rejectExecution(...)`
- 审核 HTTP：`/api/workflow/reviews`

实现要点：

1. 运行图选择顺序为请求 `versionId` -> Agent `publishedVersionId` -> Agent 当前 `graphJson` 草稿。
2. 审核接口当前返回原始 DTO/空 `200`，不走统一 `Response<T>`。
3. 审核详情展示“已成功上游节点 + 当前暂停节点”。
4. `__MANUAL_PAUSE__` 不作为节点级审核详情。

### 2. Chat + Workflow

- Chat 会话接口：`/api/chat`
- 当前前端聊天页主链路：`POST /api/workflow/execution/start`
- 消息回写：`SchedulerService` 负责用户消息、assistant 初始化、最终完成/失败/暂停摘要回写

判断 Chat 问题时，不要只看 `ChatController.sendMessage(...)`。前端实际调用在：

```text
ai-agent-foward/src/modules/chat/api/chatService.ts
```

### 3. Knowledge

- 上传入口：`POST /api/knowledge/document/upload`
- 异步处理：`AsyncDocumentProcessor`
- 工作流节点：`KnowledgeNodeExecutorStrategy`
- 检索服务：`KnowledgeRetrievalServiceImpl`
- 向量适配：`MilvusVectorStoreAdapter` 或 `NoOpVectorStoreAdapter`

当前标准 metadata 键名：

```text
dataset_id, document_id, agent_id, chunk_index, filename
```

### 4. MCP + TOOL Node

- MCP API：`/api/mcp`
- 工具名约定：`mcp__{serverId}__{toolName}`
- TOOL 节点执行：`ToolNodeExecutorStrategy`
- 工具输出字段：`tool_response`、`result`

### 5. Swarm + Writing

当前 Swarm 运行时与 Writing 投影的边界：

| 层 | 当前职责 |
|----|----------|
| Swarm | workspace、agent、group、message、runtime、SSE、stop |
| Writing | session、task、result、draft、overview 聚合 |

当前表结构没有 `writing_agent` 表。协作者与写作会话的关系在 `swarm_workspace_agent.session_id/sort_order` 上。

创建 workspace 当前会创建：

1. `swarm_workspace`
2. Coordinator `swarm_workspace_agent`
3. P2P group
4. `writing_session`
5. Coordinator 的 `sessionId/sortOrder`

## 热点文件

| 主题 | 文件 |
|------|------|
| 启动入口 | `ai-agent-interfaces/src/main/java/com/zj/aiagent/AiAgentApplication.java` |
| 认证拦截 | `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/interceptor/LoginInterceptor.java` |
| Web 配置 | `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/config/WebMvcConfig.java` |
| Agent | `ai-agent-application/src/main/java/com/zj/aiagent/application/agent/service/AgentApplicationService.java` |
| Workflow 调度 | `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java` |
| Workflow 领域 | `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java` |
| Review | `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/HumanReviewController.java` |
| Knowledge 异步处理 | `ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/AsyncDocumentProcessor.java` |
| LLM 节点 | `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/LlmNodeExecutorStrategy.java` |
| MCP TOOL 节点 | `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/ToolNodeExecutorStrategy.java` |
| Swarm runtime | `ai-agent-application/src/main/java/com/zj/aiagent/application/swarm/SwarmAgentRuntimeService.java` |
| Swarm tools | `ai-agent-application/src/main/java/com/zj/aiagent/application/swarm/runtime/SwarmTools.java` |
| Writing overview | `ai-agent-application/src/main/java/com/zj/aiagent/application/writing/WritingProjectionService.java` |
| 前端路由 | `ai-agent-foward/src/app/router.tsx` |
| 前端 Chat SSE | `ai-agent-foward/src/modules/chat/api/chatService.ts` |
| 初始化 SQL | `docker/init/mysql/01_init_schema.sql` |

## 文档使用规则

1. 以当前代码、Controller、DTO、前端 adapter 和初始化 SQL 为准。
2. 历史 PRD、探索文档、bugfix-log 保留时间线，不默认代表当前实现。
3. 若文档与代码冲突，先修面向开发入口的文档：`README.md`、`docs/PROJECT_QUICK_CONTEXT.md`、`docs/api/**`、`.blueprint/**`。
