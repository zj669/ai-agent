## Metadata
- file: `.blueprint/infrastructure/persistence/RedisRepositories.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: RedisRepositories
- 该文件描述 Redis 持久化适配器的职责边界，实现领域层定义的 Port 接口，负责执行快照（Checkpoint）的保存/加载、工作流取消标记、SSE 事件发布。使用 IRedisService 封装 Redis 操作，支持 JSON 序列化和 TTL 管理。

## 2) 核心方法
- `saveCheckpoint()`
- `loadCheckpoint()`
- `deleteCheckpoint()`
- `publish()`
- `markAsCancelled()`

## 3) 具体方法
### 3.1 saveCheckpoint()
- 函数签名: `void saveCheckpoint(String executionId, Execution execution)` (ExecutionRepositoryImpl)
- 入参: `executionId` 执行 ID，`execution` 执行聚合根对象（包含状态、节点状态、上下文）
- 出参: 无（副作用：保存到 Redis，key=workflow:checkpoint:{executionId}，TTL=1小时）
- 功能含义: 保存执行快照到 Redis，用于暂停/恢复和故障恢复，序列化为 JSON 存储
- 链路作用: SchedulerService.pauseExecution() → ExecutionRepository.saveCheckpoint() → IRedisService.setString() → Redis String 类型

### 3.2 loadCheckpoint()
- 函数签名: `Optional<Execution> loadCheckpoint(String executionId)` (ExecutionRepositoryImpl)
- 入参: `executionId` 执行 ID
- 出参: `Optional<Execution>` 反序列化的执行对象，不存在返回 empty
- 功能含义: 从 Redis 加载执行快照，用于恢复暂停的执行或故障恢复，反序列化 JSON 为领域对象
- 链路作用: SchedulerService.resumeExecution() → ExecutionRepository.loadCheckpoint() → IRedisService.getString() → JSON 反序列化

### 3.3 deleteCheckpoint()
- 函数签名: `void deleteCheckpoint(String executionId)` (ExecutionRepositoryImpl)
- 入参: `executionId` 执行 ID
- 出参: 无（副作用：删除 Redis key）
- 功能含义: 删除执行快照，在执行完成或取消后清理 Redis 内存
- 链路作用: SchedulerService.onExecutionComplete() → ExecutionRepository.deleteCheckpoint() → IRedisService.delete() → Redis DEL 命令

### 3.4 publish()
- 函数签名: `void publish(String executionId, StreamEvent event)` (RedisSsePublisher)
- 入参: `executionId` 执行 ID，`event` 流式事件对象（包含 eventType、nodeId、data）
- 出参: 无（副作用：发布到 Redis Pub/Sub 频道）
- 功能含义: 发布 SSE 事件到 Redis 频道（workflow:channel:{executionId}），由 WorkflowController 的 RedisSseListener 订阅并推送到前端
- 链路作用: NodeExecutorStrategy.executeAsync() → StreamPublisher.publishDelta() → RedisSsePublisher.publish() → Redis PUBLISH → RedisSseListener → SseEmitter

### 3.5 markAsCancelled()
- 函数签名: `void markAsCancelled(String executionId)` (RedisWorkflowCancellationAdapter)
- 入参: `executionId` 执行 ID
- 出参: 无（副作用：设置取消标记，key=workflow:cancel:{executionId}，TTL=1小时）
- 功能含义: 标记工作流为已取消状态，用于中断正在执行的节点，执行器在每个节点前检查此标记
- 链路作用: SchedulerService.cancelExecution() → WorkflowCancellationPort.markAsCancelled() → IRedisService.setString() → SchedulerService.executeNode() 检查 isCancelled()


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全所有方法签名、入参、出参、功能含义和链路作用，基于 RedisWorkflowCancellationAdapter 和 ExecutionRepositoryImpl 实现。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
