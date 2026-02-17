# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/NodeExecutionResult.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/NodeExecutionResult.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/NodeExecutionResult.java
- Type: .java
- Status: 正常

## Responsibility
- 节点执行结果值对象，承载执行状态、输出、路由分支、错误信息与暂停阶段。
- 为执行聚合推进逻辑提供统一结果模型。

## Key Symbols / Structure
- 字段：`status`, `outputs`, `selectedBranchId`, `errorMessage`, `triggerPhase`。
- 工厂方法：
  - `success(outputs)`
  - `failed(errorMessage)`
  - `routing(branchId, outputs)`
  - `paused(phase)` / `paused(phase, outputs)`
- 判断方法：`isSuccess()`, `isPaused()`, `isRouting()`。

## Dependencies
- `ExecutionStatus`
- `TriggerPhase`
- `Map<String, Object>`

## Notes
- 条件节点通过 `selectedBranchId` 表达路由结果。
