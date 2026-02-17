# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/dag/NodeRouteDecision.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/dag/NodeRouteDecision.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/dag/NodeRouteDecision.java
- Type: .java

## Responsibility
- 表达条件节点执行后的路由决策结果，决定后续执行分支与是否终止。
- 统一封装“下一节点集合/停止标记/回环标记”三类控制语义。

## Key Symbols / Structure
- 工厂：`continueWith(Set<String>)`、`continueWith(String)`、`continueAll()`。
- 终止：`stop()`。
- 回环：`loopBack(String targetNodeId)`。
- 读取：`getNextNodeIds()/isStopExecution()/isLoopBack()`。

## Dependencies
- JDK `Set`、`Collections`。

## Notes
- `continueAll()` 通过 `nextNodeIds=null` 语义表示“由框架选择全部候选”。
