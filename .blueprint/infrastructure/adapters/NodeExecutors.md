## Metadata
- file: `.blueprint/infrastructure/adapters/NodeExecutors.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: NodeExecutors
- 该文件描述节点执行器策略的职责边界，实现 NodeExecutorStrategy 接口，负责执行不同类型的工作流节点（LLM、Condition、Tool、HTTP、Start、End）。每个执行器封装特定节点类型的业务逻辑，支持异步执行和流式输出。

## 2) 核心方法
- `executeAsync()`
- `getSupportedType()`
- `evaluate()`
- `parseBranchesFromConfig()`
- `convertLegacyEdgesToBranches()`

## 3) 具体方法
### 3.1 executeAsync()
- 函数签名: `CompletableFuture<NodeExecutionResult> executeAsync(Node node, Map<String, Object> resolvedInputs, StreamPublisher streamPublisher)`
- 入参: `node` 节点定义，`resolvedInputs` 已解析的输入变量（包含 __context__、__outgoingEdges__ 等内部变量），`streamPublisher` 流式发布器
- 出参: `CompletableFuture<NodeExecutionResult>` 异步返回执行结果（success/failed/routing）
- 功能含义: 异步执行节点逻辑，根据节点类型调用不同策略（LLM 调用模型、Condition 评估分支、Tool 执行工具、HTTP 发送请求），支持流式输出
- 链路作用: SchedulerService.executeNode() → NodeExecutorFactory.getExecutor() → executeAsync() → StreamPublisher.publish*() → Redis SSE

### 3.2 getSupportedType()
- 函数签名: `NodeType getSupportedType()`
- 入参: 无
- 出参: `NodeType` 枚举值（LLM/CONDITION/TOOL/HTTP/START/END）
- 功能含义: 返回该执行器支持的节点类型，用于 NodeExecutorFactory 路由到正确的执行器
- 链路作用: NodeExecutorFactory 初始化时注册策略映射，执行时根据 node.getType() 查找对应执行器

### 3.3 evaluate()
- 函数签名: `ConditionBranch evaluate(List<ConditionBranch> branches, ExecutionContext context)` (ConditionEvaluatorPort)
- 入参: `branches` 条件分支列表（包含 priority、targetNodeId、conditionGroups），`context` 执行上下文（包含 sharedState、nodeOutputs）
- 出参: `ConditionBranch` 选中的分支（按 priority 排序，匹配第一个满足条件的分支，或返回 default 分支）
- 功能含义: 结构化条件评估，遍历分支按优先级评估 conditionGroups（支持 AND/OR 逻辑），使用 SpEL 表达式引擎计算条件
- 链路作用: ConditionNodeExecutorStrategy.evaluateByStructuredCondition() → ConditionEvaluatorPort.evaluate() → SpEL 评估 → 返回 selectedBranch

### 3.4 parseBranchesFromConfig()
- 函数签名: `List<ConditionBranch> parseBranchesFromConfig(NodeConfig config, String nodeId)`
- 入参: `config` 节点配置对象，`nodeId` 节点 ID（用于日志）
- 出参: `List<ConditionBranch>` 解析后的分支列表，解析失败返回 null
- 功能含义: 从 NodeConfig.properties["branches"] 解析 ConditionBranch 列表，使用 ObjectMapper 反序列化 JSON 配置
- 链路作用: ConditionNodeExecutorStrategy.executeAsync() → parseBranchesFromConfig() → ObjectMapper.convertValue() → 返回分支列表或 fallback 到 legacy 转换

### 3.5 convertLegacyEdgesToBranches()
- 函数签名: `List<ConditionBranch> convertLegacyEdgesToBranches(List<Edge> edges, String nodeId)`
- 入参: `edges` 旧版边列表（包含 condition、isDefault 字段），`nodeId` 节点 ID
- 出参: `List<ConditionBranch>` 转换后的分支列表（default 分支 priority=MAX_VALUE）
- 功能含义: 向后兼容旧版工作流图，将 Edge.condition 转换为 ConditionBranch，default 边转为 isDefault=true 分支
- 链路作用: parseBranchesFromConfig() 返回 null 时触发 → convertLegacyEdgesToBranches() → 构建兼容分支列表 → 继续条件评估流程


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全所有方法签名、入参、出参、功能含义和链路作用，基于 ConditionNodeExecutorStrategy 和 LlmNodeExecutorStrategy 实现。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
