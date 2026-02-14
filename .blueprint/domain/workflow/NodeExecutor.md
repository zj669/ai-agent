## Metadata
- file: `.blueprint/domain/workflow/NodeExecutor.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: NodeExecutor
- 该文件用于描述 NodeExecutor 的职责边界与协作关系。

## 2) 核心方法
- `executeAsync()`
- `getSupportedType()`
- `supportsStreaming()`
- `publishStart()`
- `publishDelta()`

## 3) 具体方法
### 3.1 executeAsync()
- 函数签名: `CompletableFuture<NodeExecutionResult> executeAsync(Node node, Map<String, Object> resolvedInputs, StreamPublisher streamPublisher)`
- 入参:
  - `node`: 节点定义（包含类型、配置、输入模板）
  - `resolvedInputs`: 已解析的输入参数（SpEL 表达式已替换为实际值）
  - `streamPublisher`: 流式推送器（用于实时推送执行过程到前端 SSE）
- 出参: CompletableFuture<NodeExecutionResult>（异步执行结果，包含状态、输出、分支选择等）
- 功能含义: 异步执行节点逻辑，支持流式输出（LLM 节点），返回执行结果供 Execution.advance() 处理。
- 链路作用: 在 SchedulerService.scheduleNode() 中调用，执行具体节点业务逻辑（LLM/HTTP/Condition/Tool 等）。

### 3.2 getSupportedType()
- 函数签名: `NodeType getSupportedType()`
- 入参: 无
- 出参: NodeType 枚举（START/END/LLM/HTTP/CONDITION/TOOL）
- 功能含义: 返回该执行器支持的节点类型，用于策略工厂路由。
- 链路作用: 在 NodeExecutorFactory.getStrategy() 中调用，根据节点类型选择对应执行器。

### 3.3 supportsStreaming()
- 函数签名: `boolean supportsStreaming()`
- 入参: 无
- 出参: true 表示支持 SSE 流式推送，false 表示不支持
- 功能含义: 标识该执行器是否支持流式输出（LLM 节点返回 true，其他节点返回 false）。
- 链路作用: 在 StreamPublisher 中判断是否启用流式推送逻辑。

### 3.4 publishStart()
- 函数签名: `void publishStart()`（StreamPublisher 接口方法，非 NodeExecutorStrategy 直接方法）
- 入参: 无
- 出参: 无（void）
- 功能含义: 发布节点开始执行事件到 SSE 流，通知前端节点状态变更。
- 链路作用: 在 SchedulerService.scheduleNode() 中调用，标记节点执行开始。

### 3.5 publishDelta()
- 函数签名: `void publishDelta(String delta)`（StreamPublisher 接口方法，非 NodeExecutorStrategy 直接方法）
- 入参:
  - `delta`: 增量内容（LLM 流式输出的 token）
- 出参: 无（void）
- 功能含义: 发布节点执行增量内容到 SSE 流，实现流式输出效果。
- 链路作用: 在 LlmNodeExecutorStrategy 中调用，推送 LLM 生成的 token 到前端。


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全方法签名与语义，从 NodeExecutorStrategy.java 提取真实实现契约。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
