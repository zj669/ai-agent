# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/workflow/ControlSignal.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/workflow/ControlSignal.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/workflow/ControlSignal.java
- Type: .java

## Responsibility
- 定义节点执行后给调度器的控制指令枚举。
- 统一工作流流转语义（继续、暂停、结束、错误）。

## Key Symbols / Structure
- `CONTINUE`：继续执行下游。
- `PAUSE`：暂停等待人工或外部信号。
- `END`：正常结束。
- `ERROR`：异常终止。

## Dependencies
- 无外部依赖（JDK Enum）。

## Notes
- 与 `StateUpdate.signal` 一起决定调度器行为。
