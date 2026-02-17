# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/ConditionItem.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/ConditionItem.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/ConditionItem.java
- Type: .java
- Status: 正常

## Responsibility
- 条件评估中的最小比较单元，定义左操作数、比较操作符与右操作数。

## Key Symbols / Structure
- 字段：`leftOperand`, `operator`, `rightOperand`。

## Dependencies
- `ComparisonOperator`

## Notes
- 左右操作数可承载变量引用与字面值，供条件评估器解析与比较。
