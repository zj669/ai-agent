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
| CONDITION | ConditionNodeExecutorStrategy | ConditionEvaluatorPort, ObjectMapper, ChatClient | 条件路由，EXPRESSION 模式使用结构化评估器，LLM 模式使用语义路由 |
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

### 依赖注入
- `ConditionEvaluatorPort` — 结构化条件评估（替代直接 SpEL）
- `ObjectMapper` — Jackson JSON 反序列化 branches 配置
- `Executor` — 异步线程池
- `RestClient.Builder` — LLM 模式 HTTP 调用

### EXPRESSION 模式 (routingStrategy = "EXPRESSION")
1. 从 `NodeConfig.properties["branches"]` 解析 `List<ConditionBranch>`（Jackson 反序列化）
2. 如果 branches 为空/null，从 `__outgoingEdges__` 做旧模型兼容转换（`convertLegacyEdgesToBranches`）
3. 从 `resolvedInputs["__context__"]` 获取 `ExecutionContext`
4. 调用 `ConditionEvaluatorPort.evaluate(branches, context)` 获取命中分支
5. 返回 `NodeExecutionResult.routing(branch.targetNodeId, outputs)`

### 旧模型兼容转换 (convertLegacyEdgesToBranches)
- CONDITIONAL 边 → 非 default 分支（SpEL 条件无法解析为结构化模型，作为 default 处理）
- DEFAULT 边 → default 分支
- 保证转换后恰好有一个 default 分支

### LLM 模式 (routingStrategy = "LLM")
1. 从 `NodeConfig.properties["branches"]` 解析 `List<ConditionBranch>`（复用 `parseBranchesFromConfig`）
2. 如果 branches 为空，从 `__outgoingEdges__` 做旧模型兼容转换（复用 `convertLegacyEdgesToBranches`）
3. 构建 Prompt: 上下文 + `branch.description` 描述各非 default 分支
4. 调用 ChatClient，解析返回的目标节点 ID（trim + case-insensitive 匹配）
5. 匹配失败时发送澄清 prompt 重试一次（列出所有有效目标 ID）
6. 重试仍失败则使用 default 分支
（注：LLM 模式已在 Task 6.2 中完成重构，使用 branch.description 替代 edge.condition）

### 兜底逻辑
- branches 配置为空且无旧模型边 → 返回 `NodeExecutionResult.failed`
- ConditionEvaluatorPort 校验失败（无 default / 多 default）→ 抛出 ConditionConfigurationException

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
- 条件节点 EXPRESSION 模式通过 `ConditionEvaluatorPort` 评估结构化条件分支，不再直接使用 SpEL
- 条件节点的出边通过 `__outgoingEdges__` 键注入到 resolvedInputs（仅用于旧模型兼容）
- 条件节点的 `ExecutionContext` 通过 `__context__` 键注入到 resolvedInputs
- 执行日志记录 renderMode: `HIDDEN`(不展示) / `THOUGHT`(思维链) / `MESSAGE`(聊天消息)

## StructuredConditionEvaluator 详细设计

### 职责
- 实现 ConditionEvaluatorPort 端口，基于结构化条件模型（ConditionBranch/Group/Item）评估分支
- 替代直接使用 SpEL 表达式的条件评估方式，提供类型安全的比较

### 核心方法
| 方法 | 输入 | 输出 | 说明 |
|------|------|------|------|
| evaluate | List\<ConditionBranch\>, ExecutionContext | ConditionBranch | 按 priority 排序评估，首个命中胜出，无命中返回 default |
| validateBranches | List\<ConditionBranch\> | void | 校验恰好一个 default 分支 |
| resolveOperand | String, ExecutionContext | Object | 解析 `nodes.{nodeId}.{key}` 和 `inputs.{key}` 变量引用 |
| compareValues | Object, ComparisonOperator, Object | boolean | 类型安全比较（String/Number/null） |
| evaluateGroup | ConditionGroup, ExecutionContext | boolean | 按 AND/OR 逻辑评估组内条件 |

### 变量引用格式
- `nodes.{nodeId}.{outputKey}` → 从 ExecutionContext.nodeOutputs 获取
- `inputs.{key}` → 从 ExecutionContext.inputs 获取
- 不存在的变量 → 返回 null，条件视为不满足

### 错误处理
- 无 default 分支 → 抛出 ConditionConfigurationException
- 多个 default 分支 → 抛出 ConditionConfigurationException
- ConditionItem.operator 为 null → 跳过该 item，视为 false，log WARN
- 变量引用不存在 → 条件视为不满足，log WARN
- 类型不兼容比较 → 条件视为不满足，log WARN

### 依赖
- **上游**: ConditionNodeExecutorStrategy (将在后续 Task 6 中重构)
- **下游**: ExecutionContext (读取变量)

## MilvusVectorStoreAdapter 类型转换

### 职责
- 实现 VectorStore 端口，适配 Spring AI 的 Milvus VectorStore
- 负责 domain 层值对象与 Spring AI 类型之间的转换

### 类型转换逻辑
| 方向 | 源类型 | 目标类型 | 转换方法 |
|------|--------|---------|---------|
| Domain → Spring AI | com.zj.aiagent.domain.memory.valobj.Document | org.springframework.ai.document.Document | toDomainDocument() |
| Spring AI → Domain | org.springframework.ai.document.Document | com.zj.aiagent.domain.memory.valobj.Document | toSpringAiDocument() |
| Domain → Spring AI | com.zj.aiagent.domain.memory.valobj.SearchRequest | org.springframework.ai.vectorstore.SearchRequest | toSpringAiSearchRequest() |

### 转换细节
- Document 转换：id, content, metadata 字段一一对应
- SearchRequest 转换：query, topK, filterExpression, similarityThreshold 字段映射
- 所有公开方法接收/返回 domain 层类型，内部调用 Spring AI 时进行转换

## ExpressionResolverPort (新增)

### 职责
- 提供 SpEL 表达式解析能力，替代 ExecutionContext 中的 resolve() 方法
- 支持 `#{inputs.key}`, `#{nodeId.output.key}`, `#{sharedState.key}` 格式

### 实现位置
- StructuredConditionEvaluator 内置 SpEL 解析逻辑
- 其他需要表达式解析的 infrastructure 组件可复用此逻辑

### 表达式格式
- `#{inputs.key}` — 引用全局输入
- `#{nodeId.output.key}` — 引用指定节点的输出
- `#{sharedState.key}` — 引用共享状态
- 非表达式字符串（不以 `#{` 开头）直接返回原值

## 变更日志
- [初始] 从现有代码逆向生成蓝图
- [2026-02-08] 补充所有执行器类型、条件路由双模式、流式推送实现、持久化实现、SSE 事件结构
- [2026-02-09] LlmNodeExecutorStrategy 和 HttpNodeExecutorStrategy 增加 {{key}} Mustache 风格占位符替换支持
- [2026-02-10] **架构重构**:
  - 新增 ExpressionResolverPort 端口，将 SpEL 解析逻辑从 domain 层迁移到 infrastructure 层
  - MilvusVectorStoreAdapter 新增类型转换逻辑（domain Document ↔ Spring AI Document）
  - StructuredConditionEvaluator 内置 SpEL 解析能力，支持 `#{expression}` 格式
- [2026-07] 新增 StructuredConditionEvaluator，实现 ConditionEvaluatorPort 端口，基于结构化条件模型评估分支
- [2026-07] 重构 ConditionNodeExecutorStrategy EXPRESSION 模式：注入 ConditionEvaluatorPort + ObjectMapper，从 NodeConfig.properties["branches"] 解析分支，移除直接 SpEL 评估，保留旧模型兼容转换
- [2026-07] 重构 ConditionNodeExecutorStrategy LLM 模式：使用 branch.description 构建 Prompt（替代 edge.condition），添加重试逻辑（无效 ID → 澄清 prompt 重试一次 → default 分支兜底），响应解析 trim + case-insensitive 匹配
