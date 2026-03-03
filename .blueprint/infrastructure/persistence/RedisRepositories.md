## Metadata
- file: `.blueprint/infrastructure/persistence/RedisRepositories.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-03-02
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: RedisRepositories
- 该文件描述 Redis 持久化适配器的职责边界，实现领域层定义的 Port 接口，负责执行快照（Checkpoint）写入、工作流取消标记、SSE 事件发布。使用 IRedisService 封装 Redis 操作，支持 JSON 序列化和 TTL 管理。
- 当前阶段范围仅服务“暂停/恢复 + 人审通过继续”主路径。

## 2) 核心方法
- `saveCheckpoint()`
- `publish()`
- `markAsCancelled()`

## 3) 具体方法
### 3.1 saveCheckpoint()
- 函数签名: `void save(Checkpoint checkpoint)`（`CheckpointRepository` 适配器）
- 入参: `checkpoint` 执行检查点对象
- 出参: 无（副作用：写入 Redis key，含普通 checkpoint 与 pause checkpoint）
- 功能含义: 持久化执行快照，用于暂停态留痕与运行轨迹保留。
- 链路作用: SchedulerService.checkPause()/onNodeComplete() → CheckpointRepository.save() → IRedisService.setString()。

### 3.2 publish()
- 函数签名: `void publish(String executionId, StreamEvent event)` (RedisSsePublisher)
- 入参: `executionId` 执行 ID，`event` 流式事件对象（包含 eventType、nodeId、data）
- 出参: 无（副作用：发布到 Redis Pub/Sub 频道）
- 功能含义: 发布 SSE 事件到 Redis 频道（workflow:channel:{executionId}），由 WorkflowController 的 RedisSseListener 订阅并推送到前端
- 链路作用: NodeExecutorStrategy.executeAsync() → StreamPublisher.publishDelta() → RedisSsePublisher.publish() → Redis PUBLISH → RedisSseListener → SseEmitter

### 3.3 markAsCancelled()
- 函数签名: `void markAsCancelled(String executionId)` (RedisWorkflowCancellationAdapter)
- 入参: `executionId` 执行 ID
- 出参: 无（副作用：设置取消标记，key=workflow:cancel:{executionId}，TTL=1小时）
- 功能含义: 标记工作流为已取消状态，用于中断正在执行的节点，执行器在每个节点前检查此标记
- 链路作用: SchedulerService.cancelExecution() → WorkflowCancellationPort.markAsCancelled() → IRedisService.setString() → SchedulerService.executeNode() 检查 isCancelled()

## 4) 非目标说明
- 当前阶段不把“checkpoint 读取恢复（load/findPausePoint/findLatest）”作为验收要求。
- checkpoint 删除/清理策略属于后续优化项，不纳入本阶段主路径契约。


## 5) 变更记录
- 2026-03-02: 收敛蓝图范围，仅保留“暂停/恢复 + 人审通过继续”主路径所需 Redis 能力，移除 checkpoint 读取恢复强契约。
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全所有方法签名、入参、出参、功能含义和链路作用，基于 RedisWorkflowCancellationAdapter 和 ExecutionRepositoryImpl 实现。

## 6) Temp缓存区
当前状态为 `正常`，本区留空。
