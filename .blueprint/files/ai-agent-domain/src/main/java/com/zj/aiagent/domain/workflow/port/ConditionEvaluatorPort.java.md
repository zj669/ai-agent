# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/ConditionEvaluatorPort.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/ConditionEvaluatorPort.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/ConditionEvaluatorPort.java
- Type: .java (interface)
- Status: 正常

## Responsibility
- 条件评估端口，按优先级评估 `ConditionBranch` 列表并返回命中分支。
- 约束分支配置合法性（default 分支规则）并通过异常暴露错误配置。

## Key Symbols / Structure
- `evaluate(List<ConditionBranch> branches, ExecutionContext context)`

## Dependencies
- `ConditionBranch`, `ExecutionContext`
- `List`

## Notes
- 具体实现位于 Infrastructure（如结构化条件评估器）。
