# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/workflow/StateUpdate.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/workflow/StateUpdate.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/workflow/StateUpdate.java
- Type: .java

## Responsibility
- 封装节点执行后的状态增量与控制信号（继续/暂停/结束/错误）。
- 提供工厂方法简化节点侧结果构造。

## Key Symbols / Structure
- 字段：`updates`、`signal`、`message`。
- 工厂：`of(...)`、`pause(...)`、`end(...)`、`error(...)`。

## Dependencies
- `ControlSignal`。
- Lombok `@Data`。

## Notes
- `updates` 在构造时兜底为空 Map，避免空指针。
