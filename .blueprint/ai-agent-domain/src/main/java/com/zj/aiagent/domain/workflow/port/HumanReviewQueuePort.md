# HumanReviewQueuePort.java 蓝图

## Metadata
- title: HumanReviewQueuePort
- type: port
- summary: 人工审核队列端口接口，管理待审核的工作流执行队列，Domain 层定义、Infrastructure 层实现

## 关键方法单元

### addToPendingQueue
- purpose: 将执行ID加入待审核队列
- input: executionId (String)
- 调用时机: `SchedulerService.checkPause()` 暂停执行后

### removeFromPendingQueue
- purpose: 从待审核队列移除执行ID
- input: executionId (String)
- 调用时机: `SchedulerService.resumeExecution()` 审核通过后

### isInPendingQueue
- purpose: 检查执行是否在待审核队列中
- input: executionId (String)
- output: boolean

### getPendingExecutionIds
- purpose: 获取所有待审核的执行ID集合
- output: Set<String>

## 实现
- `RedisHumanReviewQueueAdapter`：基于 Redis Set (`human_review:pending`) 实现

## 已知问题
- `HumanReviewController.getPendingReviews()` 绕过此端口，直接使用 `RedissonClient` 操作同一 key

## 变更记录
- 2026-03-17: 初始蓝图生成
