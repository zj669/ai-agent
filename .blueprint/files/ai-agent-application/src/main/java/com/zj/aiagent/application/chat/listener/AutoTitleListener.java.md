# Blueprint Mirror: ai-agent-application/src/main/java/com/zj/aiagent/application/chat/listener/AutoTitleListener.java

## Metadata
- file: `ai-agent-application/src/main/java/com/zj/aiagent/application/chat/listener/AutoTitleListener.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-application/src/main/java/com/zj/aiagent/application/chat/listener/AutoTitleListener.java
- Type: .java

## Responsibility
- 监听消息追加事件，在会话仍为默认标题时异步生成并更新标题。
- 通过 WebSocket 向前端推送标题更新。

## Key Symbols / Structure
- `onMessageAppended(MessageAppendedEvent event)`
  - 读取会话
  - 判断标题是否 `New Chat*`
  - 生成简化标题（当前实现为 stub）
  - 保存会话并推送 `sendTitleUpdate`

## Dependencies
- `ConversationRepository`
- `WebSocketMessageService`
- 事件: `MessageAppendedEvent`
- Spring: `@Async`, `@EventListener`

## Notes
- 状态: 正常
- 标题生成逻辑当前为占位实现（`Smart Chat + conversationId前缀`）。
