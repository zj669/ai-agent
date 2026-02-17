# Blueprint Mirror: ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java

## Metadata
- file: `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java
- Type: .java

## Responsibility
- 工作流执行总编排器：启动、节点调度、暂停/恢复、取消、完成收口与消息回填。
- 串联 Execution 聚合、节点执行策略、检查点、人工审核、SSE 推流与聊天消息生命周期。

## Key Symbols / Structure
- 启动
  - `startExecution(executionId, agentId, userId, conversationId, versionId, inputs, mode)`
  - `startExecution(Execution, inputs, mode)`
- 记忆
  - `hydrateMemory(execution, inputs)`（LTM + STM）
- 调度
  - `scheduleNodes(...)`, `scheduleNode(...)`
  - `onNodeComplete(...)`
- 人审与暂停恢复
  - `checkPause(...)`
  - `resumeExecution(...)`
  - `pauseExecution(...)`
- 收口
  - `onExecutionComplete(execution)`
  - `extractFinalResponseFromExecution(...)`
  - `extractFinalResponseFromLogs(...)`
  - `buildThoughtSteps(...)`
- 其他
  - `cancelExecution(executionId)`
  - `isCancelled(...)`, `isExecutionPaused(...)`

## Dependencies
- Domain ports/repo:
  - `ExecutionRepository`, `CheckpointRepository`, `WorkflowCancellationPort`
  - `HumanReviewQueuePort`, `HumanReviewRepository`, `WorkflowNodeExecutionLogRepository`
  - `AgentRepository`, `ConversationRepository`, `VectorStore`, `ExpressionResolverPort`
- Domain model/service:
  - `Execution`, `WorkflowGraph`, `Node`, `NodeExecutionResult`, `WorkflowGraphFactory`
- Infra/Application:
  - `NodeExecutorFactory`, `IRedisService`, `StreamPublisherFactory`, `ApplicationEventPublisher`, `ChatApplicationService`

## Notes
- 状态: 正常
- 关键一致性策略：执行锁（Redisson）+ 检查点持久化 + paused gate 防止并发回调越界推进。
