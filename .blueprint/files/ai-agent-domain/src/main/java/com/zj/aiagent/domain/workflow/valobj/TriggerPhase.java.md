# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/TriggerPhase.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/TriggerPhase.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/TriggerPhase.java
- Type: .java

## Responsibility
- 定义人工审核触发阶段枚举，用于区分节点“执行前审核”与“执行后审核”。
- 作为暂停/恢复流程中的核心判定条件，驱动 `Execution.resume` 与 `SchedulerService.checkPause` 的分支行为。

## Key Symbols / Structure
- `BEFORE_EXECUTION`: 节点执行前触发审核，通常用于人工修改输入。
- `AFTER_EXECUTION`: 节点执行后触发审核，通常用于人工修改输出。

## Dependencies
- 无外部依赖（纯领域枚举）。

## Notes
- 与 `HumanReviewConfig.triggerPhase` 搭配使用，是审核队列入队、恢复调度策略的关键语义字段。
