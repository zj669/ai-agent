# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/ruletree/StrategyHandler.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/ruletree/StrategyHandler.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/ruletree/StrategyHandler.java
- Type: .java

## Responsibility
- 定义规则树节点处理器接口，接收请求与动态上下文执行业务。
- 提供默认空实现 `DEFAULT` 作为兜底处理器。

## Key Symbols / Structure
- `StrategyHandler DEFAULT = (T, D) -> null`。
- `Result apply(Request request, Context dynamicContext)`。

## Dependencies
- JDK 泛型。

## Notes
- 与 `StrategyMapper` 一起用于路由节点选择与执行。
