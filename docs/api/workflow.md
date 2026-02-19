# 工作流执行 API 文档

## 概述

工作流执行 API 提供了启动、监控、控制工作流执行的能力。所有接口都位于 `/api/workflow/execution` 路径下。

## 认证

所有接口需要 JWT Token 认证（除特殊说明外）。

**请求头格式**:
```
Authorization: Bearer {token}
```

**错误响应**:
- `401 Unauthorized`: Token 无效或已过期
- `403 Forbidden`: 无权限访问该资源

## 核心特性

- **流式推送**：使用 SSE (Server-Sent Events) 实时推送执行进度
- **异步执行**：工作流在后台异步执行，不阻塞请求
- **状态管理**：支持暂停、恢复、取消等操作
- **记忆系统**：自动加载长期记忆(LTM)和短期记忆(STM)

## 接口列表

### 1. 启动工作流执行

启动一个新的工作流执行，并通过 SSE 流式返回执行进度。

**接口**：`POST /api/workflow/execution/start`

**Content-Type**：`application/json`

**Response-Type**：`text/event-stream` ⚠️ **SSE 流式接口**

**特殊说明**: 此接口返回类型为 `SseEmitter`（Server-Sent Events），非标准 JSON 响应。

**请求参数**：

```json
{
  "agentId": 1,
  "userId": 100,
  "conversationId": "conv_123",
  "versionId": 2,
  "inputs": {
    "query": "帮我分析一下这个数据",
    "context": "用户上下文信息"
  },
  "mode": "STANDARD"
}
```

**请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| agentId | Long | Body | 是 | Agent ID |
| userId | Long | Body | 是 | 用户 ID |
| conversationId | String | Body | 是 | 会话 ID |
| versionId | Integer | Body | 否 | 版本号，null 则使用已发布版本 |
| inputs | Map | Body | 是 | 输入参数 |
| mode | String | Body | 否 | 执行模式：STANDARD/DEBUG/DRY_RUN，默认 STANDARD |

**SSE 事件格式**：

**特殊说明**: 此接口返回类型为 `SseEmitter`（Server-Sent Events），非标准 JSON 响应。

```
event: connected
data: {"executionId":"exec_123"}

event: start
data: {"executionId":"exec_123","nodeId":"node_1","nodeType":"START","timestamp":1234567890}

event: update
data: {"executionId":"exec_123","nodeId":"node_2","delta":"正在分析...","timestamp":1234567891}

event: finish
data: {"executionId":"exec_123","nodeId":"node_2","status":"SUCCEEDED","timestamp":1234567892}

event: error
data: {"executionId":"exec_123","message":"执行失败：节点超时"}

event: ping
data: pong
```

**事件类型说明**：

| 事件 | 说明 |
|------|------|
| connected | 连接建立成功，返回 executionId |
| start | 节点开始执行 |
| update | 节点执行进度更新（流式输出） |
| finish | 节点执行完成 |
| error | 执行错误 |
| ping | 心跳事件（每 15 秒） |

**示例代码**：

```javascript
// 前端 JavaScript 示例
const eventSource = new EventSource('/api/workflow/execution/start', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    agentId: 1,
    userId: 100,
    conversationId: 'conv_123',
    inputs: { query: '你好' }
  })
});

eventSource.addEventListener('connected', (e) => {
  const data = JSON.parse(e.data);
  console.log('Execution ID:', data.executionId);
});

eventSource.addEventListener('update', (e) => {
  const data = JSON.parse(e.data);
  console.log('Progress:', data.delta);
});

eventSource.addEventListener('finish', (e) => {
  const data = JSON.parse(e.data);
  console.log('Completed:', data.status);
  eventSource.close();
});

eventSource.addEventListener('error', (e) => {
  const data = JSON.parse(e.data);
  console.error('Error:', data.message);
  eventSource.close();
});
```

---

### 2. 停止/取消执行

取消正在执行的工作流。

**接口**：`POST /api/workflow/execution/stop`

**请求参数**：

| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| executionId | String | Body | 是 | 工作流执行 ID |

**响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**错误码**：

| 状态码 | 说明 |
|--------|------|
| 200 | 取消成功 |
| 404 | 执行不存在 |
| 400 | 执行已完成，无法取消 |

---

### 3. 获取执行详情

获取工作流执行的详细信息。

**接口**：`GET /api/workflow/execution/{executionId}`

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "executionId": "exec_123",
    "agentId": 1,
    "userId": 100,
    "conversationId": "conv_123",
    "status": "SUCCEEDED",
    "startTime": "2026-02-10T10:00:00Z",
    "endTime": "2026-02-10T10:05:30Z",
    "nodeStatuses": {
      "node_1": "SUCCEEDED",
      "node_2": "SUCCEEDED",
      "node_3": "SUCCEEDED"
    }
  }
}
```

---

### 4. 获取节点执行日志

获取特定节点的执行日志。

**接口**：`GET /api/workflow/execution/{executionId}/node/{nodeId}`

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "executionId": "exec_123",
    "nodeId": "node_2",
    "nodeType": "LLM",
    "status": "SUCCEEDED",
    "startTime": "2026-02-10T10:01:00Z",
    "endTime": "2026-02-10T10:01:30Z",
    "input": "{\"query\":\"你好\"}",
    "output": "{\"response\":\"你好！我是AI助手。\"}",
    "errorMessage": null
  }
}
```

---

### 5. 获取执行思维链日志

获取整个执行的所有节点日志，按时间排序。

**接口**：`GET /api/workflow/execution/{executionId}/logs`

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "nodeId": "node_1",
      "nodeName": "开始",
      "nodeType": "START",
      "status": "SUCCEEDED",
      "startTime": "2026-02-10T10:00:00Z",
      "endTime": "2026-02-10T10:00:01Z"
    },
    {
      "nodeId": "node_2",
      "nodeName": "LLM 处理",
      "nodeType": "LLM",
      "status": "SUCCEEDED",
      "startTime": "2026-02-10T10:00:01Z",
      "endTime": "2026-02-10T10:00:30Z"
    }
  ]
}
```

---

### 6. 获取会话执行历史

获取某个会话的所有执行记录。

**接口**：`GET /api/workflow/execution/history/{conversationId}`

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "executionId": "exec_123",
      "status": "SUCCEEDED",
      "startTime": "2026-02-10T10:00:00Z",
      "endTime": "2026-02-10T10:05:30Z"
    },
    {
      "executionId": "exec_122",
      "status": "FAILED",
      "startTime": "2026-02-10T09:50:00Z",
      "endTime": "2026-02-10T09:51:00Z"
    }
  ]
}
```

---

### 7. 获取执行上下文快照

获取执行的上下文信息，包括 LTM、STM、执行日志和全局变量。

**接口**：`GET /api/workflow/execution/{executionId}/context`

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "executionId": "exec_123",
    "longTermMemories": [
      "用户偏好：喜欢简洁的回答",
      "历史经验：上次成功处理了类似问题"
    ],
    "chatHistory": [
      {
        "role": "USER",
        "content": "你好",
        "timestamp": 1234567890
      },
      {
        "role": "ASSISTANT",
        "content": "你好！有什么可以帮助你的？",
        "timestamp": 1234567891
      }
    ],
    "executionLog": "[node_1-开始]: 工作流启动\n[node_2-LLM处理]: 完成意图识别",
    "globalVariables": {
      "userIntent": "greeting",
      "confidence": 0.95
    }
  }
}
```

---

## 错误码

| 错误码 | HTTP 状态 | 说明 |
|--------|-----------|------|
| 200 | 200 | 成功 |
| 400 | 400 | 请求参数错误 |
| 404 | 404 | 资源不存在 |
| 500 | 500 | 服务器内部错误 |

**错误响应格式**：

```json
{
  "error": "INVALID_PARAMETER",
  "message": "agentId 不能为空",
  "timestamp": "2026-02-10T10:00:00Z"
}
```

---

## 业务规则

### 执行状态流转

```
PENDING → RUNNING → SUCCEEDED
                  → FAILED
                  → CANCELLED
                  → PAUSED → RUNNING
                  → PAUSED_FOR_REVIEW → RUNNING
```

### 节点状态流转

```
PENDING → RUNNING → SUCCEEDED
                  → FAILED
                  → SKIPPED (条件分支未选中)
```

### 记忆系统

- **长期记忆 (LTM)**：启动时从 Milvus 向量库检索，基于用户意图
- **短期记忆 (STM)**：启动时从 MySQL 加载会话历史
- **环境感知 (Awareness)**：动态更新的执行日志

### 执行模式

- **STANDARD**：标准模式，正常执行
- **DEBUG**：调试模式，记录详细日志
- **DRY_RUN**：干运行模式，不实际执行，仅验证流程

---

## 性能建议

1. **SSE 连接**：建议设置 30 分钟超时
2. **心跳检测**：每 15 秒发送一次 ping 事件
3. **并发限制**：建议单个 Agent 同时执行不超过 5 个工作流
4. **重试策略**：网络错误建议重试 3 次，业务错误不重试

---

## 更新日志

- **2026-02-10**：初始版本，基于架构重构后的代码
- **架构改进**：Domain 层完全纯净，使用端口适配器模式

---

## 相关文档

- [工作流引擎架构](.blueprint/domain/workflow/WorkflowEngine.md)
- [执行上下文设计](.blueprint/domain/workflow/ExecutionContext.md)
- [节点执行器实现](.blueprint/infrastructure/adapters/NodeExecutors.md)
