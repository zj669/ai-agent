# HumanReviewController.java 蓝图

## Metadata
- title: HumanReviewController
- type: controller
- summary: 人工审核 REST 控制器，提供待审核列表、审核详情、提交审核（恢复执行）、审核历史四个端点

## 路由：`/api/workflow/reviews`

### GET /pending — 获取待审核列表
- purpose: 从 Redis Set 获取所有待审核 executionId，逐个查询 Execution 组装 DTO
- output: `List<PendingReviewDTO>`
- 已知问题: 直接使用 `RedissonClient` 而非 `HumanReviewQueuePort`

### GET /{executionId} — 获取审核详情
- purpose: 加载 Execution，收集上游已成功节点 + 当前暂停节点的输入输出
- output: `ReviewDetailDTO`（含 `List<NodeContextDTO>`）
- 关键逻辑:
  - filter: `nodeStatus == SUCCEEDED || nodeId == pausedNodeId`
  - 当前节点: BEFORE_EXECUTION 不展示输出，AFTER_EXECUTION 展示输出
  - 上游节点: 始终展示输出
  - 所有节点: 通过 `expressionResolver.resolveInputs()` 解析输入
- 已知问题: L81-83 空 if 块（状态校验无实际处理）

### POST /resume — 提交审核（恢复执行）
- purpose: 委托 `SchedulerService.resumeExecution()` 恢复暂停的工作流
- input: `ResumeExecutionRequest`（executionId, nodeId, edits, comment, nodeEdits）
- 鉴权: 从 `UserContext.getUserId()` 获取审核人ID

### GET /history — 审核历史
- purpose: 分页查询审核记录
- input: userId (optional), pageable
- output: `Page<HumanReviewRecord>`

## 依赖
- `SchedulerService`：恢复执行
- `RedissonClient`：待审核队列（应改为 HumanReviewQueuePort）
- `HumanReviewRepository`：审核历史
- `ExecutionRepository`：加载执行实例
- `ExpressionResolverPort`：解析节点输入表达式

## 变更记录
- 2026-03-17: 初始蓝图生成
