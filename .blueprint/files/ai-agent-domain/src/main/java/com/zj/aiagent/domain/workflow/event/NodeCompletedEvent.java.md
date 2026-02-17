# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/event/NodeCompletedEvent.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/event/NodeCompletedEvent.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/event/NodeCompletedEvent.java
- Type: .java
- Status: 正常

## Responsibility
- 节点完成事件对象，记录节点执行完成时的输入、输出、状态、渲染模式与时间信息。

## Key Symbols / Structure
- 字段：`executionId`, `nodeId`, `nodeName`, `nodeType`, `renderMode`, `status`, `inputs`, `outputs`, `errorMessage`, `startTime`, `endTime`。

## Dependencies
- `Map<String, Object>`
- `LocalDateTime`

## Notes
- 与节点执行日志模型语义一致，用于事件驱动链路传递节点结果。
