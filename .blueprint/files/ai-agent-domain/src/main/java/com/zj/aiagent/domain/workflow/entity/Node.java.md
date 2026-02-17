# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Node.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Node.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Node.java
- Type: .java
- Status: 正常

## Responsibility
- 工作流图中的节点实体，承载节点静态定义（类型、输入输出映射、依赖关系、位置信息、配置）。
- 为执行引擎提供节点语义判断能力（起始/结束/条件/是否需人工审核）。

## Key Symbols / Structure
- 字段：`nodeId`, `name`, `type`, `config`, `inputs`, `outputs`, `dependencies`, `successors`, `position`。
- 方法：
  - `requiresHumanReview()`：判断节点是否开启人工审核。
  - `isStartNode()` / `isEndNode()` / `isConditionNode()`：节点类型判定。
- 内部类：`Position { x, y }`，用于前端画布布局。

## Dependencies
- `NodeConfig`
- `NodeType`
- Java 集合：`Map`, `Set`

## Notes
- 该类为领域层纯实体，不包含基础设施依赖。
