# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/ExecutionContext.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/ExecutionContext.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/ExecutionContext.java
- Type: .java
- Status: 正常

## Responsibility
- 工作流执行上下文值对象（智能黑板），统一承载输入、节点输出、共享状态、LTM、STM 与执行日志。
- 为节点执行与表达式解析提供运行时数据视图。

## Key Symbols / Structure
- 主要字段：
  - 基础数据：`inputs`, `nodeOutputs`, `sharedState`
  - 记忆：`longTermMemories`, `chatHistory`
  - 感知日志：`executionLog`
- 方法：
  - `setInputs(...)`, `setNodeOutput(...)`, `getNodeOutput(...)`
  - `snapshot()`：上下文快照复制
  - `appendLog(...)`, `getExecutionLogContent()`, `clearExecutionLog()`

## Dependencies
- Java 集合并发容器：`ConcurrentHashMap`, `List`, `Map`
- `StringBuilder`

## Notes
- 该对象保持领域纯粹，不包含表达式引擎或外部依赖。
