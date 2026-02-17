# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/bizlogic/BizLogicProcessor.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/bizlogic/BizLogicProcessor.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/bizlogic/BizLogicProcessor.java
- Type: .java

## Responsibility
- 定义业务处理器生命周期阶段接口（初始化、校验、填充、处理、后置）。
- 统一业务编排最小处理单元，便于组合式流程执行。

## Key Symbols / Structure
- `init(CONTEXT)`
- `validate(CONTEXT)`
- `fill(CONTEXT)`
- `handle(CONTEXT)`
- `post(CONTEXT)`（默认空实现）

## Dependencies
- 仅 JDK 泛型。

## Notes
- 常由委托类按阶段批量执行多个处理器。
