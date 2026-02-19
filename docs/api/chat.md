# Chat API 文档

## 概述

聊天对话模块提供会话管理和消息存储功能，支持用户与 AI Agent 的对话历史记录。

**Base URL**: `/api/chat`

## 认证

所有接口需要提供 `userId` 参数进行权限校验。

**请求头格式**:
```
Authorization: Bearer {token}
```

**错误响应**:
- `401 Unauthorized`: Token 无效或已过期
- `403 Forbidden`: 无权限访问该资源

---

## 接口列表

### 1. 创建会话

创建一个新的对话会话。

**请求**

```http
POST /api/chat/conversations
```

**Query Parameters**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| userId | String | Query | 是 | 用户ID |
| agentId | String | Query | 是 | 智能体ID |

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": "550e8400-e29b-41d4-a716-446655440000"
}
```

返回新创建的会话ID（UUID格式）。

**示例**

```bash
curl -X POST "http://localhost:8080/api/chat/conversations?userId=user123&agentId=agent456"
```

---

### 2. 获取会话列表

获取用户与指定 Agent 的会话历史列表，按更新时间倒序排列。

**请求**

```http
GET /api/chat/conversations
```

**Query Parameters**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| userId | String | Query | 是 | 用户ID |
| agentId | String | Query | 是 | 智能体ID |
| page | Integer | Query | 否 | 页码（从1开始），默认1 |
| size | Integer | Query | 否 | 每页数量，默认20 |

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
        "userId": "user123",
        "agentId": "agent456",
        "title": "New Chat 02-10 14:30",
        "createdAt": "2026-02-10T14:30:00",
        "updatedAt": "2026-02-10T15:45:00"
      }
    ]
  }
}
```

**字段说明**

- `total`: 总记录数
- `pages`: 总页数
- `list`: 会话列表
  - `id`: 会话ID
  - `userId`: 用户ID
  - `agentId`: 智能体ID
  - `title`: 会话标题
  - `createdAt`: 创建时间
  - `updatedAt`: 最后更新时间

**示例**

```bash
curl "http://localhost:8080/api/chat/conversations?userId=user123&agentId=agent456&page=1&size=20"
```

---

### 3. 获取会话消息历史

获取指定会话的消息列表，支持正序/倒序排列。

**请求**

```http
GET /api/chat/conversations/{conversationId}/messages
```

**Path Parameters**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| conversationId | String | Path | 是 | 会话ID |

**Query Parameters**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| userId | String | Query | 是 | 用户ID（用于权限校验） |
| page | Integer | Query | 否 | 页码（从1开始），默认1 |
| size | Integer | Query | 否 | 每页数量，默认50 |
| order | String | Query | 否 | 排序方式：`asc`（正序，旧→新）或 `desc`（倒序，新→旧），默认asc |

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
      "content": "你好，请帮我分析这段代码",
      "thoughtProcess": null,
      "citations": null,
      "metadata": {},
      "status": "COMPLETED",
      "createdAt": "2026-02-10T14:30:00"
    },
    {
      "id": "msg-002",
      "conversationId": "550e8400-e29b-41d4-a716-446655440000",
      "role": "ASSISTANT",
      "content": "好的，我来帮你分析...",
      "thoughtProcess": [
        {
          "nodeId": "node-1",
          "nodeName": "代码分析",
          "content": "正在分析代码结构...",
          "timestamp": 1707564600000
        }
      ],
      "citations": [
        {
          "source": "知识库文档",
          "url": "https://example.com/doc",
          "snippet": "相关代码片段..."
        }
      ],
      "metadata": {
        "runId": "run-123",
        "tokenCount": 1500
      },
      "status": "COMPLETED",
      "createdAt": "2026-02-10T14:31:00"
    }
  ]
}
```

**字段说明**

- `id`: 消息ID
- `conversationId`: 所属会话ID
- `role`: 消息角色
  - `USER`: 用户消息
  - `ASSISTANT`: AI助手消息
  - `SYSTEM`: 系统消息
- `content`: 消息内容
- `thoughtProcess`: 思维链过程（仅 ASSISTANT 消息）
  - `nodeId`: 节点ID
  - `nodeName`: 节点名称
  - `content`: 思考内容
  - `timestamp`: 时间戳
- `citations`: 引用源列表（仅 ASSISTANT 消息）
  - `source`: 来源名称
  - `url`: 来源链接
  - `snippet`: 引用片段
- `metadata`: 元数据
  - `runId`: 工作流执行ID
  - `tokenCount`: Token 消耗数量
- `status`: 消息状态
  - `PENDING`: 等待生成
  - `STREAMING`: 流式生成中
  - `COMPLETED`: 已完成
  - `FAILED`: 生成失败
- `createdAt`: 创建时间

**权限校验**

- 如果 `userId` 与会话所有者不匹配，返回 `400 Bad Request`
- 错误信息：`"No permission to access conversation: {conversationId}"`

**示例**

```bash
# 获取最新50条消息（倒序）
curl "http://localhost:8080/api/chat/conversations/550e8400-e29b-41d4-a716-446655440000/messages?userId=user123&order=desc&size=50"

# 获取历史消息（正序，用于加载更多）
curl "http://localhost:8080/api/chat/conversations/550e8400-e29b-41d4-a716-446655440000/messages?userId=user123&order=asc&page=2&size=20"
```

---

### 4. 删除会话

删除指定会话及其所有消息（软删除）。

**请求**

```http
DELETE /api/chat/conversations/{conversationId}
```

**Path Parameters**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| conversationId | String | Path | 是 | 会话ID |

**Query Parameters**

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| userId | String | Query | 是 | 用户ID（用于权限校验） |

**响应**

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

无响应体，成功时返回 HTTP 200。

**权限校验**

- 如果 `userId` 与会话所有者不匹配，返回 `400 Bad Request`
- 错误信息：`"No permission to delete conversation: {conversationId}"`

**示例**

```bash
curl -X DELETE "http://localhost:8080/api/chat/conversations/550e8400-e29b-41d4-a716-446655440000?userId=user123"
```

---

## 数据模型

### Conversation

```typescript
interface Conversation {
  id: string;              // 会话ID (UUID)
  userId: string;          // 用户ID
  agentId: string;         // 智能体ID
  title: string;           // 会话标题
  createdAt: string;       // 创建时间 (ISO 8601)
  updatedAt: string;       // 更新时间 (ISO 8601)
}
```

### Message

```typescript
interface Message {
  id: string;                      // 消息ID (UUID)
  conversationId: string;          // 所属会话ID
  role: 'USER' | 'ASSISTANT' | 'SYSTEM';  // 消息角色
  content: string;                 // 消息内容
  thoughtProcess?: ThoughtStep[];  // 思维链过程
  citations?: Citation[];          // 引用源列表
  metadata: Record<string, any>;   // 元数据
  status: 'PENDING' | 'STREAMING' | 'COMPLETED' | 'FAILED';  // 消息状态
  createdAt: string;               // 创建时间 (ISO 8601)
}

interface ThoughtStep {
  nodeId: string;          // 节点ID
  nodeName: string;        // 节点名称
  content: string;         // 思考内容
  timestamp: number;       // 时间戳
}

interface Citation {
  source: string;          // 来源名称
  url?: string;            // 来源链接
  snippet?: string;        // 引用片段
}
```

---

## 错误处理

### 错误响应格式

```json
{
  "code": 400,
  "message": "No permission to access conversation: 550e8400-e29b-41d4-a716-446655440000",
  "data": null,
  "timestamp": "2026-02-10T14:30:00"
}
```

### 常见错误码

| HTTP 状态码 | 说明 | 可能原因 |
|------------|------|---------|
| 400 | Bad Request | 参数错误、权限不足 |
| 401 | Unauthorized | Token 无效或已过期 |
| 404 | Not Found | 会话或消息不存在 |
| 500 | Internal Server Error | 服务器内部错误 |

---

## 使用场景

### 场景1：创建新对话

```javascript
// 1. 创建会话
const conversationId = await fetch('/api/chat/conversations?userId=user123&agentId=agent456', {
  method: 'POST'
}).then(res => res.text());

// 2. 启动工作流执行（发送消息）
const eventSource = new EventSource(`/api/workflow/execution/start`, {
  method: 'POST',
  body: JSON.stringify({
    agentId: 'agent456',
    userId: 'user123',
    conversationId: conversationId,
    inputs: { userMessage: '你好' }
  })
});

// 3. 监听 SSE 流式响应
eventSource.addEventListener('update', (event) => {
  const data = JSON.parse(event.data);
  console.log('Delta:', data.payload.delta);
});
```

### 场景2：加载历史消息

```javascript
// 加载最新50条消息（倒序显示）
const messages = await fetch(
  `/api/chat/conversations/${conversationId}/messages?userId=user123&order=desc&size=50`
).then(res => res.json());

// 滚动加载更多（正序）
const olderMessages = await fetch(
  `/api/chat/conversations/${conversationId}/messages?userId=user123&order=asc&page=2&size=20`
).then(res => res.json());
```

### 场景3：会话列表管理

```javascript
// 获取会话列表
const conversations = await fetch(
  `/api/chat/conversations?userId=user123&agentId=agent456&page=1&size=20`
).then(res => res.json());

// 删除会话
await fetch(
  `/api/chat/conversations/${conversationId}?userId=user123`,
  { method: 'DELETE' }
);
```

---

## 性能优化建议

### 前端优化

1. **虚拟滚动**: 消息列表使用虚拟滚动，避免渲染大量 DOM
2. **增量加载**: 使用分页加载历史消息，避免一次性加载全部
3. **本地缓存**: 使用 IndexedDB 缓存历史消息，减少网络请求

### 后端优化

1. **数据库索引**:
   - `idx_user_agent` (user_id, agent_id) - 会话列表查询
   - `idx_conversation_created` (conversation_id, created_at) - 消息历史查询
2. **分页查询**: 使用 MyBatis Plus 的 Page 对象，避免全表扫描
3. **Redis 缓存**: 高频查询的会话列表可考虑 Redis 缓存（当前未实现）

---

## 安全注意事项

1. **权限校验**: 所有接口必须验证 `userId` 是否与会话所有者匹配
2. **SQL 注入**: 使用 MyBatis Plus 的参数化查询，避免 SQL 注入
3. **XSS 防护**: 前端渲染消息内容时需要进行 HTML 转义
4. **敏感信息**: 不要在 `metadata` 中存储密码、Token 等敏感信息

---

## 未来扩展

1. **消息编辑**: 支持用户编辑已发送的消息
2. **消息搜索**: 支持全文搜索会话消息
3. **会话标签**: 支持为会话添加标签分类
4. **消息导出**: 支持导出会话历史为 Markdown/PDF
5. **实时协作**: 支持多用户实时查看同一会话（WebSocket）

---

## 变更日志

- **2026-02-10**: 初始版本，添加权限校验和排序支持
