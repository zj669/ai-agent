# NodeExecutors (Infrastructure Adapters) Blueprint

## 职责契约
- **做什么**: 实现 Domain 层的 NodeExecutorStrategy 端口，为每种节点类型提供具体的执行逻辑
- **不做什么**: 不管理执行生命周期；不决定调度顺序；不直接被 Controller 调用

## 实现清单

| 节点类型 | 实现类 | 核心依赖 | 说明 |
|---------|--------|---------|------|
| START | StartNodeExecutorStrategy | - | 起始节点，透传输入参数到输出 |
| END | EndNodeExecutorStrategy | - | 结束节点，汇总上游输出 |
| LLM | LlmNodeExecutorStrategy | ChatModelPort, StreamPublisher | 调用 OpenAI 兼容 API，支持流式输出，注入 LTM/STM/Awareness |
| CONDITION | ConditionNodeExecutorStrategy | SpEL / ChatClient | 条件路由，支持 EXPRESSION 和 LLM 两种模式 |
| HTTP | HttpNodeExecutorStrategy | RestClient | HTTP 请求节点 |
| TOOL | ToolNodeExecutorStrategy | MCP 工具调用 | MCP 工具节点 |

## LlmNodeExecutorStrategy 详细设计

### 执行流程
1. 从 NodeConfig 读取模型配置 (model, baseUrl, apiKey, temperature 等)
2. 动态创建 OpenAiChatModel 实例（不使用 Spring AI 自动配置）
3. 构建 System Prompt，注入:
   - LTM: `context.longTermMemories` → 系统知识
   - Awareness: `context.getExecutionLogContent()` → 执行进度
4. 构建 Message Chain:
   - STM: `context.chatHistory` → 历史消息
   - 当前用户输入
5. 流式调用 ChatClient，通过 StreamPublisher 推送 delta
6. 收集完整响应，返回 NodeExecutionResult.success

### 模板占位符替换 (buildUserPrompt)
- 支持两种占位符格式：`#{key}` (原有) 和 `{{key}}` (Mustache 风格)
- 遍历 resolvedInputs，对每个 key 同时替换两种格式
- **跳过规则**: `__` 前缀的 key 为内部变量（如 `__outgoingEdges__`），不参与替换
- **null 值处理**: 值为 null 时跳过替换，保留原始占位符文本
- 无模板时回退到 `resolvedInputs.get("input")`

### 流式输出
- 通过 `StreamPublisher.publishDelta(delta)` 逐 token 推送
- `isFinalOutputNode` 决定 renderMode: MARKDOWN(聊天) / THOUGHT(思维链)

## HttpNodeExecutorStrategy 模板替换

### resolveTemplate 方法
- 用于 URL、Headers、Body 等所有模板字段的占位符替换
- 支持两种占位符格式：`#{key}` (原有) 和 `{{key}}` (Mustache 风格)
- **跳过规则**: `__` 前缀的 key 为内部变量，不参与替换
- **null 值处理**: 值为 null 时跳过替换，保留原始占位符文本
- template 为 null 时直接返回 null

## ConditionNodeExecutorStrategy 详细设计

### EXPRESSION 模式 (routingStrategy = "EXPRESSION")
1. 从 `resolvedInputs` 获取出边列表 (`__outgoingEdges__`)
2. 构建 SpEL EvaluationContext，注入所有 resolvedInputs
3. 遍历非默认边，评估 `edge.condition` (SpEL 表达式)
4. 首个为 true 的边胜出 → `NodeExecutionResult.routing(targetId, outputs)`

### LLM 模式 (routingStrategy = "LLM")
1. 从 NodeConfig 读取模型配置
2. 构建 Prompt: 上下文 + 可选分支描述
3. 调用 ChatClient，解析返回的目标节点 ID
4. 验证返回 ID 是否在有效边列表中

### 兜底逻辑
- 无条件命中 → 查找 `edge.isDefault()` 的边
- 无默认边 → 使用第一条边
- 无任何边 → 返回 `NodeExecutionResult.failed`

## 流式推送实现

### RedisSseStreamPublisher
- 实现 StreamPublisher 端口
- 持有 `RedisSsePublisher`(Redis Pub/Sub) + `ObjectMapper` + `StreamContext`
- 构建 `SseEventPayload` 推送到 Redis 频道

### RedisSseStreamPublisherFactory
- 实现 StreamPublisherFactory 端口
- `create(StreamContext)` → 返回绑定上下文的 RedisSseStreamPublisher 实例

### SSE 事件结构 (SseEventPayload)
| 字段 | 说明 |
|------|------|
| executionId | 执行ID |
| nodeId | 节点ID |
| parentId | 父节点ID (并行组) |
| nodeType | 节点类型 |
| eventType | START / UPDATE / FINISH / ERROR |
| status | ExecutionStatus |
| timestamp | 时间戳 |
| payload.title | 节点名称 |
| payload.content | 完整内容 |
| payload.delta | 增量内容 |
| payload.isThought | 是否为思考过程 |
| payload.renderMode | MARKDOWN / THOUGHT / TEXT / JSON_EVENT |

## 持久化实现

### ExecutionRepositoryImpl
- 实现 ExecutionRepository 端口
- Execution 序列化为 JSON 存储到 Redis (临时) + MySQL (最终)

### CheckpointRepositoryImpl
- 实现 CheckpointRepository 端口 (未在端口列表中显式定义，通过 Redis 直接操作)
- Key 格式: `execution:checkpoint:{executionId}`

### WorkflowNodeExecutionLogRepositoryImpl
- 实现 WorkflowNodeExecutionLogRepository 端口
- 持久化到 MySQL `workflow_node_execution_log` 表

### HumanReviewRepositoryImpl
- 实现 HumanReviewRepository 端口
- 持久化到 MySQL `workflow_human_review_record` 表

### HumanReviewQueuePortImpl
- 实现 HumanReviewQueuePort 端口
- Redis Set: `human_review:pending`

### WorkflowCancellationPortImpl
- 实现 WorkflowCancellationPort 端口
- Redis 标记取消状态

## 依赖拓扑
- **上游**: SchedulerService (通过 NodeExecutorStrategy 端口)
- **下游**: ChatModelPort(LLM调用), RedisSsePublisher(Redis Pub/Sub), MySQL Mapper, Redis

## 设计约束
- LLM 节点通过用户配置的模型参数动态创建 ChatModel 实例，不使用 Spring AI 自动配置
- 所有执行器通过 `@Qualifier("nodeExecutorThreadPool")` 注入线程池
- 条件节点的出边通过 `__outgoingEdges__` 键注入到 resolvedInputs
- 执行日志记录 renderMode: `HIDDEN`(不展示) / `THOUGHT`(思维链) / `MESSAGE`(聊天消息)

## 变更日志
- [初始] 从现有代码逆向生成蓝图
- [2026-02-08] 补充所有执行器类型、条件路由双模式、流式推送实现、持久化实现、SSE 事件结构
- [2026-02-09] LlmNodeExecutorStrategy 和 HttpNodeExecutorStrategy 增加 {{key}} Mustache 风格占位符替换支持
