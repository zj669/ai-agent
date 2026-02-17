# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/dag/DagContext.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/dag/DagContext.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/dag/DagContext.java
- Type: .java

## Responsibility
- 定义 DAG 执行上下文读写契约，统一承载执行期共享数据与节点结果。
- 提供执行标识访问能力，支撑日志追踪与跨组件关联。

## Key Symbols / Structure
- 通用数据：`setValue/getValue/getValue(default)`。
- 节点结果：`setNodeResult/getNodeResult/getAllNodeResults/isNodeExecuted`。
- 标识：`getExecutionId()/getConversationId()`。

## Dependencies
- JDK `Map<String, Object>`。

## Notes
- 接口不限制上下文存储实现，可由内存/缓存/持久化适配。
