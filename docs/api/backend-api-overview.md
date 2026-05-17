# 后端 API 总览

## 系统概述

AI Agent Platform 后端提供 Agent 管理、工作流执行、人工审核、Chat 会话、知识库、节点元数据、Dashboard、LLM 配置、MCP、Swarm 和 Writing overview 等能力。

当前代码采用 DDD/Ports and Adapters 风格组织，但 Maven 依赖并非完全纯净分层：`ai-agent-application` 当前直接依赖 `ai-agent-infrastructure`。做架构描述时应使用“分层风格”，不要写成严格隔离的理想 DDD。

## 认证机制

受保护接口通过 JWT Bearer Token 认证：

```http
Authorization: Bearer {token}
```

用户认证接口实际路径是 `/client/user`：

| 动作 | 当前接口 |
|------|----------|
| 发送邮箱验证码 | `POST /client/user/email/sendCode` |
| 邮箱注册 | `POST /client/user/email/register` |
| 登录 | `POST /client/user/login` |
| 刷新 Token | `POST /client/user/refresh` |
| 当前用户信息 | `GET /client/user/info` |
| 修改资料 | `POST /client/user/profile` |
| 登出 | `POST /client/user/logout` |
| 重置密码 | `POST /client/user/resetPassword` |

本地调试可通过配置启用 debug header。默认代码中的 header 名称为 `debug-user`，前端 Chat SSE 代码也有该调试头的兼容。

## 响应格式

大多数业务接口使用统一响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

例外：

1. `/api/workflow/execution/start` 返回 `text/event-stream`，不是 JSON。
2. `/api/workflow/execution/{executionId}/stream` 返回 `text/event-stream`。
3. `/api/workflow/reviews/*` 当前由 `HumanReviewController` 返回原始 DTO 或空 `200`，不走统一 `Response<T>` 包装。
4. `DELETE /api/chat/conversations/{conversationId}` 当前 Controller 返回 `void`。

## API 模块

| 模块 | 路径 | 说明 | 文档 |
|------|------|------|------|
| User/Auth | `/client/user` | 邮箱验证码、注册、登录、刷新、资料、登出、重置密码 | [user-api.md](./user-api.md) |
| Agent | `/api/agent` | Agent CRUD、发布、回滚、版本管理 | [agent.md](./agent.md) |
| Workflow | `/api/workflow/execution` | 工作流启动、订阅、停止、暂停、详情、日志、上下文 | [workflow.md](./workflow.md) |
| Review | `/api/workflow/reviews` | 待审核、详情、恢复、拒绝、历史 | [review.md](./review.md) |
| Chat | `/api/chat` | 会话创建、列表、消息历史、删除；发送消息接口存在但当前前端主要直接启动 Workflow | [chat.md](./chat.md) |
| Knowledge | `/api/knowledge` | 数据集、文档上传/处理/重试、检索 | [knowledge.md](./knowledge.md) |
| Metadata | `/api/meta` | 节点模板、节点类型 | [meta.md](./meta.md) |
| Dashboard | `/api/dashboard` | 用户维度统计 | [dashboard.md](./dashboard.md) |
| LLM Config | `/api/llm-config` | 模型配置 CRUD、默认配置、测试 | [llm-config.md](./llm-config.md) |
| MCP | `/api/mcp` | MCP server 配置、连接、工具发现 | [mcp.md](./mcp.md) |
| Swarm | `/api/swarm/*` | 多 Agent 工作空间、Agent、群组、消息、图谱、SSE | [swarm.md](./swarm.md) |
| Writing | `/api/writing` | 写作会话和协作 overview 聚合查询 | [writing.md](./writing.md) |

## 关键接口清单

### Agent

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/agent/create` | 创建 Agent |
| `POST` | `/api/agent/update` | 更新 Agent 与 `graphJson` |
| `POST` | `/api/agent/publish` | 发布当前图并生成版本快照 |
| `POST` | `/api/agent/rollback` | 回滚到指定版本 |
| `POST` | `/api/agent/delete` | 逻辑删除 |
| `DELETE` | `/api/agent/{id}/versions/{version}` | 删除指定历史版本 |
| `DELETE` | `/api/agent/{id}/force` | 强制逻辑删除 Agent 与版本 |
| `GET` | `/api/agent/list` | 当前用户 Agent 列表 |
| `GET` | `/api/agent/{id}` | Agent 详情 |
| `GET` | `/api/agent/{id}/versions` | 版本列表 |

当前后端没有 `/api/agent/offline`。如果文档或前端代码提到该接口，应标为偏移点。

### Workflow

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/workflow/execution/start` | 启动执行并直接返回 SSE |
| `GET` | `/api/workflow/execution/{executionId}/stream` | 重新订阅已有执行的 SSE |
| `POST` | `/api/workflow/execution/stop` | 停止执行 |
| `POST` | `/api/workflow/execution/pause` | 手动暂停 |
| `GET` | `/api/workflow/execution/{executionId}` | 执行详情 |
| `GET` | `/api/workflow/execution/{executionId}/node/{nodeId}` | 节点详情 |
| `GET` | `/api/workflow/execution/{executionId}/logs` | 节点执行日志 |
| `GET` | `/api/workflow/execution/history/{conversationId}` | 会话执行历史 |
| `GET` | `/api/workflow/execution/{executionId}/context` | 执行上下文 |

### 节点类型

当前代码和初始化 SQL 支持：

```text
START, END, LLM, HTTP, KNOWLEDGE, CONDITION, TOOL
```

## 技术栈

| 类别 | 当前代码 |
|------|----------|
| 后端 | Spring Boot 3.4.9, Java 21 |
| AI | Spring AI 1.0.1, OpenAI Chat/Embedding, Milvus VectorStore, Tika Reader |
| 数据库 | MySQL, MyBatis Plus |
| 缓存/队列 | Redis, Redisson |
| 对象存储 | MinIO |
| 实时通信 | SSE |
| 前端 | Vite, React 19, Ant Design 6, React Router 7, Zustand |

## 本地开发端口

| 服务 | 默认地址 |
|------|----------|
| 后端 | `http://localhost:8080` |
| 前端 | `http://localhost:5173` |
| MySQL | `localhost:13306` |
| Redis | `localhost:6379` |
| Milvus | `localhost:19530` |
| MinIO Console | `http://localhost:9001` |

## 快速调用示例

### 登录

```bash
curl -X POST http://localhost:8080/client/user/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"SecurePass123"}'
```

### 创建 Agent

```bash
curl -X POST http://localhost:8080/api/agent/create \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"name":"客服助手","description":"智能客服","icon":"robot"}'
```

### 更新 Agent 图

```bash
curl -X POST http://localhost:8080/api/agent/update \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"id":1,"name":"客服助手","description":"智能客服","graphJson":"{\"nodes\":[],\"edges\":[]}","version":1}'
```

### 启动工作流 SSE

```bash
curl -N -X POST http://localhost:8080/api/workflow/execution/start \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"agentId":1,"userId":1,"conversationId":"conv-1","inputs":{"inputMessage":"你好"},"mode":"STANDARD"}'
```

### 重新订阅执行流

```bash
curl -N "http://localhost:8080/api/workflow/execution/{executionId}/stream?token={token}"
```

## 相关入口

| 资源 | 路径 |
|------|------|
| Spring Boot 入口 | `ai-agent-interfaces/src/main/java/com/zj/aiagent/AiAgentApplication.java` |
| Controller | `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/**` |
| 工作流调度 | `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java` |
| Workflow 领域聚合 | `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java` |
| 初始化 SQL | `docker/init/mysql/01_init_schema.sql` |
| 前端 API adapter | `ai-agent-foward/src/shared/api/adapters/**` |
