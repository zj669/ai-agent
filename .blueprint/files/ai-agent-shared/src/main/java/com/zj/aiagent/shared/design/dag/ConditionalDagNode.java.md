# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/dag/ConditionalDagNode.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/dag/ConditionalDagNode.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/dag/ConditionalDagNode.java
- Type: .java

## Responsibility
- 定义条件 DAG 节点契约：执行返回路由决策而非普通业务结果。
- 额外暴露候选下游集合，供调度器做分支裁剪与校验。

## Key Symbols / Structure
- 继承 `DagNode<C, NodeRouteDecision>`。
- `execute(C context)`：返回 `NodeRouteDecision`。
- `getCandidateNextNodes()`：返回所有候选下游节点 ID。

## Dependencies
- `DagNode`、`NodeRouteDecision`、`DagNodeExecutionException`。

## Notes
- 候选节点不等同依赖节点，语义上属于“可路由目标集合”。
