# API Documentation

> 当前文件是静态资源中的轻量 API 索引。详细接口说明以仓库 `docs/api/*.md` 为准；如果两者冲突，优先看 Controller 代码和 `docs/api`。

## 当前认证入口

用户认证接口实际路径为 `/client/user`。

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/client/user/email/sendCode` | 发送邮箱验证码 |
| `POST` | `/client/user/email/register` | 邮箱注册 |
| `POST` | `/client/user/login` | 登录 |
| `POST` | `/client/user/refresh` | 刷新 Token |
| `GET` | `/client/user/info` | 当前用户信息 |
| `POST` | `/client/user/profile` | 修改资料 |
| `POST` | `/client/user/logout` | 登出 |
| `POST` | `/client/user/resetPassword` | 重置密码 |

受保护接口使用：

```http
Authorization: Bearer {token}
```

## 当前 API 模块

| 模块 | 路径 | 说明 |
|------|------|------|
| Agent | `/api/agent` | Agent CRUD、发布、回滚、版本管理 |
| Chat | `/api/chat` | 会话创建、列表、消息历史、删除 |
| Workflow | `/api/workflow/execution` | 工作流启动、订阅、停止、暂停、详情、日志、上下文 |
| Review | `/api/workflow/reviews` | 待审核、详情、恢复、拒绝、历史 |
| Knowledge | `/api/knowledge` | 数据集、文档、重试、检索 |
| Metadata | `/api/meta` | 节点模板、节点类型 |
| Dashboard | `/api/dashboard` | 当前用户统计 |
| LLM Config | `/api/llm-config` | 模型配置 CRUD、默认配置、测试 |
| MCP | `/api/mcp` | MCP server 配置、连接和工具发现 |
| Swarm | `/api/swarm/*` | 多 Agent workspace、agent、group、message、graph、SSE |
| Writing | `/api/writing` | 写作会话和 overview 聚合查询 |

## Chat 与 Workflow 的当前关系

当前前端聊天页主链路不是直接让 `ChatController` 生成回答，而是：

```text
ChatPage
  -> modules/chat/api/chatService.ts
  -> POST /api/workflow/execution/start
  -> SchedulerService.startExecution(...)
  -> 写入 USER 消息
  -> 初始化 ASSISTANT PENDING 消息
  -> 执行 workflow
  -> 回写 ASSISTANT COMPLETED / FAILED / 暂停摘要
```

Workflow start 请求体常用字段：

```json
{
  "agentId": 1001,
  "userId": 1,
  "conversationId": "conv-uuid",
  "versionId": 2,
  "inputs": {
    "inputMessage": "你好"
  },
  "mode": "STANDARD"
}
```

`SchedulerService` 提取用户输入的优先级为：

```text
inputMessage -> input -> query -> message
```

## Workflow 核心接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/workflow/execution/start` | 启动执行并直接返回 SSE |
| `GET` | `/api/workflow/execution/{executionId}/stream` | 重新订阅已有执行的 SSE |
| `POST` | `/api/workflow/execution/stop` | 停止执行 |
| `POST` | `/api/workflow/execution/pause` | 手动暂停 |
| `GET` | `/api/workflow/execution/{executionId}` | 执行详情 |
| `GET` | `/api/workflow/execution/{executionId}/node/{nodeId}` | 节点执行详情 |
| `GET` | `/api/workflow/execution/{executionId}/logs` | 节点执行日志 |
| `GET` | `/api/workflow/execution/history/{conversationId}` | 会话执行历史 |
| `GET` | `/api/workflow/execution/{executionId}/context` | 执行上下文 |

`POST /start` 是 POST SSE，浏览器原生 `EventSource` 不能携带 POST body；当前前端使用 `fetch` 读取流。断线重连或原生 `EventSource` 场景使用 `GET /{executionId}/stream`，认证可走 `?token=...`。

## Review 核心接口

当前人工审核接口路径是 `/api/workflow/reviews`，不是 `/api/workflow/human-review`。

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/workflow/reviews/pending` | 待审核列表 |
| `GET` | `/api/workflow/reviews/{executionId}` | 审核详情 |
| `POST` | `/api/workflow/reviews/resume` | 审核通过并恢复 |
| `POST` | `/api/workflow/reviews/reject` | 审核拒绝并终止 |
| `GET` | `/api/workflow/reviews/history` | 审核历史 |

实现注意：

- Review 接口当前返回原始 DTO 或空 `200`，不走统一 `Response<T>`。
- `resume/reject` 请求支持 `expectedVersion`，用于乐观锁校验。

## Knowledge 核心接口

当前知识库路径使用单数资源段：

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/knowledge/dataset` | 创建知识库 |
| `GET` | `/api/knowledge/dataset/list` | 知识库列表 |
| `GET` | `/api/knowledge/dataset/{id}` | 知识库详情 |
| `DELETE` | `/api/knowledge/dataset/{id}` | 删除知识库 |
| `POST` | `/api/knowledge/document/upload` | 上传文档 |
| `GET` | `/api/knowledge/document/list` | 文档列表 |
| `GET` | `/api/knowledge/document/{id}` | 文档详情 |
| `DELETE` | `/api/knowledge/document/{id}` | 删除文档 |
| `POST` | `/api/knowledge/document/{id}/retry` | 重试失败文档 |
| `POST` | `/api/knowledge/search` | 数据集检索调试接口 |

工作流知识库节点支持 `SEMANTIC / KEYWORD / HYBRID` 三种策略。Milvus metadata 当前标准键名为 `dataset_id/document_id/agent_id/chunk_index/filename`。

## Agent 核心接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/agent/create` | 创建 Agent |
| `POST` | `/api/agent/update` | 更新 Agent 和 `graphJson` |
| `POST` | `/api/agent/publish` | 发布当前图 |
| `POST` | `/api/agent/rollback` | 回滚版本 |
| `POST` | `/api/agent/delete` | 逻辑删除 |
| `DELETE` | `/api/agent/{id}/versions/{version}` | 删除指定版本 |
| `DELETE` | `/api/agent/{id}/force` | 强制逻辑删除 |
| `GET` | `/api/agent/list` | Agent 列表 |
| `GET` | `/api/agent/{id}` | Agent 详情 |
| `GET` | `/api/agent/{id}/versions` | 版本列表 |

当前后端没有 `/api/agent/offline`；前端 `agentAdapter.offlineAgent()` 仍调用该路径，这是当前前后端偏移点。

## 节点类型

当前支持：

```text
START, END, LLM, HTTP, KNOWLEDGE, CONDITION, TOOL
```

## Swarm / Writing 当前结构

当前初始化 SQL 中没有 `writing_agent` 表。写作协作者关系由：

```text
swarm_workspace_agent.session_id
swarm_workspace_agent.sort_order
```

配合以下表组成：

```text
writing_session
writing_task
writing_result
writing_draft
```

## 详细文档入口

| 文档 | 路径 |
|------|------|
| API 总览 | `docs/api/backend-api-overview.md` |
| Agent | `docs/api/agent.md` |
| Chat | `docs/api/chat.md` |
| Workflow | `docs/api/workflow.md` |
| Review | `docs/api/review.md` |
| Knowledge | `docs/api/knowledge.md` |
| User/Auth | `docs/api/user-api.md` |
| LLM Config | `docs/api/llm-config.md` |
| MCP | `docs/api/mcp.md` |
| Swarm | `docs/api/swarm.md` |
| Writing | `docs/api/writing.md` |
