# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/strategymode/StrategyMapper.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/strategymode/StrategyMapper.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/strategymode/StrategyMapper.java
- Type: .java

## Responsibility
- 定义策略处理器类型映射契约，暴露处理器可处理的业务类型。
- 供策略工厂按类型检索具体处理器。

## Key Symbols / Structure
- `IBaseEnum getType()`：返回策略处理类型标识。

## Dependencies
- `IBaseEnum`。

## Notes
- 与 `StrategyHandler` 组合形成“类型 + 执行”双接口契约。
