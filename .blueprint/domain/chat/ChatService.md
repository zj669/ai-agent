# ChatService Blueprint

## 职责契约
- **做什么**: 管理对话系统——会话(Conversation)的创建与查询、消息(Message)的持久化与检索、思维链(ThinkingContent)和引用源(Citation)的关联
- **不做什么**: 不负责 LLM 调用（那是 NodeExecutor 的职责）；不负责工作流执行；不负责流式输出

## 核心实体

### Conversation
- 会话实体，关联 userId 和 agentId
- 持有消息列表

### Message
- 消息实体，包含角色(user/assistant)、内容、思维链、引用源、元数据
- 支持 renderMode 控制前端展示方式

## 接口摘要

| 方法 | 输入 | 输出 | 副作用 | 约束 |
|------|------|------|--------|------|
| createConversation | userId, agentId | Conversation | 写DB | - |
| listConversations | userId, agentId | List<Conversation> | 无 | - |
| saveMessage | conversationId, Message | Message | 写DB | conversationId 必须存在 |
| getMessages | conversationId | List<Message> | 无 | 按时间排序 |
| updateMessage | messageId, updates | Message | 写DB | 仅更新允许的字段 |

## 依赖拓扑
- **上游**: ChatController, ChatApplicationService, SchedulerService(工作流完成时更新消息)
- **下游**: ConversationRepository(端口)

## 领域事件
- 发布: 无
- 监听: 无（由 SchedulerService 在工作流完成时主动调用更新消息）

## 设计约束
- 会话存储在 conversations 表，消息存储在 messages 表
- Message 的 thinkingContent 和 citations 以 JSON 字段存储
- 工作流执行完成后，SchedulerService 会回写 assistant 消息到对应会话

## 变更日志
- [初始] 从现有代码逆向生成蓝图
