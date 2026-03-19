# RedisHumanReviewQueueAdapter.java 蓝图

## Metadata
- title: RedisHumanReviewQueueAdapter
- type: adapter
- summary: 基于 Redis Set 的人工审核队列适配器，实现 HumanReviewQueuePort，使用 IRedisService 操作 key `human_review:pending`

## 技术细节
- Redis 数据结构：Set
- Key：`human_review:pending`
- 无过期时间（持久化存储，直到审核完成手动移除）
- 使用项目封装的 `IRedisService`（非直接 RedisTemplate）

## 关键方法单元

### addToPendingQueue
- purpose: `IRedisService.addToSet(PENDING_QUEUE_KEY, executionId)`
- 异常处理: catch → log.error → 抛 RuntimeException

### removeFromPendingQueue
- purpose: `IRedisService.removeFromSet(PENDING_QUEUE_KEY, executionId)`
- 异常处理: catch → log.error → 抛 RuntimeException

### isInPendingQueue
- purpose: `IRedisService.isSetMember(PENDING_QUEUE_KEY, executionId)`
- 异常处理: catch → log.error → 返回 false（降级）

### getPendingExecutionIds
- purpose: `IRedisService.getSetMembers(PENDING_QUEUE_KEY)`
- 异常处理: catch → log.error → 返回 emptySet（降级）

## 已知问题
- `HumanReviewController` 绕过此适配器，直接用 `RedissonClient.getSet("human_review:pending")` 操作同一 key，形成两套 Redis 访问路径

## 变更记录
- 2026-03-17: 初始蓝图生成
