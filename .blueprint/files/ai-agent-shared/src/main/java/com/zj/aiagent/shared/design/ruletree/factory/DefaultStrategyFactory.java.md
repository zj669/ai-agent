# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/ruletree/factory/DefaultStrategyFactory.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/ruletree/factory/DefaultStrategyFactory.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/ruletree/factory/DefaultStrategyFactory.java
- Type: .java

## Responsibility
- 定义单线程规则树工厂基类，提供根路由节点与统一处理器出口。
- 对外隐藏规则树内部拓扑细节。

## Key Symbols / Structure
- `getRootNode()`：返回 `AbstractStrategyRouter` 根节点。
- `strategyHandler()`：直接返回根节点作为处理器。

## Dependencies
- `AbstractStrategyRouter`、`StrategyHandler`。
- Lombok `@Data`。

## Notes
- 便于调用方以统一 `StrategyHandler` 方式执行整棵规则树。
