# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/dag/DagNode.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/dag/DagNode.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/dag/DagNode.java
- Type: .java

## Responsibility
- 定义 DAG 节点基础契约：节点标识、依赖、执行入口、生命周期与并发/超时属性。
- 为 DAG 调度器提供统一可执行节点抽象。

## Key Symbols / Structure
- `getNodeId()/getNodeName()`：节点标识信息。
- `getDependencies()`：上游依赖集合。
- `execute(C context)`：节点执行入口。
- 生命周期：`beforeExecute/afterExecute`（默认空实现）。
- 执行属性：`isParallelizable()`、`getTimeoutMillis()`。

## Dependencies
- `DagContext`、`DagNodeExecutionException`。
- JDK `Set`。

## Notes
- 通过泛型 `C/R` 适配不同上下文和结果类型。
