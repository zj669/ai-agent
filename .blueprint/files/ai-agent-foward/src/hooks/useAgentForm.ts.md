# Blueprint Mirror: ai-agent-foward/src/hooks/useAgentForm.ts

## Source File
- Path: ai-agent-foward/src/hooks/useAgentForm.ts
- Type: .ts

## Responsibility
- Agent 表单业务 Hook，负责创建/编辑表单状态、字段校验与提交编排。

## Key Symbols / Structure
- useAgentForm

## Dependencies
- react
- services/agentService
- types/agent

## Notes
- 创建 Agent 成功后跳转 `"/agents/:id/workflow"`，直接进入拖拉拽工作流编辑页。
- Auto-created blueprint mirror template.
