# 人工审核模块 API 文档

## 概述

人工审核模块提供工作流执行过程中的人工干预能力，支持在节点执行前（BEFORE_EXECUTION）或执行后（AFTER_EXECUTION）暂停工作流，等待人工审核和修改。

**Base URL**: `/api/workflow/reviews`

## 认证

所有接口需要用户登录（通过 UserContext 获取当前用户）。

**请求头格式**:
```
Authorization: Bearer {token}
```

**错误响应**:
- `401 Unauthorized`: Token 无效或已过期
- `403 Forbidden`: 无权限访问该资源

---

## API 列表

### 1. 获取待审核列表

**接口**: `GET /api/workflow/reviews/pending`

**描述**: 分页查询当前待审核的工作流执行任务

**请求参数**:
| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| page | int | Query | 否 | 页码（从0开始），默认0 |
| size | int | Query | 否 | 每页大小，默认20 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "executionId": "exec-123",
        "nodeId": "node-456",
        "nodeName": "数据审核节点",
        "agentName": "Agent-789",
        "triggerPhase": "BEFORE_EXECUTION",
        "pausedAt": "2026-02-10T10:30:00",
        "userId": 1
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 5,
    "totalPages": 1
  }
}
```

**字段说明**:
- `executionId`: 工作流执行ID
- `nodeId`: 触发审核的节点ID
- `nodeName`: 节点名称
- `agentName`: 所属 Agent 名称
- `triggerPhase`: 触发阶段（BEFORE_EXECUTION/AFTER_EXECUTION）
- `pausedAt`: 暂停时间
- `userId`: 工作流所有者ID

---

### 2. 获取审核详情

**接口**: `GET /api/workflow/reviews/{executionId}`

**描述**: 获取指定工作流执行的审核详细信息，包括上下文数据和可编辑字段

**路径参数**:
| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| executionId | String | Path | 是 | 工作流执行ID |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "executionId": "exec-123",
    "nodeId": "node-456",
    "nodeName": "数据审核节点",
    "triggerPhase": "BEFORE_EXECUTION",
    "contextData": {
      "query": "用户输入的查询内容",
      "temperature": 0.7,
      "maxTokens": 2000
    },
    "config": {
      "prompt": "请审核以下参数是否合理",
      "editableFields": ["query", "temperature"]
    }
  }
}
```

**字段说明**:
- `contextData`: 上下文数据
  - BEFORE_EXECUTION: 节点输入参数
  - AFTER_EXECUTION: 节点输出结果
- `config.prompt`: 审核提示词
- `config.editableFields`: 可编辑的字段列表

**错误响应**:
- `400`: 执行不存在
- `409`: 执行不处于待审核状态

---

### 3. 提交审核（通过）

**接口**: `POST /api/workflow/reviews/resume`

**描述**: 审核通过并恢复工作流执行，可选择性修改输入/输出数据

**请求体**:
```json
{
  "executionId": "exec-123",
  "nodeId": "node-456",
  "edits": {
    "query": "修改后的查询内容",
    "temperature": 0.5
  },
  "comment": "审核通过，已调整温度参数"
}
```

**字段说明**:
| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| executionId | String | Body | 是 | 工作流执行ID |
| nodeId | String | Body | 是 | 节点ID（用于验证） |
| edits | Object | Body | 否 | 修改的数据 |
| comment | String | Body | 否 | 审核意见 |

**响应示例**:
```json
{
  "code": 200,
  "message": "Execution resumed successfully",
  "data": null
}
```

**错误响应**:
- `401`: 未登录
- `403`: 无权限审核（非工作流所有者且非审核员）
- `409`: 执行不处于待审核状态（幂等性保护）
- `500`: 恢复执行失败

---

### 4. 拒绝审核

**接口**: `POST /api/workflow/reviews/reject`

**描述**: 拒绝审核并终止工作流执行

**请求体**:
```json
{
  "executionId": "exec-123",
  "nodeId": "node-456",
  "reason": "输入参数不符合业务规则"
}
```

**字段说明**:
| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| executionId | String | Body | 是 | 工作流执行ID |
| nodeId | String | Body | 是 | 节点ID |
| reason | String | Body | 是 | 拒绝原因 |

**响应示例**:
```json
{
  "code": 200,
  "message": "Execution rejected successfully",
  "data": null
}
```

**错误响应**:
- `401`: 未登录
- `403`: 无权限审核
- `500`: 拒绝执行失败

---

### 5. 查询审核历史

**接口**: `GET /api/workflow/reviews/history`

**描述**: 分页查询审核历史记录

**请求参数**:
| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| userId | Long | Query | 否 | 审核人ID（不传则查询所有） |
| page | int | Query | 否 | 页码（从0开始），默认0 |
| size | int | Query | 否 | 每页大小，默认20 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "id": "1",
        "executionId": "exec-123",
        "nodeId": "node-456",
        "reviewerId": 1,
        "decision": "APPROVE",
        "triggerPhase": "BEFORE_EXECUTION",
        "originalData": "{\"query\":\"原始查询\"}",
        "modifiedData": "{\"query\":\"修改后查询\"}",
        "comment": "审核通过",
        "reviewedAt": "2026-02-10T10:35:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 10,
    "totalPages": 1
  }
}
```

**字段说明**:
- `decision`: 审核决策（APPROVE/REJECT）
- `originalData`: 原始数据快照（JSON字符串）
- `modifiedData`: 修改后的数据（JSON字符串）

---

## 权限控制

### 审核权限规则

1. **工作流所有者**: 可以审核自己创建的工作流（通过 `Execution.userId` 判断）
2. **审核员角色**: 具有全局审核权限（TODO: 需实现角色管理）
3. **当前实现**: 简化版本，所有登录用户都可以审核

### 权限验证流程

```
1. 检查用户是否登录（UserContext.getUserId()）
2. 查询 Execution 实体
3. 判断 userId == execution.getUserId()
4. 或查询用户角色表（未实现）
```

---

## 审核流程说明

### BEFORE_EXECUTION（审核输入）

```
1. 节点执行前触发暂停
2. 创建 HumanReviewRecord（originalData = 节点输入）
3. 执行状态变更为 PAUSED_FOR_REVIEW
4. 审核人修改输入参数
5. 提交审核 → 用修改后的输入执行节点
6. 继续工作流执行
```

### AFTER_EXECUTION（审核输出）

```
1. 节点执行完成后触发暂停
2. 创建 HumanReviewRecord（originalData = 节点输出）
3. 执行状态变更为 PAUSED_FOR_REVIEW
4. 审核人修改输出结果
5. 提交审核 → 用修改后的输出推进 DAG
6. 继续工作流执行
```

---

## 数据持久化

| 数据类型 | 存储位置 | 说明 |
|---------|---------|------|
| 待审核队列 | Redis Set (`human_review:pending`) | 临时存储，审核完成后移除 |
| 审核记录 | MySQL (`workflow_human_review_record`) | 永久存储，作为审计日志 |
| 执行检查点 | Redis (`execution:checkpoint:*`) | 临时存储，用于暂停/恢复 |

---

## 安全性设计

### 1. 幂等性保证

- 提交审核前检查执行状态是否为 `PAUSED_FOR_REVIEW`
- 重复提交返回 409 状态码，不会重复执行

### 2. 并发控制

- 使用 Redisson 分布式锁（`lock:exec:{executionId}`）
- 锁超时时间：30秒

### 3. 审计日志

- 所有审核操作记录到 `workflow_human_review_record` 表
- 包含原始数据、修改数据、审核人、审核时间

### 4. 权限验证

- 每次审核操作前验证用户权限
- 非授权用户返回 403 状态码

---

## 错误码说明

| HTTP 状态码 | 说明 | 可能原因 |
|------------|------|---------|
| 200 | OK | 成功 |
| 400 | Bad Request | 请求参数错误 |
| 401 | Unauthorized | Token 无效或已过期 |
| 403 | Forbidden | 无权限访问 |
| 409 | Conflict | 状态冲突（幂等性保护） |
| 500 | Internal Server Error | 服务器内部错误 |

---

## 待优化项

### 1. 审核员角色管理
- 扩展 `user_account` 表，增加 `role` 字段
- 实现基于角色的权限控制（RBAC）

### 2. 审核超时机制
- 配置审核超时时间（如24小时）
- 超时后自动拒绝或通知管理员

### 3. 审核任务分配
- 支持指定审核人
- 审核任务队列管理

### 4. 审核提醒
- WebSocket 实时通知
- 邮件/短信提醒

### 5. 批量审核
- 支持批量通过/拒绝
- 批量修改参数

---

## 使用示例

### 前端集成示例

```typescript
// 1. 获取待审核列表
const response = await fetch('/api/workflow/reviews/pending?page=0&size=20');
const pendingReviews = await response.json();

// 2. 获取审核详情
const detail = await fetch(`/api/workflow/reviews/${executionId}`);
const reviewDetail = await detail.json();

// 3. 提交审核
await fetch('/api/workflow/reviews/resume', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    executionId: 'exec-123',
    nodeId: 'node-456',
    edits: { query: '修改后的内容' },
    comment: '审核通过'
  })
});

// 4. 拒绝审核
await fetch('/api/workflow/reviews/reject', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    executionId: 'exec-123',
    nodeId: 'node-456',
    reason: '不符合规则'
  })
});
```

---

## 变更日志

- **2026-02-10**: 初始版本，包含基础审核功能和权限控制
