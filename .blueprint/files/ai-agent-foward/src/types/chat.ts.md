# Blueprint Mirror: ai-agent-foward/src/types/chat.ts

## Source File
- Path: ai-agent-foward/src/types/chat.ts
- Type: .ts

## Responsibility
- 对话领域类型定义文件，负责声明会话、消息与流式事件的数据类型契约。

## Key Symbols / Structure
- MessageRole / MessageStatus
- ThoughtStep / Citation
- Message / Conversation
- CreateConversationRequest / SendMessageRequest
- PageResult<T> / SSEPayload

## Dependencies
- `types/auth`（复用 `ApiResponse` 通用响应结构）
- TypeScript 类型系统
- 对话服务与状态层（`services/chatService`, `stores/chatStore`）

## Notes
- Auto-created blueprint mirror template.
