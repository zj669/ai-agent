# SchedulerService Blueprint

## 职责契约
- **做什么**: 工作流调度的应用服务——编排 Execution 生命周期、协调节点执行、管理执行上下文、处理人工审核暂停/恢复、记忆水合(LTM/STM)、消息管理、执行日志记录
- **不做什么**: 不负责具体节点的执行逻辑；不负责 Agent 配置管理；不直接操作底层存储

## 接口摘要

| 方法 | 输入 | 输出 | 副作用 | 约束 |
|------|------|------|--------|------|
| startExecution | executionId, agentId, userId, conversationId, versionId, inputs, ExecutionMode | void | 创建Execution, 保存消息, 水合记忆, 解析图, 开始调度 | 异步 |
| resumeExecution | executionId, nodeId, edits, reviewerId, decision | void | 加载检查点, 创建审核记录, 恢复执行, 根据phase调度或推进 | 执行必须处于PAUSED |
| cancelExecution | executionId | void | 标记取消 | 幂等 |
| scheduleNodes | executionId, List\<Node\>, parentId | void | 遍历节点列表，逐个调度 | 内部方法 |
| scheduleNode | executionId, node, parentId | void | 检查暂停→解析输入→创建StreamPublisher→异步执行→回调onNodeComplete | 内部方法 |
| checkPause | executionId, node, phase, publisher, resolvedInputs | boolean | 检查人工审核配置，暂停执行并保存检查点 | 返回true表示已暂停 |
| onNodeComplete | executionId, nodeId, nodeName, nodeType, result | void | 更新上下文, 记录日志, 追加Awareness, 推进DAG | 事件回调 |
| onExecutionComplete | execution | void | 持久化结果, 更新assistant消息(含ThoughtSteps), 发布事件 | 内部方法 |

## 核心流程

```
startExecution(public)
  → 查询 Agent，获取 graphJson（草稿/已发布/指定版本）
  → WorkflowGraphFactory.fromJson(graphJson) 解析为领域对象
  → 构建 Execution 聚合根
  → startExecution(private)
      → 保存用户消息 (chatApplicationService.appendUserMessage)
      → 初始化 Assistant 消息 (chatApplicationService.initAssistantMessage)
      → hydrateMemory (记忆水合)
          → extractUserQuery 提取用户意图
          → VectorStore 语义检索 → LTM
          → ConversationRepository 加载历史 → STM
      → execution.start(inputs) → 获取就绪节点
      → executionRepository.save(execution)
      → scheduleNodes(readyNodes)

scheduleNode
  → isCancelled? → 终止
  → checkPause(BEFORE_EXECUTION)? → 暂停
  → context.resolveInputs(node.inputs) → 解析SpEL
  → 注入 __outgoingEdges__ (条件节点)
  → StreamPublisherFactory.create(StreamContext) → 创建推送器
  → publisher.publishStart()
  → NodeExecutorStrategy.executeAsync(node, resolvedInputs, publisher)
  → .thenAccept(onNodeComplete)

onNodeComplete
  → publisher.publishFinish(result)
  → 记录 WorkflowNodeExecutionLog
  → context.appendLog (Awareness)
  → checkPause(AFTER_EXECUTION)? → 暂停
  → execution.advance(nodeId, result) → 获取下一批就绪节点
  → executionRepository.update(execution)
  → scheduleNodes(nextNodes) 或 onExecutionComplete

onExecutionComplete
  → extractFinalResponseFromExecution → 提取最终响应
  → buildThoughtSteps → 构建思维链步骤
  → chatApplicationService.completeAssistantMessage → 更新消息
  → executionRepository.update(execution)
```

## 记忆水合 (Memory Hydration)

### hydrateMemory 流程
1. 提取用户查询 (`extractUserQuery`)
2. LTM: 调用 VectorStore 语义检索相关知识 → `context.longTermMemories`
3. STM: 从 ConversationRepository 加载会话历史 → `context.chatHistory`

### Awareness 日志
- 每个节点完成后调用 `context.appendLog(nodeId, nodeName, summary)`
- `generateNodeSummary` 根据 NodeType 生成摘要
- 后续 LLM 节点可通过 `context.getExecutionLogContent()` 获取执行进度

## 消息管理集成
- 启动时: 保存用户消息 + 初始化 PENDING 状态的 Assistant 消息
- 完成时: 更新 Assistant 消息内容（最终响应 + ThoughtSteps）
- ThoughtSteps: 从 WorkflowNodeExecutionLog 构建，包含每个节点的执行摘要

## 依赖拓扑
- **上游**: WorkflowController, HumanReviewController
- **下游**:
  - Domain: Execution, WorkflowGraph, NodeExecutorStrategy(端口), ExecutionRepository(端口), CheckpointRepository(端口), StreamPublisherFactory(端口), WorkflowNodeExecutionLogRepository(端口), HumanReviewRepository(端口), HumanReviewQueuePort(端口), WorkflowCancellationPort(端口)
  - Application: ChatApplicationService(消息管理)
  - Domain/Knowledge: VectorStore(LTM检索)
  - Domain/Agent: AgentRepository(读取Agent和版本)

## 设计约束
- 节点执行通过 `nodeExecutorThreadPool` 异步调度
- 执行上下文临时存储在 Redis（检查点），完成后持久化到 MySQL
- 人工审核节点触发 PAUSED_FOR_REVIEW，需要外部 resume
- 流式输出通过 StreamPublisher 推送到前端 (SSE via Redis Pub/Sub)
- 消息保存失败不阻塞工作流执行（try-catch 容错）
- ExecutionMode (STANDARD/DEBUG/DRY_RUN) 影响日志级别和外部调用行为

## 变更日志
- [初始] 从现有代码逆向生成蓝图
- [2026-02-08] 补充完整流程时序、记忆水合细节、消息管理集成、Awareness 日志、ThoughtSteps 构建
