# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/ruletree/AbstractStrategyRouter.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/ruletree/AbstractStrategyRouter.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/ruletree/AbstractStrategyRouter.java
- Type: .java

## Responsibility
- 提供规则树单线程路由基类：先选策略再执行，未命中时走默认处理器。
- 统一路由模板，子类仅需实现 `get(...)` 路由规则。

## Key Symbols / Structure
- `defaultStrategyHandler`：默认处理器，默认 `StrategyHandler.DEFAULT`。
- `router(request, dynamicContext)`：路由并执行目标处理器。

## Dependencies
- `StrategyMapper`、`StrategyHandler`。
- Lombok `@Getter/@Setter`。

## Notes
- 不包含异步预处理能力，异步版见 `AbstractMultiThreadStrategyRouter`。
