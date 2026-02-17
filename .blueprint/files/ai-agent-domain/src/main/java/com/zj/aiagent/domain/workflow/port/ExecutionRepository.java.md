# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/ExecutionRepository.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/ExecutionRepository.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/ExecutionRepository.java
- Type: .java

## Responsibility
- 定义 Execution 聚合根的持久化端口，屏蔽 Redis/MySQL 等存储实现细节。
- 提供执行全生命周期的数据访问能力：保存、查询、更新、删除、按会话追溯历史。

## Key Symbols / Structure
- `void save(Execution execution)`
  - 持久化新建执行实例。
- `Optional<Execution> findById(String executionId)`
  - 按执行 ID 查询聚合根快照。
- `void update(Execution execution)`
  - 更新执行状态，契约要求支持乐观锁冲突语义。
- `void delete(String executionId)`
  - 删除执行数据。
- `List<Execution> findByConversationId(String conversationId)`
  - 查询会话维度的执行历史。

## Dependencies
- Domain aggregate:
  - `Execution`
- Java types:
  - `Optional`, `List`

## Notes
- 该端口被 `SchedulerService` 与接口层调试查询接口共同依赖，是执行状态读写的统一入口。
- 乐观锁失败异常语义由实现层映射到 `OptimisticLockingFailureException`（注释约定）。
