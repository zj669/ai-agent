# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/workflow/WorkflowState.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/workflow/WorkflowState.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/workflow/WorkflowState.java
- Type: .java

## Responsibility
- 作为工作流共享状态容器，维护执行期键值数据与对应 Reducer 合并策略。
- 提供状态读取、更新、复制与按 `StateUpdate` 合并的核心能力。
- 不负责节点调度，只提供线程安全状态存储与合并。

## Key Symbols / Structure
- `data/reducers`: `ConcurrentHashMap` 维护状态与合并器。
- `apply(StateUpdate update)`: 按 key 应用 reducer 或覆盖写入。
- `registerReducer(String key, StateReducer<T> reducer)`: 注册字段级合并策略。
- `copy()`: 复制状态与 reducer 注册表。

## Dependencies
- `StateUpdate`、`StateReducer`、`WorkflowStateListener`。
- Lombok `@AllArgsConstructor/@Getter/@Setter/@Slf4j`。

## Notes
- `apply` 中 reducer 失败会降级覆盖并记录 warn 日志。
