# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Edge.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Edge.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Edge.java
- Type: .java
- Status: 正常

## Responsibility
- 表示工作流图中节点间连接关系，支持普通依赖边、条件边与默认边。
- 封装条件路由语义判断，供条件节点分支选择与剪枝逻辑使用。

## Key Symbols / Structure
- 字段：`edgeId`, `source`, `target`, `condition`, `edgeType`。
- 方法：
  - `isConditional()`：判定是否条件边（类型或 condition 非空）。
  - `isDefault()`：判定是否默认兜底边（`DEFAULT` 或 `condition=default`）。
- 枚举：`EdgeType { DEPENDENCY, CONDITIONAL, DEFAULT }`。

## Dependencies
- 内部枚举 `EdgeType`

## Notes
- 默认边判定逻辑显式避免将普通空条件边误识别为 default。
