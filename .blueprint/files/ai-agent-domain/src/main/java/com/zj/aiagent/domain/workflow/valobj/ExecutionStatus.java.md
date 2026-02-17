# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/ExecutionStatus.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/ExecutionStatus.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/ExecutionStatus.java
- Type: .java

## Responsibility
- 定义工作流执行与节点执行共用的生命周期状态枚举。
- 为执行链路提供统一状态码（`code`），用于日志事件、持久化映射与前后端状态展示。

## Key Symbols / Structure
- `PENDING(0)`: 待执行。
- `RUNNING(1)`: 执行中。
- `SUCCEEDED(2)`: 执行成功。
- `FAILED(3)`: 执行失败。
- `SKIPPED(4)`: 因条件分支未命中而跳过。
- `PAUSED(5)`: 手动或流程暂停。
- `CANCELLED(6)`: 已取消。
- `PAUSED_FOR_REVIEW(10)`: 因人工审核触发暂停。
- 结构特征：枚举项附带 `int code`，通过 Lombok `@Getter` 暴露读取。

## Dependencies
- Lombok:
  - `@Getter`, `@AllArgsConstructor`
- 无其他业务依赖（纯值对象枚举）。

## Notes
- 该枚举在 `Execution` 聚合、`SchedulerService` 调度链路、节点日志事件中被反复使用，是 workflow 状态语义的单一来源。
