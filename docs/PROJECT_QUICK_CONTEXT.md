# AI Agent Platform 快速上下文

## 文档目标

这份文档是给 AI 和新成员的当前代码入口。它只描述当前实现，不把历史 PRD、论文材料或 bugfix 记录当作当前事实。

## 一句话理解项目

这是一个可编排、可执行、可审核、可追溯的 AI Agent 平台。用户配置 Agent 的 `graphJson` 后，系统把它解析为工作流 DAG，通过不同节点执行器完成 LLM 调用、知识库检索、HTTP 请求、MCP 工具调用和条件分支，并在需要时进入人工审核队列。

## 当前实现边界

1. 后端是 Maven 多模块 Spring Boot 项目，Java 21，Spring Boot 3.4.9，Spring AI 1.0.1。
2. 前端是 `ai-agent-foward`，使用 Vite、React 19、Ant Design 6、React Router 7、Zustand。
3. 认证入口是 `/client/user`，业务 API 主要在 `/api/**`。
4. 前端聊天页当前直接调用 `/api/workflow/execution/start` 启动 workflow SSE，不主要依赖 `ChatController.sendMessage`。
5. Milvus 可通过配置启用；未启用时 `NoOpVectorStoreAdapter` 会让写入/检索返回空结果并记录日志。
6. 架构采用 DDD/Ports and Adapters 风格，但当前 Maven 依赖不是完全纯净分层：`ai-agent-application` 直接依赖 `ai-agent-infrastructure`。

## 已落地能力清单

| 能力 | 当前代码事实 |
|------|--------------|
| Agent | 创建、更新、发布、回滚、版本列表、版本删除、逻辑删除 |
| Workflow | DAG 调度、节点状态推进、条件剪枝、暂停/恢复/拒绝、SSE 事件、执行日志 |
| Node Executor | `START / END / LLM / HTTP / KNOWLEDGE / CONDITION / TOOL` |
| Chat | 会话 CRUD、消息历史；聊天页通过 workflow start 触发执行并由 `SchedulerService` 回写消息 |
| Review | 待审核列表、审核详情、恢复、拒绝、审核历史；返回原始 DTO/空 `200`，不走统一 `Response<T>` |
| Knowledge | 数据集、文档上传、异步解析分块、MinIO、Milvus/No-Op、语义/关键词/混合检索 |
| User/Auth | 邮箱验证码、邮箱注册、登录、刷新、个人信息、登出、重置密码、JWT 拦截 |
| LLM Config | 用户级模型配置 CRUD、默认配置、连通性测试、DTO 不返回 API Key |
| MCP | Server CRUD、连接/断开、状态、工具发现、TOOL 节点调用 |
| Dashboard | 当前用户资源与执行统计，带缓存 |
| Swarm | Workspace、Agent、Group、Message、Graph/Search、Agent/UI SSE、运行时停止 |
| Writing | `writing_session/task/result/draft` 与 `swarm_workspace_agent.session_id/sort_order` 组成写作协作投影 |

## 建议加载顺序

### 第 0 层：全局入口

1. [README.md](../README.md)
2. [本文件](./PROJECT_QUICK_CONTEXT.md)
3. [.blueprint/README.md](../.blueprint/README.md)

### 第 1 层：接口与模块边界

1. [后端 API 总览](./api/backend-api-overview.md)
2. [Agent API](./api/agent.md)
3. [Workflow API](./api/workflow.md)
4. [Review API](./api/review.md)
5. [Chat API](./api/chat.md)
6. [Knowledge API](./api/knowledge.md)
7. [User/Auth API](./api/user-api.md)

### 第 2 层：扩展能力

1. [LLM Config API](./api/llm-config.md)
2. [MCP API](./api/mcp.md)
3. [Swarm API](./api/swarm.md)
4. [Writing API](./api/writing.md)
5. [Dashboard API](./api/dashboard.md)
6. [Metadata API](./api/meta.md)

### 第 3 层：代码与数据结构

1. 启动入口：`ai-agent-interfaces/src/main/java/com/zj/aiagent/AiAgentApplication.java`
2. 控制器：`ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/**`
3. 应用服务：`ai-agent-application/src/main/java/com/zj/aiagent/application/**`
4. 领域实体：`ai-agent-domain/src/main/java/com/zj/aiagent/domain/**`
5. 初始化 SQL：`docker/init/mysql/01_init_schema.sql`

## 快速定位

| 问题 | 优先入口 |
|------|----------|
| 项目当前做什么 | `README.md` + 本文件 |
| 有哪些 HTTP 接口 | `docs/api/backend-api-overview.md` |
| Agent graphJson 怎么保存/发布 | `docs/api/agent.md` + `AgentApplicationService` |
| 工作流怎么执行 | `docs/api/workflow.md` + `SchedulerService` + `Execution` |
| 人工审核怎么接入 | `docs/api/review.md` + `HumanReviewController` |
| Chat 为什么走 workflow | `docs/api/chat.md` + `modules/chat/api/chatService.ts` |
| 知识库为什么检索为空 | `docs/api/knowledge.md` + `AsyncDocumentProcessor` + `MilvusVectorStoreAdapter` |
| MCP 工具怎么接入 | `docs/api/mcp.md` + `ToolNodeExecutorStrategy` |
| Swarm/Writing 当前结构 | `docs/api/swarm.md` + `docs/api/writing.md` |

## 当前已知偏移点

1. 前端 `agentAdapter.offlineAgent()` 调用 `/api/agent/offline`，后端 `AgentController` 当前没有该接口。
2. 部分历史文档仍提到 `writing_agent` 表；当前初始化 SQL 已标注该表删除，写作 Agent 关联迁移到 `swarm_workspace_agent.session_id/sort_order`。
3. 部分旧文档把用户 API 写为 `/api/user`；当前控制器实际为 `/client/user`。
4. 部分旧架构文档声称 Domain/Application 纯净分层；当前 Maven 依赖显示 application 直接依赖 infrastructure。
5. `application.yml` 中 `spring.application.name` / `spring.config.name` 当前为 `aiagemt`，疑似历史拼写遗留；文档不应写成已确认的 `ai-agent` 配置名。

## 处理文档时的判断规则

1. 当前实现以代码、控制器、DTO、初始化 SQL 和前端 API adapter 为准。
2. `docs/bugfix-log/**`、历史 PRD、探索报告优先保留时间线，不直接改成当前态。
3. 面向开发入口的文档需要主动标明历史文档可能过期，避免后续开发误读。
