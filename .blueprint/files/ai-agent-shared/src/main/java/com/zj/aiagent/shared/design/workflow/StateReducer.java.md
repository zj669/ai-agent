# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/workflow/StateReducer.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/workflow/StateReducer.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/workflow/StateReducer.java
- Type: .java

## Responsibility
- 抽象工作流状态字段的“旧值 + 新值”合并策略。
- 作为 `WorkflowState.apply` 的策略接口，支持覆盖、追加、聚合等语义。

## Key Symbols / Structure
- `@FunctionalInterface StateReducer<T>`
- `T reduce(T oldValue, T newValue)`

## Dependencies
- 仅 JDK 泛型与函数式接口语义。

## Notes
- 与 `BuiltInReducers` 配合使用，业务也可自定义 reducer。
