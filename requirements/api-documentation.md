# AI Agent 后端 API 接口文档

> 生成时间: 2026-01-11

---

## 一、工作流执行模块 (WorkflowController)

**前缀**: `/api/workflow/execution`

### 1. 启动工作流执行 (SSE 流式)

- **Method**: `POST`
- **Path**: `/start`
- **Content-Type**: `application/json`
- **Produces**: `text/event-stream`

**请求体**:
```json
{
  "agentId": 1,
  "userId": 1,
  "conversationId": "conv-uuid",
  "graph": { /* WorkflowGraph对象 */ },
  "inputs": { "key": "value" }
}
```

**SSE 事件流**:

| 事件名 | 说明 | 数据格式 |
|--------|------|----------|
| `connected` | 连接成功 | `{ "executionId": "uuid" }` |
| `message` | 节点执行消息 | [SseEventPayload](#sseeventpayload) |
| `ping` | 心跳 (每15秒) | `"pong"` |
| `error` | 错误事件 | `{ "message": "error details" }` |

---

### 2. 停止执行

- **Method**: `POST`
- **Path**: `/stop`

**请求体**:
```json
{
  "executionId": "uuid"
}
```

**响应**: `200 OK` (无 Body)

---

### 3. 获取执行详情

- **Method**: `GET`
- **Path**: `/{executionId}`

**响应**:
```json
{
  "executionId": "uuid",
  "agentId": 1,
  "userId": 1,
  "conversationId": "conv-uuid",
  "status": "RUNNING",
  "nodeStatuses": { "node-1": "SUCCEEDED", "node-2": "RUNNING" },
  "createdAt": "2026-01-11T10:00:00",
  "updatedAt": "2026-01-11T10:05:00",
  "version": 3
}
```

---

### 4. 获取节点执行日志

- **Method**: `GET`
- **Path**: `/{executionId}/node/{nodeId}`

**响应**:
```json
{
  "id": 1,
  "executionId": "uuid",
  "nodeId": "node-1",
  "nodeName": "LLM节点",
  "nodeType": "LLM",
  "status": 1,
  "inputs": { "prompt": "hello" },
  "outputs": { "response": "world" },
  "errorMessage": null,
  "startTime": "2026-01-11T10:00:00",
  "endTime": "2026-01-11T10:00:05"
}
```

---

### 5. 获取会话执行历史

- **Method**: `GET`
- **Path**: `/history/{conversationId}`

**响应**:
```json
[
  {
    "executionId": "uuid-1",
    "status": "SUCCEEDED",
    "createdAt": "2026-01-11T10:00:00"
  },
  {
    "executionId": "uuid-2",
    "status": "FAILED",
    "createdAt": "2026-01-11T09:00:00"
  }
]
```

---

## 二、用户模块 (UserController)

**前缀**: `/client/user`

### 1. 发送邮箱验证码

- **Method**: `POST`
- **Path**: `/email/sendCode`

**请求体**:
```json
{
  "email": "user@example.com"
}
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

### 2. 邮箱注册

- **Method**: `POST`
- **Path**: `/email/register`

**请求体**:
```json
{
  "email": "user@example.com",
  "code": "123456",
  "password": "yourpassword",
  "nickname": "用户名"
}
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "jwt-token",
    "userId": 1,
    "nickname": "用户名"
  }
}
```

---

### 3. 用户登录

- **Method**: `POST`
- **Path**: `/login`

**请求体**:
```json
{
  "email": "user@example.com",
  "password": "yourpassword"
}
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "jwt-token",
    "userId": 1,
    "nickname": "用户名"
  }
}
```

---

### 4. 获取当前用户信息

- **Method**: `GET`
- **Path**: `/info`
- **Header**: `Authorization: Bearer <token>`

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "nickname": "用户名",
    "avatar": "url"
  }
}
```

---

### 5. 修改用户信息

- **Method**: `POST`
- **Path**: `/modify`
- **Header**: `Authorization: Bearer <token>`

**请求体**:
```json
{
  "nickname": "新昵称",
  "avatar": "新头像URL"
}
```

**响应**: 同"获取用户信息"

---

### 6. 用户登出

- **Method**: `POST`
- **Path**: `/logout`
- **Header**: `Authorization: Bearer <token>`

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

## 三、Agent 管理模块 (AgentController)

**前缀**: `/api/agent`

### 1. 创建 Agent

- **Method**: `POST`
- **Path**: `/create`

**请求体**:
```json
{
  "name": "我的Agent",
  "description": "描述",
  "icon": "icon-url"
}
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": 1  // Agent ID
}
```

---

### 2. 更新 Agent

- **Method**: `PUT`
- **Path**: `/update`

**请求体**:
```json
{
  "id": 1,
  "name": "更新名称",
  "description": "更新描述",
  "icon": "new-icon",
  "graphJson": "{ ... }",
  "version": 2
}
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

### 3. 发布 Agent

- **Method**: `POST`
- **Path**: `/publish`

**请求体**:
```json
{
  "id": 1
}
```

**响应**: `{ "code": 0, "message": "success", "data": null }`

---

### 4. 回滚 Agent

- **Method**: `POST`
- **Path**: `/rollback`

**请求体**:
```json
{
  "id": 1,
  "targetVersion": 2
}
```

**响应**: `{ "code": 0, "message": "success", "data": null }`

---

### 5. 删除 Agent

- **Method**: `POST`
- **Path**: `/delete/{id}`

**响应**: `{ "code": 0, "message": "success", "data": null }`

---

### 6. 获取 Agent 列表

- **Method**: `GET`
- **Path**: `/list`

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": [
    { "id": 1, "name": "Agent1", "icon": "url", "status": "DRAFT" },
    { "id": 2, "name": "Agent2", "icon": "url", "status": "PUBLISHED" }
  ]
}
```

---

### 7. 获取 Agent 详情

- **Method**: `GET`
- **Path**: `/{id}`

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "userId": 1,
    "name": "Agent名称",
    "description": "描述",
    "icon": "url",
    "graphJson": "{ ... }",
    "version": 1,
    "status": "DRAFT"
  }
}
```

---

### 8. 调试 Agent (SSE)

- **Method**: `POST`
- **Path**: `/debug`
- **Produces**: `text/event-stream`

**请求体**:
```json
{
  "agentId": 1,
  "inputMessage": "你好",
  "debugMode": true
}
```

**SSE 事件流**:

| 事件名 | 说明 | 数据格式 |
|--------|------|----------|
| `start` | 调试开始 | `"Debug Started"` |
| `log` | 调试日志 | 字符串 |
| 完成后 | 流关闭 | - |

---

## 四、元数据模块 (MetadataController)

**前缀**: `/api/meta`

### 1. 获取节点模板列表

- **Method**: `GET`
- **Path**: `/node-templates`

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "type": "LLM",
      "name": "大模型节点",
      "description": "调用LLM生成内容",
      "configSchema": { ... }
    },
    {
      "type": "HTTP",
      "name": "HTTP请求节点",
      "description": "发起HTTP请求",
      "configSchema": { ... }
    }
  ]
}
```

---

## 附录：数据结构

### SseEventPayload

SSE 消息事件负载：

```json
{
  "executionId": "uuid",
  "nodeId": "node-1",
  "nodeType": "LLM",
  "timestamp": 1736589600000,
  "isThought": false,
  "content": "生成的内容...",
  "renderConfig": {
    "mode": "MESSAGE",  // HIDDEN | THOUGHT | MESSAGE
    "title": "正在思考..."
  },
  "data": { /* 可选，额外调试数据 */ }
}
```

### 统一响应格式 (Response)

```json
{
  "code": 0,           // 0=成功, 非0=错误码
  "message": "success",
  "data": { ... }      // 业务数据
}
```

### ExecutionStatus 枚举

| 状态 | Code | 说明 |
|------|------|------|
| PENDING | 0 | 待执行 |
| RUNNING | 1 | 执行中 |
| SUCCEEDED | 2 | 成功 |
| FAILED | 3 | 失败 |
| SKIPPED | 4 | 跳过 |
| PAUSED | 5 | 暂停 |
| CANCELLED | 6 | 已取消 |
