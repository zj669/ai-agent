# 人工审核模块 API 文档

## 概述

人工审核模块用于在工作流执行过程中把某个节点挂起，等待人工查看和修改后再恢复，或直接拒绝终止执行。

- Base URL: `/api/workflow/reviews`
- 当前控制器: `HumanReviewController`
- 当前返回风格: 原始 `ResponseEntity<T>` / 空 `200`，不走统一 `Response<T>` 包装

## 认证

当前接口至少要求用户已登录，`UserContext.getUserId()` 非空。

- 本地调试可使用 `debug-user` 请求头
- 未登录返回 `401`

## 核心数据结构

### PendingReviewDTO

```json
{
  "executionId": "exec-123",
  "nodeId": "node-456",
  "nodeName": "人工审核节点",
  "agentName": "Agent-1",
  "triggerPhase": "AFTER_EXECUTION",
  "pausedAt": "2026-03-18T13:00:00",
  "userId": null,
  "executionVersion": 7
}
```

字段说明：
- `executionVersion`: 当前执行版本号，审核中心列表页通过/拒绝时应透传，用于乐观锁校验

### ReviewDetailDTO

```json
{
  "executionId": "exec-123",
  "nodeId": "node-456",
  "nodeName": "知识库节点",
  "executionVersion": 7,
  "triggerPhase": "AFTER_EXECUTION",
  "nodes": [
    {
      "nodeId": "start-1",
      "nodeName": "开始",
      "nodeType": "START",
      "status": "SUCCEEDED",
      "inputs": {
        "query": "周杰是谁"
      },
      "outputs": {
        "inputMessage": "周杰是谁"
      }
    },
    {
      "nodeId": "knowledge-1",
      "nodeName": "知识库检索",
      "nodeType": "KNOWLEDGE",
      "status": "PAUSED_FOR_REVIEW",
      "inputs": {
        "query": "周杰是谁"
      },
      "outputs": {
        "knowledge_list": [
          "周杰是西南科技大学的学生"
        ]
      }
    }
  ]
}
```

字段说明：
- `nodes`: 展示“所有已成功上游节点 + 当前暂停节点”
- `inputs`: 通过表达式解析后的节点输入
- `outputs`:
  - 对上游节点：始终返回当前上下文中的输出
  - 对当前暂停节点：
    - `AFTER_EXECUTION`: 返回当前节点输出，可编辑
    - `BEFORE_EXECUTION`: 不返回当前节点输出

## 接口列表

### 1. 获取待审核列表

- 方法: `GET /api/workflow/reviews/pending`
- 返回: `200 OK` + `List<PendingReviewDTO>`

响应示例：
```json
[
  {
    "executionId": "exec-123",
    "nodeId": "node-456",
    "nodeName": "人工审核节点",
    "agentName": "Agent-1",
    "triggerPhase": "BEFORE_EXECUTION",
    "pausedAt": "2026-03-18T13:00:00",
    "userId": null,
    "executionVersion": 3
  }
]
```

实现备注：
- 当前不是分页接口
- 底层从 `HumanReviewQueuePort.getPendingExecutionIds()` 取 executionId，再逐个装配 DTO

### 2. 获取审核详情

- 方法: `GET /api/workflow/reviews/{executionId}`
- 返回: `200 OK` + `ReviewDetailDTO`

错误语义：
- `400/500` 取决于异常映射
- 关键业务校验包括：
  - 执行必须存在
  - 执行状态必须为 `PAUSED_FOR_REVIEW` 或 `PAUSED`
  - `pausedNodeId` 必须存在且不能是 `__MANUAL_PAUSE__`
  - 暂停节点必须仍能在图中找到

### 3. 审核通过并恢复执行

- 方法: `POST /api/workflow/reviews/resume`
- 返回: 空 `200`

请求体：
```json
{
  "executionId": "exec-123",
  "nodeId": "node-456",
  "expectedVersion": 7,
  "edits": {
    "knowledge_list": [
      "人工修正后的知识内容"
    ]
  },
  "comment": "审核通过，已调整输出",
  "nodeEdits": {
    "upstream-node-id": {
      "text": "人工修正后的上游输出"
    }
  }
}
```

字段说明：
- `expectedVersion`: 推荐必传。当前后端支持为空，但传值后才能触发明确的并发冲突保护
- `edits`: 当前暂停节点的修改内容
- `nodeEdits`: 多节点编辑，key 为 `nodeId`

当前处理语义：
- `BEFORE_EXECUTION`
  - `edits` 作为当前节点恢复执行时的附加输入
  - 当前节点会被重新执行
- `AFTER_EXECUTION`
  - `edits` 直接覆盖当前节点输出
  - 当前节点不会重跑，而是直接把结果推进到下游

并发保护：
- `expectedVersion != execution.version` 时抛 `OptimisticLockingFailureException`
- 当前全局异常处理会映射为 `409`

### 4. 审核拒绝并终止执行

- 方法: `POST /api/workflow/reviews/reject`
- 返回: 空 `200`

请求体：
```json
{
  "executionId": "exec-123",
  "nodeId": "node-456",
  "expectedVersion": 7,
  "reason": "输出不符合业务规则"
}
```

字段说明：
- `expectedVersion`: 后端已支持，审核中心列表页当前会透传
- `reason`: 拒绝原因

当前处理语义：
- 写审核记录 `decision=REJECT`
- `Execution.reject(nodeId)` 将执行置为失败态并清理暂停态
- 从待审核队列移除
- 推送 `workflow_rejected`

### 5. 查询审核历史

- 方法: `GET /api/workflow/reviews/history`
- 参数：
  - `userId` 可选
  - `page/size` 由 Spring Data `Pageable` 处理
- 返回: `Page<HumanReviewRecord>`

## 当前实现约束

### 响应包装差异

审核接口与大多数业务接口不同：

- `review`：原始 DTO / 空 `200`
- `knowledge`、`agent` 等：统一 `Response<T>`

这会直接影响前端 adapter 的实现方式。

### 审核详情展示边界

- 手动暂停 `__MANUAL_PAUSE__` 不会进入审核详情页
- 当前详情页主要用于“节点级人工审核”，不是所有暂停态的通用查看器

### 当前仍需关注的风险

- AFTER_EXECUTION 恢复路径的 version 递增次数仍应持续关注，避免 `resume()` 与后续推进链路重复递增
- 聊天页拒绝流程已有真实 reject 能力，但目前未像恢复流程一样透传 `expectedVersion`

## 调试建议

### 审核页/聊天页打开详情为空时

优先确认：
- 当前请求走的是 `/api/workflow/reviews/{executionId}`
- 前端没有按 `Response<T>` 去解包 review 接口
- 当前 execution 是否仍是 `PAUSED_FOR_REVIEW` / `PAUSED`

### 审核恢复报 409 时

优先确认：
- 页面拿到的 `executionVersion`
- 提交时是否带了 `expectedVersion`
- 是否已有其他人先完成了恢复/拒绝

## 更新记录

- 2026-03-18:
  - 对齐当前 review API 的真实返回风格
  - 补充 `executionVersion/expectedVersion` 并发约定
  - 补充 reject 闭环与 AFTER_EXECUTION 输出编辑语义
