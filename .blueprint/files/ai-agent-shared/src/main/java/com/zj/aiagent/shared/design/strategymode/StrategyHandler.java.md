# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/strategymode/StrategyHandler.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/strategymode/StrategyHandler.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/strategymode/StrategyHandler.java
- Type: .java

## Responsibility
- 定义策略模式执行器接口：在标识类型基础上提供 `apply` 执行入口。
- 不做路由决策，路由由工厂/调用方完成。

## Key Symbols / Structure
- 继承 `StrategyMapper`。
- `Result apply(Request request)`：执行策略。

## Dependencies
- `StrategyMapper`。

## Notes
- 使用泛型解耦请求与返回类型。
