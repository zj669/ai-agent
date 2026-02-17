# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/ExpressionResolverPort.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/ExpressionResolverPort.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/ExpressionResolverPort.java
- Type: .java (interface)
- Status: 正常

## Responsibility
- 表达式解析端口，提供基于 `ExecutionContext` 的 SpEL 变量解析与输入批量解析。

## Key Symbols / Structure
- `resolve(String expression, ExecutionContext context)`
- `resolveInputs(Map<String, Object> inputMappings, ExecutionContext context)`

## Dependencies
- `ExecutionContext`
- `Map`

## Notes
- 解析失败返回原始表达式的容错语义由实现遵循该契约。
