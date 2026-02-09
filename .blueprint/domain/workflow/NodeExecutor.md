# NodeExecutor Blueprint

## 职责契约
- **做什么**: 定义节点执行的策略接口，每种节点类型有独立的执行器实现；提供流式推送端口和工厂
- **不做什么**: 不管理执行生命周期（那是 Execution 聚合根的职责）；不决定节点调度顺序（那是 SchedulerService 的职责）

## 核心端口

### NodeExecutorStrategy (策略接口)
| 方法 | 输入 | 输出 | 说明 |
|------|------|------|------|
| executeAsync | Node, Map resolvedInputs, StreamPublisher | CompletableFuture\<NodeExecutionResult\> | 异步执行节点 |
| getSupportedType | - | NodeType | 返回支持的节点类型 |
| supportsStreaming | - | boolean | 是否支持流式输出（默认 false） |

### StreamPublisher (流式推送端口)
| 方法 | 输入 | 输出 | 说明 |
|------|------|------|------|
| publishStart | - | void | 推送节点开始事件 |
| publishDelta | String delta | void | 推送增量文本（打字机效果），自动根据 isFinalOutputNode 决定 renderMode |
| publishDelta | String delta, boolean isThought | void | 推送增量文本，显式指定是否为思考过程 |
| publishThought | String thought | void | 推送思考过程 |
| publishFinish | NodeExecutionResult | void | 推送节点完成事件 |
| publishError | String errorMessage | void | 推送错误信息 |
| publishData | Object data, String renderMode | void | 推送结构化数据 (JSON/TABLE) |
| publishEvent | String eventType, Map payload | void | 推送自定义事件 (JSON_EVENT) |

### StreamPublisherFactory (工厂端口)
| 方法 | 输入 | 输出 | 说明 |
|------|------|------|------|
| create | StreamContext | StreamPublisher | 根据流式上下文创建推送器实例 |

### StreamContext (值对象)
- 纯数据载体，不持有接口引用
- 字段: `executionId`, `nodeId`, `parentId`(并行组ID), `nodeType`, `nodeName`, `isFinalOutputNode`
- `isFinalOutputNode`: true → renderMode=MARKDOWN (聊天消息); false → renderMode=THOUGHT (思维链)

## 节点执行器实现清单

| 节点类型 | 实现类 | 核心依赖 | 说明 |
|---------|--------|---------|------|
| START | StartNodeExecutorStrategy | - | 起始节点，透传输入 |
| END | EndNodeExecutorStrategy | - | 结束节点，汇总输出 |
| LLM | LlmNodeExecutorStrategy | ChatModelPort, StreamPublisher | 调用 OpenAI 兼容 API，支持流式输出，注入 LTM/STM/Awareness |
| CONDITION | ConditionNodeExecutorStrategy | SpEL / ChatClient | 两种路由模式: EXPRESSION(SpEL表达式) / LLM(语义理解) |
| HTTP | HttpNodeExecutorStrategy | RestClient | HTTP 请求节点 |
| TOOL | ToolNodeExecutorStrategy | MCP 工具调用 | MCP 工具节点 |

### 条件节点路由模式
- **EXPRESSION**: 遍历出边，用 SpEL 评估每条边的 `condition` 字段，首个为 true 的边胜出
- **LLM**: 构建 Prompt 描述上下文和可选分支，让 LLM 返回目标节点 ID
- **兜底**: 无条件命中时使用 `isDefault()` 的边，或第一条边

## NodeExecutionResult (值对象)
| 工厂方法 | 说明 |
|---------|------|
| success(outputs) | 执行成功 |
| failed(errorMessage) | 执行失败 |
| routing(branchId, outputs) | 条件路由（携带选中分支ID） |
| paused(phase) / paused(phase, outputs) | 暂停等待审核 |

判断方法: `isSuccess()`, `isPaused()`, `isRouting()`

## 依赖拓扑
- **上游**: SchedulerService (通过策略模式调用)
- **下游**: ChatModelPort(LLM调用), StreamPublisher(流式输出), WorkflowNodeExecutionLogRepository(日志记录)

## 设计约束
- 策略模式: 通过 `getSupportedType()` 匹配节点类型，Spring 自动注入所有实现
- 所有执行器必须返回 CompletableFuture，通过 `nodeExecutorThreadPool` 异步执行
- LLM 节点通过用户配置的模型参数动态创建 ChatModel 实例，不使用 Spring AI 自动配置
- 执行日志（输入、输出、耗时、renderMode）必须写入 `workflow_node_execution_log` 表
- StreamPublisher 通过 Factory 模式创建，每个节点执行获得独立实例（绑定 StreamContext）

## 变更日志
- [初始] 从现有代码逆向生成蓝图
- [2026-02-08] 补充完整执行器清单、条件路由模式、StreamContext/StreamPublisher 详细接口、NodeExecutionResult 工厂方法
