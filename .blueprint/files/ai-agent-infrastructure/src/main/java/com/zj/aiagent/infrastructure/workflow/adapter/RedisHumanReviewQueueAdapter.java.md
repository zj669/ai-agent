## Metadata
- file: `.blueprint/files/ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/adapter/RedisHumanReviewQueueAdapter.java.md`
- source: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/adapter/RedisHumanReviewQueueAdapter.java`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-15
- owner: backend-blueprint-shadow

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: RedisHumanReviewQueueAdapter
- 实现 `HumanReviewQueuePort`，用 Redis Set 管理待审核 executionId 队列。
- 把审核队列这一领域能力映射到 `IRedisService` 的集合操作。

## 2) 核心方法
- `addToPendingQueue(String executionId)`
- `removeFromPendingQueue(String executionId)`
- `isInPendingQueue(String executionId)`
- `getPendingExecutionIds()`

## 3) 具体方法
### 3.1 addToPendingQueue(...)
- 函数签名: `public void addToPendingQueue(String executionId)`
- 入参: executionId
- 出参: 无
- 功能含义: 将执行ID加入 `human_review:pending` 集合。
- 链路作用: 进入人工审核时登记待处理队列。

### 3.2 getPendingExecutionIds()
- 函数签名: `public Set<String> getPendingExecutionIds()`
- 入参: 无
- 出参: 待审核 executionId 集合
- 功能含义: 读取 Redis Set 全量成员，失败时返回空集。
- 链路作用: 审核任务列表与轮询处理的数据来源。

## 4) 变更记录
- 2026-02-15: 回填人工审核队列适配器蓝图语义。
- 2026-02-14: 初始化镜像蓝图（自动补缺）。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
