# Blueprint Mirror: ai-agent-application/src/main/java/com/zj/aiagent/application/chat/event/MessageAppendedEvent.java

## Metadata
- file: `ai-agent-application/src/main/java/com/zj/aiagent/application/chat/event/MessageAppendedEvent.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-application/src/main/java/com/zj/aiagent/application/chat/event/MessageAppendedEvent.java
- Type: .java

## Responsibility
- 聊天消息追加后的应用事件载体，供监听器异步处理（如自动标题）。

## Key Symbols / Structure
- 字段: `message`
- 构造: `MessageAppendedEvent(Message message)`

## Dependencies
- Spring `ApplicationEvent`
- Domain `Message`

## Notes
- 状态: 正常
- 当前 `ChatApplicationService` 中事件发布代码为注释状态，事件类型仍保留。
