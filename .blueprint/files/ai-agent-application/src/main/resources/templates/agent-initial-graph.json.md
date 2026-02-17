# Blueprint Mirror: ai-agent-application/src/main/resources/templates/agent-initial-graph.json

## Metadata
- file: `ai-agent-application/src/main/resources/templates/agent-initial-graph.json`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-application/src/main/resources/templates/agent-initial-graph.json
- Type: .json

## Responsibility
- 新建 Agent 时使用的初始工作流图模板。
- 提供最小可执行 DAG：`START -> END`。

## Key Symbols / Structure
- 顶层字段：`dagId/version/description/memory/startNodeId/nodes/edges`
- 默认节点
  - `start` (`nodeType=START`): 接收 `inputMessage`
  - `end` (`nodeType=END`): 输出 `finalResult`
- 默认边
  - `edge-start-end`: `source=start`, `target=end`, `edgeType=DEPENDENCY`

## Dependencies
- 被 `AgentApplicationService.init/generateInitialGraphJson` 读取与注入 `dagId`。

## Notes
- 状态: 正常
- 该模板作为工作流编辑器初始草稿，不含业务节点。
