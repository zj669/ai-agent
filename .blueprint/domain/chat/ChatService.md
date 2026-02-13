# ChatService Blueprint

## 职责契约
- **做什么**: 管理对话系统——会话(Conversation)的创建与查询、消息(Message)的持久化与检索、思维链(ThoughtStep)和引用源(Citation)的关联、消息状态流转管理
- **不做什么**: 不负责 LLM 调用（那是 NodeExecutor 的职责）；不负责工作流执行；不负责流式输出（由 StreamPublisher 负责）

## 核心实体

### Conversation (聚合根)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 会话ID (UUID) |
| userId | String | 用户ID |
| agentId | String | 智能体ID |
| title | String | 会话标题（可自动生成） |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |

**领域方法**:
- `updateTitle(String)`: 更新会话标题
- `markUpdated()`: 标记会话已更新（刷新 updatedAt）

### Message (实体)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 消息ID (UUID) |
| conversationId | String | 所属会话ID |
| role | MessageRole | 角色: USER, ASSISTANT, SYSTEM |
| content | String | 消息内容 |
| thoughtProcess | List\<ThoughtStep\> | 思维链过程（递归结构） |
| citations | List\<Citation\> | 引用源列表 |
| metadata | Map\<String, Object\> | 元数据（runId, tokenCount等） |
| status | MessageStatus | 消息状态 |
| createdAt | LocalDateTime | 创建时间 |

**消息状态流转**:
```
PENDING → STREAMING → COMPLETED
                   ↘ FAILED
```

**领域方法**:
- `initAssistant(conversationId, runId)`: 初始化 Assistant 消息（PENDING 状态）

## 接口摘要

### ChatApplicationService

| 方法 | 输入 | 输出 | 副作用 | 约束 |
|------|------|------|--------|------|
| createConversation | userId, agentId | String (conversationId) | 写DB | - |
| getConversationHistory | userId, agentId, Pageable | PageResult\<Conversation\> | 无 | 按 updatedAt 倒序 |
| appendUserMessage | conversationId, content, metadata | Message | 写DB, 更新会话时间 | conversationId 必须存在 |
| initAssistantMessage | conversationId, runId | String (messageId) | 写DB | 创建 PENDING 状态消息 |
| finalizeMessage | messageId, content, thoughtProcess, status | void | 写DB | 更新消息内容和状态 |
| findMessageByRunId | runId | Optional\<Message\> | 无 | 通过 metadata.runId 查询 |
| getMessages | conversationId, Pageable | List\<Message\> | 无 | 按 createdAt 正序 |
| deleteConversation | conversationId | void | 软删除会话和消息 | 事务保护 |

### ConversationRepository (端口)

| 方法 | 输入 | 输出 | 说明 |
|------|------|------|------|
| save | Conversation | Conversation | 保存或更新会话 |
| findById | String | Optional\<Conversation\> | 查询会话（排除已删除） |
| deleteById | String | void | 软删除会话 |
| findByUserIdAndAgentId | userId, agentId, Pageable | PageResult\<Conversation\> | 分页查询用户会话列表 |
| saveMessage | Message | Message | 保存或更新消息 |
| findMessageById | String | Optional\<Message\> | 查询消息详情 |
| findMessagesByConversationId | conversationId, Pageable | List\<Message\> | 查询会话消息历史 |
| findMessageByRunId | String | Optional\<Message\> | 根据 runId 查询消息 |
| deleteMessagesByConversationId | String | void | 删除会话的所有消息 |

## 依赖拓扑
- **上游**: ChatController, SchedulerService (工作流完成时调用 finalizeMessage)
- **下游**: ConversationRepository (端口) → MybatisConversationRepository (实现)

## 数据流

### 用户发送消息流程
```
ChatController.appendUserMessage
  → ChatApplicationService.appendUserMessage
    → ConversationRepository.save (更新会话时间)
    → ConversationRepository.saveMessage (保存用户消息)
```

### 工作流执行消息流程
```
SchedulerService.startExecution
  → ChatApplicationService.initAssistantMessage (创建 PENDING 消息)
  → NodeExecutor 执行 (通过 StreamPublisher 推送增量)
  → SchedulerService.onExecutionComplete
    → ChatApplicationService.finalizeMessage (更新为 COMPLETED/FAILED)
```

## 安全约束
- **权限校验**: Controller 层必须验证用户是否有权访问会话
- **数据隔离**: 查询时必须过滤 `is_deleted=1` 的记录
- **事务保护**: 删除操作必须在事务中执行，保证会话和消息同步删除

## 性能优化
- **索引**: `idx_user_agent` (user_id, agent_id), `idx_conversation_created` (conversation_id, created_at)
- **分页**: 使用 MyBatis Plus 的 Page 对象，避免全表扫描
- **缓存**: 高频查询的会话列表可考虑 Redis 缓存（当前未实现）

## 设计约束
- 会话存储在 `conversations` 表，消息存储在 `messages` 表
- Message 的 `thought_process` 和 `citations` 以 JSON 字段存储
- 软删除策略：`conversations.is_deleted`, `messages` 物理删除（待优化为软删除）
- 工作流执行完成后，SchedulerService 回写 assistant 消息到对应会话

## 变更日志
- [2026-02-10] **架构优化**: 完善蓝图文档，添加安全约束、性能优化、数据流说明
- [初始] 从现有代码逆向生成蓝图
