# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java
- Type: .java

## Responsibility
- Workflow 执行聚合根，封装执行生命周期状态机、节点状态推进、条件分支剪枝、暂停/恢复语义和检查点创建。
- 以 `WorkflowGraph + ExecutionContext + nodeStatuses` 表达一次执行的完整领域状态。
- 对外提供纯领域行为（start/advance/resume/getReadyNodes），由 Application 层调度服务调用。

## Key Symbols / Structure
- 核心状态字段：`status`, `nodeStatuses`, `pausedNodeId`, `pausedPhase`, `version`, `context`。
- `start(Map<String, Object> inputs)`
  - 校验 DAG 无环、初始化上下文和节点状态，转为 RUNNING 并返回起始可执行节点。
- `advance(String nodeId, NodeExecutionResult result)`
  - 写入节点结果、更新上下文输出、按条件节点做分支剪枝、处理暂停/完成/失败判定并返回下一批就绪节点。
- `resume(String nodeId, Map<String, Object> additionalInputs)`
  - 校验暂停态与暂停节点一致性，恢复执行状态；按 `TriggerPhase` 决定是重新执行节点还是等待外层手动推进。
- `getReadyNodes()` + `calculateEffectiveInDegrees()`
  - 计算“有效入度”（已完成/跳过/失败前驱不阻塞），筛选当前可调度节点。
- `pruneUnselectedBranches(...)` + `skipNodeRecursively(...)`
  - 条件分支执行后的未选路径剪枝；汇聚节点仅在全部前驱 SKIPPED 时递归跳过。
- `createCheckpoint(String nodeId)`
  - 基于当前状态生成普通/暂停检查点快照。

## Dependencies
- Workflow domain objects:
  - `WorkflowGraph`, `Node`, `ExecutionContext`, `Checkpoint`
- Workflow enums/value objects:
  - `ExecutionStatus`, `NodeExecutionResult`, `TriggerPhase`
- Java utilities:
  - `LocalDateTime`, `Map`, `Set`, `List`, `Collectors`

## Notes
- 该聚合根不依赖基础设施，实现了领域层纯净性；并发控制通过 `version` 配合仓储层乐观锁完成。
- 分支剪枝逻辑显式区分单前驱与汇聚节点，避免误跳过仍可由其他分支激活的节点。
