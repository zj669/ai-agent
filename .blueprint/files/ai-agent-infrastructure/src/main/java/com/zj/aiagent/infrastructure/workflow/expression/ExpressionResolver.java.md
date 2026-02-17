## Metadata
- file: `.blueprint/files/ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/expression/ExpressionResolver.java.md`
- source: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/expression/ExpressionResolver.java`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-15
- owner: backend-blueprint-shadow

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: ExpressionResolver
- `ExpressionResolverPort` 的 SpEL 实现，为节点输入映射提供表达式求值能力。
- 支持 `#{inputs.key}`、`#{sharedState.key}`、`#{nodeId.outputKey}` 风格引用。

## 2) 核心方法
- `resolve(String expression, ExecutionContext context)`
- `resolveInputs(Map<String, Object> inputMappings, ExecutionContext context)`
- `buildEvaluationContext(ExecutionContext context)`

## 3) 具体方法
### 3.1 resolve(...)
- 函数签名: `public Object resolve(String expression, ExecutionContext context)`
- 入参: 表达式字符串、执行上下文
- 出参: 解析值或原始表达式
- 功能含义: 校验 `#{...}` 包裹格式后执行 SpEL 求值，失败时保底返回原文。
- 链路作用: 节点入参动态绑定的基础能力。

### 3.2 resolveInputs(...)
- 函数签名: `public Map<String, Object> resolveInputs(...)`
- 入参: 输入映射、执行上下文
- 出参: 批量解析后的映射
- 功能含义: 逐项处理字符串表达式，保留非字符串值。
- 链路作用: 调度器下发节点前的参数标准化步骤。

## 4) 变更记录
- 2026-02-15: 回填表达式解析器蓝图语义。
- 2026-02-14: 初始化镜像蓝图（自动补缺）。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
