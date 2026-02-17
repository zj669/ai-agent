# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/ConditionBranch.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/ConditionBranch.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/ConditionBranch.java
- Type: .java
- Status: 正常

## Responsibility
- 条件节点分支值对象，描述分支优先级、目标节点、描述、default 标识与条件组。
- 作为结构化条件评估输入，由评估器按优先级选择命中分支。

## Key Symbols / Structure
- 字段：`priority`, `targetNodeId`, `description`, `isDefault`, `conditionGroups`。

## Dependencies
- `ConditionGroup`
- `List`

## Notes
- 语义约束：分支按 `priority` 升序评估，default 分支作为兜底。
