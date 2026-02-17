# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/bizlogic/AbstractBizLogicDelegate.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/bizlogic/AbstractBizLogicDelegate.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/bizlogic/AbstractBizLogicDelegate.java
- Type: .java

## Responsibility
- 提供业务处理器委托模板，实现按固定阶段串行执行处理器列表。
- 将流程控制与具体处理逻辑解耦，子类仅负责提供处理器集合。

## Key Symbols / Structure
- `process(CONTEXT context)`：`init -> validate -> fill -> handle -> post`。
- `getBizLogicProcessors()`：子类返回处理器列表。
- 各阶段默认实现：遍历处理器并调用对应阶段方法。

## Dependencies
- `BizLogicProcessor<CONTEXT>`。
- JDK `List`。

## Notes
- 适用于“多处理器链式编排”的业务场景。
