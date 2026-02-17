# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/Checkpoint.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/Checkpoint.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/Checkpoint.java
- Type: .java
- Status: 正常

## Responsibility
- 执行检查点值对象，封装执行上下文快照、当前节点、创建时间与暂停点标记。
- 提供普通检查点与暂停检查点的统一工厂创建能力。

## Key Symbols / Structure
- 字段：`checkpointId`, `executionId`, `currentNodeId`, `contextSnapshot`, `createdAt`, `pausePoint`。
- 工厂方法：
  - `create(String executionId, String nodeId, ExecutionContext context)`
  - `createPausePoint(String executionId, String nodeId, ExecutionContext context)`

## Dependencies
- `ExecutionContext`
- `LocalDateTime`

## Notes
- 创建时通过 `context.snapshot()` 进行上下文深拷贝语义。
