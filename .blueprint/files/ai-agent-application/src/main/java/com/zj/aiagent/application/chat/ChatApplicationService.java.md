# Blueprint Mirror: ai-agent-application/src/main/java/com/zj/aiagent/application/chat/ChatApplicationService.java

## Metadata
- file: `ai-agent-application/src/main/java/com/zj/aiagent/application/chat/ChatApplicationService.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-application/src/main/java/com/zj/aiagent/application/chat/ChatApplicationService.java
- Type: .java

## Responsibility
- 编排会话与消息用例：会话创建/查询/删除、用户消息追加、Assistant 消息初始化与收敛。
- 提供会话访问鉴权辅助方法，统一消息长度与 XSS 过滤策略。

## Key Symbols / Structure
- 会话
  - `createConversation(userId, agentId)`
  - `getConversationHistory(userId, agentId, pageable)`
  - `deleteConversation(conversationId)`
  - `deleteConversationWithAuth(conversationId, userId)`
- 消息
  - `appendUserMessage(conversationId, content)`
  - `appendUserMessage(conversationId, content, metadata)`
  - `initAssistantMessage(conversationId, runId)`
  - `finalizeMessage(messageId, content, thoughtProcess, status)`
  - `getMessages(conversationId, pageable)`
  - `getMessagesWithAuth(conversationId, userId, pageable)`
  - `findMessageByRunId(runId)`
- 权限
  - `hasConversationAccess(conversationId, userId)`

## Dependencies
- Domain: `ConversationRepository`, `Message`, `Conversation`, `ThoughtStep`, `MessageRole`, `MessageStatus`
- Shared: `PageResult`, `XssFilterUtil`
- Spring: `ApplicationEventPublisher`, `@Transactional`

## Notes
- 状态: 正常
- `MAX_MESSAGE_LENGTH=10000`；用户消息写入前进行 XSS 过滤与空值校验。
