# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/dag/DagNodeExecutionException.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/dag/DagNodeExecutionException.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/dag/DagNodeExecutionException.java
- Type: .java

## Responsibility
- 定义 DAG 节点执行异常类型，补充节点标识与是否可重试元信息。
- 用于执行器与调度器之间传递可观测错误上下文。

## Key Symbols / Structure
- 字段：`nodeId`、`retryable`。
- 构造：基础 message/cause 构造与完整构造。
- 读取：`getNodeId()`、`isRetryable()`。

## Dependencies
- 继承 JDK `Exception`。

## Notes
- `retryable` 默认 `true`，可由调用方在完整构造中显式覆盖。
