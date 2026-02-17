# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/strategymode/factory/DefaultStrategyFactory.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/strategymode/factory/DefaultStrategyFactory.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/strategymode/factory/DefaultStrategyFactory.java
- Type: .java

## Responsibility
- 定义策略处理器工厂基类：统一暴露处理器集合并提供按类型匹配能力。
- 对空类型/空处理器集合做空返回兜底。

## Key Symbols / Structure
- `getAllHandlers()`：由子类提供处理器集合。
- `getHandler(IBaseEnum type)`：按 `handler.getType().equals(type)` 查找处理器。

## Dependencies
- `StrategyHandler`、`IBaseEnum`。
- Lombok `@Data`。

## Notes
- 适用于“一组离散策略处理器”场景。
