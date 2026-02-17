# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/StreamContext.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/StreamContext.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/StreamContext.java
- Type: .java
- Status: 正常

## Responsibility
- 流式发布上下文值对象，描述 execution/node 维度的发布元数据。

## Key Symbols / Structure
- 字段：`executionId`, `nodeId`, `parentId`, `nodeType`, `nodeName`, `isFinalOutputNode`。

## Dependencies
- 无

## Notes
- 作为 `StreamPublisherFactory` 的输入上下文，避免发布器直接依赖执行聚合。
