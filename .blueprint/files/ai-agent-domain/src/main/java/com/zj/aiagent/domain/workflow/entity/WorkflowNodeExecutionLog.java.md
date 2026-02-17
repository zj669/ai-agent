# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/WorkflowNodeExecutionLog.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/WorkflowNodeExecutionLog.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/WorkflowNodeExecutionLog.java
- Type: .java
- Status: 正常

## Responsibility
- 节点级执行日志实体，记录节点执行输入/输出、状态、渲染模式与时序信息。
- 为执行审计、可观测性与最终响应提取提供基础数据。

## Key Symbols / Structure
- 字段：`id`, `executionId`, `nodeId`, `nodeName`, `nodeType`, `renderMode`, `status`, `inputs`, `outputs`, `errorMessage`, `startTime`, `endTime`。

## Dependencies
- `Map<String, Object>`
- `LocalDateTime`

## Notes
- 状态字段使用整型语义（Running/Success/Failed），与事件/持久化层对齐。
