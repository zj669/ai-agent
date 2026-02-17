# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/event/ExecutionCompletedEvent.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/event/ExecutionCompletedEvent.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/event/ExecutionCompletedEvent.java
- Type: .java
- Status: 正常

## Responsibility
- 工作流执行完成事件对象，承载执行完成后的核心上下文（状态、输出、完成时间）。

## Key Symbols / Structure
- 字段：`executionId`, `conversationId`, `status`, `outputs`, `completedAt`。

## Dependencies
- `ExecutionStatus`
- `Map<String, Object>`
- `LocalDateTime`

## Notes
- 用于跨层传递执行结束信号与结果数据。
