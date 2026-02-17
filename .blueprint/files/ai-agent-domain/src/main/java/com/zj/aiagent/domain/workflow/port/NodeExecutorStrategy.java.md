# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/NodeExecutorStrategy.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/NodeExecutorStrategy.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/NodeExecutorStrategy.java
- Type: .java

## Responsibility
- 定义“节点执行策略”领域端口，解耦调度流程与具体节点执行实现。
- 约束基础设施层执行器的统一契约：异步执行、类型声明、流式能力声明。

## Key Symbols / Structure
- `CompletableFuture<NodeExecutionResult> executeAsync(Node node, Map<String, Object> resolvedInputs, StreamPublisher streamPublisher)`
  - 统一异步执行入口；接收已完成表达式解析的输入，并通过 `StreamPublisher` 输出过程事件。
- `NodeType getSupportedType()`
  - 声明当前策略支持的节点类型，供工厂/路由器按类型分发。
- `default boolean supportsStreaming()`
  - 默认返回 `false`，由需要流式输出的策略覆写。

## Dependencies
- Domain entities/value objects:
  - `Node`, `NodeExecutionResult`, `NodeType`
- Workflow stream port:
  - `StreamPublisher`
- Java concurrency:
  - `CompletableFuture`

## Notes
- 该接口是 workflow 执行扩展点；新增节点类型时应新增对应实现并在工厂中注册。
- `executeAsync` 的输入已是“调度层解析后的参数”，策略内部不应重复做表达式解析。
