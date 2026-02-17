# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/Branch.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/Branch.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/Branch.java
- Type: .java
- Status: 正常

## Responsibility
- 条件路由分支值对象，描述分支标识、展示信息、条件表达式与下游节点集合。

## Key Symbols / Structure
- 字段：`branchId`, `label`, `description`, `condition`, `nextNodeIds`。

## Dependencies
- `String[]`

## Notes
- `condition` 仅在 EXPRESSION 模式生效，`description` 主要用于 LLM 语义路由。
