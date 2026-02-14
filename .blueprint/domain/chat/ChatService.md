## Metadata
- file: `.blueprint/domain/chat/ChatService.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: ChatService
- 该文件用于描述 ChatService 的职责边界与协作关系。

## 2) 核心方法
- `createConversation()`
- `getConversationHistory()`
- `appendUserMessage()`
- `initAssistantMessage()`
- `findMessageByRunId()`

## 3) 具体方法
### 3.1 createConversation()
- 函数签名: `String createConversation(String userId, String agentId)`
- 入参:
  - `userId`: 用户ID
  - `agentId`: Agent ID
- 出参: 会话ID（UUID）
- 功能含义: 创建新会话，生成会话ID，设置初始标题（"New Chat MM-dd HH:mm"），持久化到数据库。
- 链路作用: 在 ChatController 中调用，初始化用户与 Agent 的对话会话。

### 3.2 getConversationHistory()
- 函数签名: `PageResult<Conversation> getConversationHistory(String userId, String agentId, Pageable pageable)`
- 入参:
  - `userId`: 用户ID
  - `agentId`: Agent ID
  - `pageable`: 分页参数
- 出参: 会话分页结果
- 功能含义: 查询用户与指定 Agent 的会话历史（分页），按更新时间倒序排列。
- 链路作用: 在 ChatController 中调用，提供会话列表查询接口。

### 3.3 appendUserMessage()
- 函数签名: `Message appendUserMessage(String conversationId, String content, Map<String, Object> metadata)`
- 入参:
  - `conversationId`: 会话ID
  - `content`: 消息内容
  - `metadata`: 元数据（如 executionId）
- 出参: Message 实体（保存的用户消息）
- 功能含义: 追加用户消息到会话，验证消息长度（最大10000字符），XSS 过滤，更新会话时间，持久化消息。
- 链路作用: 在 SchedulerService.startExecution() 中调用，保存用户输入消息。

### 3.4 initAssistantMessage()
- 函数签名: `String initAssistantMessage(String conversationId, String runId)`
- 入参:
  - `conversationId`: 会话ID
  - `runId`: 执行ID（executionId）
- 出参: 消息ID（UUID）
- 功能含义: 初始化 Assistant 消息（PENDING 状态），返回消息ID供流式更新使用。
- 链路作用: 在 SchedulerService.startExecution() 中调用，预创建 Assistant 消息占位符。

### 3.5 findMessageByRunId()
- 函数签名: `Optional<Message> findMessageByRunId(String runId)`
- 入参:
  - `runId`: 执行ID（executionId）
- 出参: Optional<Message>（关联的 Assistant 消息）
- 功能含义: 根据 runId（executionId）查询关联的 Assistant 消息，用于流式更新或完成消息。
- 链路作用: 在 SchedulerService.onExecutionComplete() 中调用，查找需要更新的消息。


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全方法签名与语义，从 ChatApplicationService.java 提取真实实现契约。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
