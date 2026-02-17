# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/workflow/WorkflowStateListener.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/workflow/WorkflowStateListener.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/workflow/WorkflowStateListener.java
- Type: .java

## Responsibility
- 定义工作流执行事件监听契约，覆盖工作流级、节点级与用户可见输出事件。
- 为 SSE/实时推送等基础设施提供统一回调面。
- 不实现传输协议，仅定义事件语义。

## Key Symbols / Structure
- 工作流事件：`onWorkflowStarted/onWorkflowCompleted/onWorkflowFailed`。
- 节点事件：`onNodeStarted/onNodeStreaming/onNodeCompleted/onNodeFailed/onNodePaused`。
- 用户输出：`onFinalAnswer`。

## Dependencies
- 依赖 `WorkflowState` 作为节点完成回调结果类型。

## Notes
- 适配器层可将这些回调映射到 Redis Pub/Sub、SSE 或日志系统。
