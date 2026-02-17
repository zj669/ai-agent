## Metadata
- file: `.blueprint/files/ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/ConditionNodeExecutorStrategy.java.md`
- source: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/ConditionNodeExecutorStrategy.java`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-15
- owner: backend-blueprint-shadow

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: ConditionNodeExecutorStrategy
- 负责 CONDITION 节点执行，支持 `EXPRESSION` 与 `LLM` 两种路由模式。
- 提供 legacy edge 到结构化 `ConditionBranch` 的兼容转换，并输出唯一 `selectedTarget`。

## 2) 核心方法
- `executeAsync(Node node, Map<String, Object> resolvedInputs, StreamPublisher streamPublisher)`
- `evaluateByStructuredCondition(...)`
- `evaluateByLlmMode(...)`
- `parseBranchesFromConfig(...)`
- `convertLegacyEdgesToBranches(...)`
- `buildLlmPrompt(...)`
- `callLlmAndMatchTarget(...)`

## 3) 具体方法
### 3.1 executeAsync(...)
- 函数签名: `CompletableFuture<NodeExecutionResult> executeAsync(...)`
- 入参: 节点配置、输入上下文、流式发布器
- 出参: `NodeExecutionResult.routing|failed`
- 功能含义: 读取 `routingStrategy` 并分派到结构化或 LLM 路由分支。
- 链路作用: 条件分支控制枢纽，决定工作流后续执行路径。

### 3.2 evaluateByStructuredCondition(...)
- 函数签名: `private NodeExecutionResult evaluateByStructuredCondition(...)`
- 入参: 节点、配置、输入
- 出参: routing 结果
- 功能含义: 调用 `ConditionEvaluatorPort` 评估 `ConditionBranch` 并返回命中目标。
- 链路作用: 稳定、可测试的结构化条件决策路径。

### 3.3 evaluateByLlmMode(...)
- 函数签名: `private NodeExecutionResult evaluateByLlmMode(...)`
- 入参: 节点、配置、输入
- 出参: routing 结果
- 功能含义: 生成选项 Prompt，调用模型匹配 targetId，失败时二次澄清并回退 default 分支。
- 链路作用: 处理语义条件场景，增强复杂决策能力。

## 4) 变更记录
- 2026-02-15: 回填条件执行器蓝图语义，补齐双模式路由与兼容转换说明。
- 2026-02-14: 初始化镜像蓝图（自动补缺）。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
