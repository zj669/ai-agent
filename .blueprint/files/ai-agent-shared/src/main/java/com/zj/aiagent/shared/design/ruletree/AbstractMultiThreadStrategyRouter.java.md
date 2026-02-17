# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/ruletree/AbstractMultiThreadStrategyRouter.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/ruletree/AbstractMultiThreadStrategyRouter.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/ruletree/AbstractMultiThreadStrategyRouter.java
- Type: .java

## Responsibility
- 定义带异步预处理能力的规则树路由基类。
- 执行流程固定为：`multiThread` 异步准备 → `doApply` 业务执行。
- 同时保留 `router` 路由选择机制与默认处理器兜底。

## Key Symbols / Structure
- `router(request, dynamicContext)`：按路由选择处理器执行。
- `apply(request, dynamicContext)`：模板方法，先并行准备再执行业务。
- `multiThread(...)`：子类实现异步加载。
- `doApply(...)`：子类实现业务受理。

## Dependencies
- `StrategyMapper`、`StrategyHandler`。
- JDK 并发异常类型。
- Lombok `@Getter/@Setter`。

## Notes
- 适合需要先并发聚合数据再决策的复杂规则树。
