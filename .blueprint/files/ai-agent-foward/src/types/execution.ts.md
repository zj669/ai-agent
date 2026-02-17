# Blueprint Mirror: ai-agent-foward/src/types/execution.ts

## Source File
- Path: ai-agent-foward/src/types/execution.ts
- Type: .ts

## Responsibility
- 工作流执行类型定义文件，负责声明执行请求、SSE 事件与节点状态等类型契约。

## Key Symbols / Structure
- NodeType / ExecutionStatus / NodeExecutionStatus / ExecutionMode enum
- NodeTemplateOption / NodeTemplateField / NodeTemplateFieldGroup / NodeTemplate
- StartExecutionRequest
- SSEEventType 及 SSEConnectedEvent / SSEStartEvent / SSEUpdateEvent / SSEFinishEvent / SSEErrorEvent

## Dependencies
- TypeScript type system
- 执行服务与聊天实时流（services/executionService, hooks/useChat）

## Notes
- Auto-created blueprint mirror template.
