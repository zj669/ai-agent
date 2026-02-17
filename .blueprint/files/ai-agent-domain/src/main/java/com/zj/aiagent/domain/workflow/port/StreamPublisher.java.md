# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/StreamPublisher.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/StreamPublisher.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/StreamPublisher.java
- Type: .java (interface)
- Status: 正常

## Responsibility
- 节点执行过程流式事件发布端口，抽象开始、增量、思考、完成、错误、结构化数据与自定义事件发布能力。

## Key Symbols / Structure
- 生命周期事件：`publishStart()`, `publishFinish(NodeExecutionResult result)`
- 文本流：`publishDelta(String delta)`, `publishDelta(String delta, boolean isThought)`
- 思考与错误：`publishThought(String thought)`, `publishError(String errorMessage)`
- 数据与自定义事件：`publishData(Object data, String renderMode)`, `publishEvent(String eventType, Map<String, Object> payload)`

## Dependencies
- `NodeExecutionResult`
- `Map<String, Object>`

## Notes
- 该端口将领域执行过程与 SSE/消息中间件实现解耦。
