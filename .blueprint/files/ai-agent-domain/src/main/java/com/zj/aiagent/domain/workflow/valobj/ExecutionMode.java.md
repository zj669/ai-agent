# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/ExecutionMode.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/ExecutionMode.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/ExecutionMode.java
- Type: .java

## Responsibility
- 定义工作流运行模式枚举，用于区分标准执行、调试执行与干运行。
- 提供模式 `code/description` 元信息及字符串到枚举的转换入口。

## Key Symbols / Structure
- `STANDARD("standard", "标准模式")`: 常规执行模式。
- `DEBUG("debug", "调试模式")`: 调试模式，强调详细日志。
- `DRY_RUN("dry_run", "干运行模式")`: 干运行模式，用于模拟执行。
- `fromCode(String code)`: 按 code 解析枚举，未匹配时回落到 `STANDARD`。
- 枚举字段：`code`、`description`。

## Dependencies
- 无外部服务依赖（纯领域枚举）。

## Notes
- 该枚举由接口层请求参数传入（`WorkflowController.StartExecutionRequest.mode`），在调度层 `SchedulerService.startExecution(...)` 参与执行路径分支。
