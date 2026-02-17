# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/ConditionGroup.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/ConditionGroup.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/ConditionGroup.java
- Type: .java
- Status: 正常

## Responsibility
- 条件组值对象，定义组内条件项列表及其逻辑连接方式（AND/OR）。

## Key Symbols / Structure
- 字段：`operator`, `conditions`。

## Dependencies
- `LogicalOperator`
- `ConditionItem`
- `List`

## Notes
- 组内逻辑通过 `operator` 控制，作为 `ConditionBranch` 评估组成部分。
