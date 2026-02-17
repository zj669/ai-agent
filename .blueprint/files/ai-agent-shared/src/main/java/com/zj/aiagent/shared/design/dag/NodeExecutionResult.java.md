# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/dag/NodeExecutionResult.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/dag/NodeExecutionResult.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/dag/NodeExecutionResult.java
- Type: .java

## Responsibility
- 统一封装 DAG 节点执行结果，兼容普通输出、路由决策、人工等待三种类型。
- 提供静态工厂方法简化调用方构造。

## Key Symbols / Structure
- `enum ResultType { NORMAL, ROUTING, HUMAN_WAIT }`。
- 工厂：`content(...)`、`routing(...)`、`humanWait(...)`、`error(...)`。
- 判定：`isRoutingDecision()`、`isHumanWait()`。
- 读取：`getContent()/getRouteDecision()/getType()`。

## Dependencies
- `NodeRouteDecision`。

## Notes
- `toString()` 对长文本做截断，便于日志输出。
