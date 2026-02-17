# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/service/WorkflowGraphFactory.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/service/WorkflowGraphFactory.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/service/WorkflowGraphFactory.java
- Type: .java (interface)
- Status: 正常

## Responsibility
- 定义工作流图工厂领域服务端口，将 `graphJson` 解析为 `WorkflowGraph`。
- 将图解析能力抽象到接口，保持 Domain 与具体 JSON 解析实现解耦。

## Key Symbols / Structure
- 方法：`WorkflowGraph fromJson(String graphJson)`。

## Dependencies
- `WorkflowGraph`

## Notes
- 实现位于基础设施层，Domain 仅声明契约。
