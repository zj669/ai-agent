# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/workflow/BuiltInReducers.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/workflow/BuiltInReducers.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/workflow/BuiltInReducers.java
- Type: .java

## Responsibility
- 提供工作流内置状态合并策略实现，覆盖常见覆盖、列表追加、消息合并、计数累加、Map 合并场景。
- 不绑定具体业务字段，由调用方按 key 注册到 `WorkflowState`。

## Key Symbols / Structure
- `overwrite()`：新值覆盖旧值。
- `appendList()`：列表追加。
- `addMessages()`：按消息 ID 合并。
- `increment()`：整数累加。
- `mergeMap()`：Map 合并（新值覆盖同键）。
- `extractMessageId(Object)`：反射提取 `getId()`。

## Dependencies
- `StateReducer`。
- JDK 集合与反射 API。

## Notes
- `addMessages` 使用 `LinkedHashMap` 保序。
