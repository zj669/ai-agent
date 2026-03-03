# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/CheckpointRepository.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/CheckpointRepository.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/CheckpointRepository.java
- Type: .java (interface)
- Status: 正常

## Responsibility
- 检查点持久化端口，负责保存、读取与删除执行检查点/暂停点。
- 当前阶段主链路仅强依赖 `save(Checkpoint)`；读取与删除能力为预留扩展接口。

## Key Symbols / Structure
- `save(Checkpoint checkpoint)`
- `findLatest(String executionId)`
- `findPausePoint(String executionId)`
- `deleteByExecutionId(String executionId)`

## Dependencies
- `Checkpoint`
- `Optional`

## Notes
- 为执行恢复能力提供存储抽象。
- 需求边界：不将 checkpoint 读取恢复作为本阶段验收前提。
