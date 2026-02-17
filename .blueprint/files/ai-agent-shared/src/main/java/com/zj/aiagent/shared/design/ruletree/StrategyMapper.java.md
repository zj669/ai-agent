# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/ruletree/StrategyMapper.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/ruletree/StrategyMapper.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/ruletree/StrategyMapper.java
- Type: .java

## Responsibility
- 定义规则树路由映射契约：根据请求与上下文选择下一处理器。
- 仅负责选择，不负责执行细节。

## Key Symbols / Structure
- `StrategyHandler<Request, Context, Result> get(Request request, Context dynamicContext)`。

## Dependencies
- `StrategyHandler`。

## Notes
- 可承载 if-else、规则表达式或配置化路由实现。
