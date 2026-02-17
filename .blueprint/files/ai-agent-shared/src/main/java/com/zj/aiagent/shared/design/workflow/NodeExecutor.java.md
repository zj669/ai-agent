# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/workflow/NodeExecutor.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/workflow/NodeExecutor.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/workflow/NodeExecutor.java
- Type: .java

## Responsibility
- 定义可执行节点的最小执行器接口，面向执行层统一调用。
- 约束执行器提供节点元信息和执行入口。

## Key Symbols / Structure
- `getNodeId()/getNodeName()/getDescription()`
- `execute(WorkflowState state): StateUpdate`

## Dependencies
- `WorkflowState`、`StateUpdate`。

## Notes
- 相比 `WorkflowNode` 更轻量，适合执行器注册与调度入口抽象。
