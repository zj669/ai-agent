# SchedulerService.java 人工审核蓝图（补充）

## Metadata
- title: SchedulerService（人工审核相关）
- type: application-service
- summary: SchedulerService 中与人工审核相关的三个核心方法：checkPause（触发暂停）、resumeExecution（恢复执行）、以及 scheduleNode/onNodeComplete 中的暂停检查点

## 依赖注入（审核相关）
- `HumanReviewQueuePort`：Redis 待审核队列
- `HumanReviewRepository`：审核记录持久化

## 关键方法单元

### checkPause（私有方法）
- location: SchedulerService.checkPause(executionId, node, phase, publisher, outputs)
- purpose: 检查节点是否需要人工审核，如需要则暂停执行
- 返回: boolean（true=已暂停，false=继续执行）
- 判断逻辑:
  1. `node.requiresHumanReview()` — 节点是否配置了审核
  2. `execution.isNodeReviewed(nodeId)` — 是否已审核过（防重复）
  3. `config.getTriggerPhase() == phase` — 阶段是否匹配（默认 BEFORE_EXECUTION）
- 暂停操作序列:
  1. 加分布式锁 `lock:exec:{executionId}`
  2. 从 Redis 重新加载 Execution
  3. `execution.advance(nodeId, NodeExecutionResult.paused(phase, outputs))`
  4. 保存 Checkpoint + 更新 Execution
  5. SSE 发布 `workflow_paused` 事件
  6. `humanReviewQueuePort.addToPendingQueue(executionId)`
  7. 更新聊天消息为暂停摘要

### 调用点 1：scheduleNode（BEFORE_EXECUTION）
- location: SchedulerService.scheduleNode() L408
- 时机: 节点执行前
- 调用: `checkPause(executionId, node, BEFORE_EXECUTION, publisher, null)`
- outputs 参数: `null`（节点还没执行，没有输出）

### 调用点 2：onNodeComplete（AFTER_EXECUTION）
- location: SchedulerService.onNodeComplete() L594-595
- 时机: 节点执行完成后
- 调用: `checkPause(executionId, node, AFTER_EXECUTION, publisher, result.getOutputs())`
- outputs 参数: 节点执行器返回的实际输出

### resumeExecution
- location: SchedulerService.resumeExecution(executionId, nodeId, edits, reviewerId, comment, nodeEdits)
- purpose: 审核通过后恢复暂停的工作流执行
- 入口: `POST /api/workflow/reviews/resume`
- 操作序列:
  1. 加分布式锁 `lock:exec:{executionId}`
  2. 校验 `pausedNodeId` 匹配
  3. Context Merging:
     - BEFORE_EXECUTION: edits 作为 additionalInputs
     - AFTER_EXECUTION: edits 覆盖当前节点 output
  4. 多节点编辑: 遍历 `nodeEdits` 更新各节点 output
  5. 保存 `HumanReviewRecord`（decision=APPROVE）
  6. `execution.resume(nodeId, edits)` → 状态恢复为 RUNNING
  7. 更新 Execution 到 Redis
  8. SSE 发布 `workflow_resumed` 事件
  9. `humanReviewQueuePort.removeFromPendingQueue(executionId)`
  10. 调度下一步:
      - AFTER_EXECUTION → 构造 success result → `onNodeComplete()` → 推进后续节点
      - BEFORE_EXECUTION → `scheduleNodes(readyNodes)` → 重新执行该节点

## 变更记录
- 2026-03-17: 初始蓝图生成
