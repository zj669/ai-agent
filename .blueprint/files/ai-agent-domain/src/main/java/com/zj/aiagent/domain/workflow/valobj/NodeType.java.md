# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/NodeType.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/NodeType.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/NodeType.java
- Type: .java

## Responsibility
- 定义工作流节点类型枚举，作为图节点语义分类与执行策略路由键。
- 约束 DAG 中可出现的节点种类，支撑 `NodeExecutorFactory`/`NodeExecutorStrategy` 分发。

## Key Symbols / Structure
- `START`: 起始节点。
- `END`: 结束节点。
- `LLM`: 大模型调用节点。
- `HTTP`: HTTP 请求节点。
- `CONDITION`: 条件路由节点。
- `TOOL`: 工具/MCP 节点。

## Dependencies
- 无外部依赖（纯领域枚举）。

## Notes
- 节点类型与执行策略是一一映射关系；新增类型时需同步补充策略实现及工厂注册，避免调度阶段无法分发。
