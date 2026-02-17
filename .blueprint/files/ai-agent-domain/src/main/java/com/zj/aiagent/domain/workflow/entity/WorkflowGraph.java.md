# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/WorkflowGraph.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/WorkflowGraph.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/WorkflowGraph.java
- Type: .java
- Status: 正常

## Responsibility
- 工作流 DAG 结构实体，维护节点映射、边映射和边详情。
- 提供图遍历、入度计算、环检测与拓扑排序等执行前/执行期基础能力。

## Key Symbols / Structure
- 字段：`graphId`, `version`, `description`, `nodes`, `edges`, `edgeDetails`。
- 方法：
  - `getOutgoingEdges(nodeId)`：获取节点出边详情（含条件信息）。
  - `getStartNodes()`：获取所有 START 节点。
  - `getSuccessors(nodeId)` / `getPredecessors(nodeId)`：邻接查询。
  - `calculateInDegrees()`：入度统计。
  - `hasCycle()`：DFS 检测环。
  - `topologicalSort()`：基于入度的拓扑排序。

## Dependencies
- `Node`, `Edge`
- Java 集合与 Stream API

## Notes
- 作为 `Execution` 聚合的重要组成，承载可执行拓扑语义。
