# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/SseEventType.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/SseEventType.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/SseEventType.java
- Type: .java

## Responsibility
- 定义 workflow 流式推送（SSE）事件类型枚举，描述节点执行事件生命周期。
- 为 `StreamPublisher` 与接口层 SSE 输出约定统一事件语义。

## Key Symbols / Structure
- `START`: 节点开始执行事件。
- `UPDATE`: 流式增量更新事件（token/chunk）。
- `FINISH`: 节点执行完成事件。
- `ERROR`: 节点执行错误事件。

## Dependencies
- 无外部依赖（纯领域枚举）。

## Notes
- 该枚举最终映射到接口层 SSE event name（如 `WorkflowController` 中按事件类型输出），用于前端流式渲染状态机驱动。
