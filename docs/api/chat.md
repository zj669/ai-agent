# Chat API 文档

## 概述

Chat 模块负责会话管理和消息历史查询。当前代码中，Chat 与 Workflow 的关系需要特别区分：

1. `ChatController` 提供会话 CRUD、消息历史查询，并保留一个 `POST /api/chat/conversations/{conversationId}/messages` SSE 发送接口。
2. 当前前端聊天页实际主要通过 `fetch("/api/workflow/execution/start")` 直接启动工作流，再由 `SchedulerService` 写入用户消息、初始化 assistant 消息并在执行完成/暂停/失败时回写结果。

因此，写当前主链路文档时，应描述为“Chat 触发 Workflow 执行并由 Scheduler 回写消息”，而不是“ChatController 自己生成回答”。

## 基础信息

- Base URL: `/api/chat`
- 认证：需要 `Authorization: Bearer {token}`
- 当前 Controller: `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/ChatController.java`
- 当前返回风格：除 SSE/DELETE 外，使用统一 `Response<T>`

## 接口列表

### 1. 创建会话

```http
POST /api/chat/conversations?agentId={agentId}
```

当前 `userId` 从 `UserContext` 读取，不需要也不接收 `userId` query 参数。

**Query 参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `agentId` | `String` | 是 | Agent ID |

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": "550e8400-e29b-41d4-a716-446655440000"
}
```

### 2. 获取会话列表

```http
GET /api/chat/conversations?agentId={agentId}&page=1&size=20
```

**Query 参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `agentId` | `String` | 是 | Agent ID |
| `page` | `int` | 否 | 页码，从 1 开始，默认 1 |
| `size` | `int` | 否 | 每页数量，默认 20 |

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "pages": 5,
    "list": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "userId": "1",
        "agentId": "1001",
        "title": "New Chat 05-14 10:30",
        "createdAt": "2026-05-14T10:30:00",
        "updatedAt": "2026-05-14T10:45:00"
      }
    ]
  }
}
```

### 3. 获取会话消息

```http
GET /api/chat/conversations/{conversationId}/messages?page=1&size=50&order=asc
```

当前 `userId` 从 `UserContext` 读取。后端会校验当前用户是否有权访问该会话。

**Path 参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `conversationId` | `String` | 是 | 会话 ID |

**Query 参数**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `page` | `int` | 否 | 页码，从 1 开始，默认 1 |
| `size` | `int` | 否 | 每页数量，默认 50 |
| `order` | `String` | 否 | `asc` 或 `desc`，默认 `asc` |

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "msg-001",
      "conversationId": "550e8400-e29b-41d4-a716-446655440000",
      "role": "USER",
      "content": "你好",
      "thoughtProcess": null,
      "citations": null,
      "metadata": {},
      "status": "COMPLETED",
      "createdAt": "2026-05-14T10:30:00"
    }
  ]
}
```

### 4. 发送消息 SSE（接口存在，但不是当前前端主路径）

```http
POST /api/chat/conversations/{conversationId}/messages
Content-Type: application/json
Accept: text/event-stream
```

**请求体**

```json
{
  "content": "你好"
}
```

实现语义：

1. 根据 `conversationId` 查询会话和关联 `agentId`。
2. 校验当前用户对会话的访问权限。
3. 校验 Agent 有已发布版本。
4. 创建 `executionId` 和 SSE emitter。
5. 调用 `SchedulerService.startExecution(...)`，输入当前为 `Map.of("input", request.getContent())`。

当前前端聊天页没有优先使用这个接口，而是直接调用 Workflow start，并传入 `inputs.inputMessage`。如果调试“前端聊天”问题，应优先看 `ai-agent-foward/src/modules/chat/api/chatService.ts`。

### 5. 删除会话

```http
DELETE /api/chat/conversations/{conversationId}
```

当前 `userId` 从 `UserContext` 读取。Controller 返回 `void`，成功时 HTTP 200，无统一 `Response<T>` 包装。

## 当前前端主链路

前端聊天页发送消息的主路径：

```text
ChatPage
  -> modules/chat/api/chatService.ts
  -> POST /api/workflow/execution/start
  -> SchedulerService.startExecution(...)
  -> ConversationRepository / MessageRepository
  -> SSE 事件回到前端
```

请求体要点：

```json
{
  "agentId": 1001,
  "userId": 1,
  "conversationId": "conv-1",
  "versionId": 2,
  "inputs": {
    "inputMessage": "你好"
  },
  "mode": "STANDARD"
}
```

`SchedulerService.extractUserQuery(...)` 当前会按优先级读取：

```text
inputMessage -> input -> query -> message
```

## Message 状态

`MessageStatus` 当前定义：

```text
PENDING, STREAMING, COMPLETED, FAILED
```

当前常见落库语义：

1. 用户消息通常直接写为 `COMPLETED`。
2. assistant 消息在工作流启动时初始化为 `PENDING`。
3. 执行成功后 assistant 消息更新为 `COMPLETED`。
4. 执行失败或审核拒绝后更新为 `FAILED`。
5. 暂停审核时会写入暂停摘要，具体状态取决于调度器分支。

## 示例

### 创建会话

```bash
curl -X POST "http://localhost:8080/api/chat/conversations?agentId=1001" \
  -H "Authorization: Bearer {token}"
```

### 查询消息

```bash
curl "http://localhost:8080/api/chat/conversations/{conversationId}/messages?order=asc&page=1&size=50" \
  -H "Authorization: Bearer {token}"
```

### 前端主链路启动 Workflow

```bash
curl -N -X POST http://localhost:8080/api/workflow/execution/start \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"agentId":1001,"userId":1,"conversationId":"conv-1","inputs":{"inputMessage":"你好"},"mode":"STANDARD"}'
```

## 更新记录

- 2026-05-14：对齐当前代码：移除 `userId` query 参数口径，补充前端直接启动 Workflow 的主链路，标明 ChatController 发送接口不是当前前端主路径。
