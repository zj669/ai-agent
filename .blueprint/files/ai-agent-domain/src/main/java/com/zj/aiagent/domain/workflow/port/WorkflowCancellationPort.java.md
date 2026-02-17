# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/WorkflowCancellationPort.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/WorkflowCancellationPort.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/WorkflowCancellationPort.java
- Type: .java (interface)
- Status: 正常

## Responsibility
- 工作流取消状态管理端口，定义标记取消与取消查询契约。

## Key Symbols / Structure
- `markAsCancelled(String executionId)`
- `isCancelled(String executionId)`

## Dependencies
- 无额外领域对象依赖

## Notes
- 取消状态通常由外部存储维护，Domain 仅声明语义。
