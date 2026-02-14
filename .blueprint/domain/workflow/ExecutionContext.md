## Metadata
- file: `.blueprint/domain/workflow/ExecutionContext.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: ExecutionContext
- 该文件用于描述 ExecutionContext 的职责边界与协作关系。

## 2) 核心方法
- `setInputs()`
- `setNodeOutput()`
- `getNodeOutput()`
- `appendLog()`
- `getExecutionLogContent()`

## 3) 具体方法
### 3.1 setInputs()
- 函数签名: `void setInputs(Map<String, Object> inputs)`
- 入参:
  - `inputs`: 全局输入参数映射（工作流启动时传入）
- 出参: 无（void）
- 功能含义: 设置工作流全局输入参数，存储到 ConcurrentHashMap 中供所有节点访问。
- 链路作用: 在 Execution.start() 时调用，初始化执行上下文的输入数据。

### 3.2 setNodeOutput()
- 函数签名: `void setNodeOutput(String nodeId, Map<String, Object> outputs)`
- 入参:
  - `nodeId`: 节点ID
  - `outputs`: 节点执行输出结果
- 出参: 无（void）
- 功能含义: 存储节点执行输出到上下文，供下游节点通过 SpEL 表达式引用（如 `{{nodes.llm1.response}}`）。
- 链路作用: 在 Execution.advance() 中调用，保存节点执行结果到黑板。

### 3.3 getNodeOutput()
- 函数签名: `Map<String, Object> getNodeOutput(String nodeId)`
- 入参:
  - `nodeId`: 节点ID
- 出参: 节点输出映射（若不存在返回空 Map）
- 功能含义: 获取指定节点的输出结果，用于表达式解析或人工审核后的输出修改。
- 链路作用: 在 ExpressionResolverPort 解析 SpEL 表达式时调用，提供节点输出数据源。

### 3.4 appendLog()
- 函数签名: `void appendLog(String nodeId, String nodeName, String summary)`
- 入参:
  - `nodeId`: 节点ID
  - `nodeName`: 节点名称
  - `summary`: 执行摘要（如 "LLM响应: ..."）
- 出参: 无（void）
- 功能含义: 追加节点执行日志到 executionLog（StringBuilder），记录执行流水账，让 LLM 节点感知当前进度（Awareness）。
- 链路作用: 在 SchedulerService.onNodeComplete() 中调用，构建执行日志供后续节点使用。

### 3.5 getExecutionLogContent()
- 函数签名: `String getExecutionLogContent()`
- 入参: 无
- 出参: 执行日志字符串（格式: `[nodeId-nodeName]: summary\n`）
- 功能含义: 获取完整执行日志内容，用于 LLM 节点的 Prompt 拼接或最终响应提取。
- 链路作用: 在 LLM 节点执行时注入 Prompt，或在 SchedulerService.onExecutionComplete() 中提取最终响应。


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全方法签名与语义，从 ExecutionContext.java 提取真实实现契约。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
