# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/StreamPublisherFactory.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/StreamPublisherFactory.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/StreamPublisherFactory.java
- Type: .java (interface)
- Status: 正常

## Responsibility
- 流式发布器工厂端口，根据 `StreamContext` 创建对应 `StreamPublisher` 实例。

## Key Symbols / Structure
- `create(StreamContext context)`

## Dependencies
- `StreamContext`
- `StreamPublisher`

## Notes
- 用于将发布上下文（executionId/nodeId/渲染信息）注入到具体发布器。
