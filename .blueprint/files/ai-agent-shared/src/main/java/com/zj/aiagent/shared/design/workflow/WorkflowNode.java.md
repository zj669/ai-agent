# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/workflow/WorkflowNode.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/workflow/WorkflowNode.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/workflow/WorkflowNode.java
- Type: .java

## Responsibility
- 定义工作流节点标准契约：标识、类型、执行入口与生命周期回调。
- 约束节点输入 `WorkflowState`、输出 `StateUpdate` 的状态驱动模型。
- 不处理路由与调度，仅描述单节点行为。

## Key Symbols / Structure
- `getNodeId()/getNodeType()`: 节点识别元信息。
- `execute(WorkflowState state)`: 节点主执行方法。
- `beforeExecute/afterExecute`: 生命周期钩子（默认空实现）。
- `getTimeoutMillis()`: 节点超时配置（默认 0）。

## Dependencies
- `WorkflowState`、`StateUpdate`。

## Notes
- 与 `NodeExecutor` 并存，前者偏 workflow 抽象，后者偏执行器清单描述。
