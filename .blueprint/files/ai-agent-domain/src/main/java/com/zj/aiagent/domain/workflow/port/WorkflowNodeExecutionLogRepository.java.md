# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/WorkflowNodeExecutionLogRepository.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/WorkflowNodeExecutionLogRepository.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/WorkflowNodeExecutionLogRepository.java
- Type: .java (interface)
- Status: 正常

## Responsibility
- 节点执行日志仓储端口，定义节点日志写入与按执行/节点维度查询能力。

## Key Symbols / Structure
- `save(WorkflowNodeExecutionLog log)`
- `findByExecutionId(String executionId)`
- `findByExecutionIdAndNodeId(String executionId, String nodeId)`
- `findByExecutionIdOrderByEndTime(String executionId)`

## Dependencies
- `WorkflowNodeExecutionLog`
- `List`

## Notes
- `findByExecutionIdOrderByEndTime` 用于按执行顺序提取最终输出节点。
