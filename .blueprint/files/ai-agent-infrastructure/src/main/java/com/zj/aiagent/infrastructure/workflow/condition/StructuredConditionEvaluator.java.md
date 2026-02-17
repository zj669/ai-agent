## Metadata
- file: `.blueprint/files/ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/condition/StructuredConditionEvaluator.java.md`
- source: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/condition/StructuredConditionEvaluator.java`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-15
- owner: backend-blueprint-shadow

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: StructuredConditionEvaluator
- `ConditionEvaluatorPort` 的结构化实现，基于 `ConditionBranch/Group/Item` 三层模型进行类型安全条件评估。
- 负责分支配置校验（必须且仅一个 default）、优先级排序、变量引用解析与比较运算。

## 2) 核心方法
- `evaluate(List<ConditionBranch> branches, ExecutionContext context)`
- `validateBranches(List<ConditionBranch> branches)`
- `evaluateBranch(ConditionBranch branch, ExecutionContext context)`
- `evaluateGroup(ConditionGroup group, ExecutionContext context)`
- `evaluateItem(ConditionItem item, ExecutionContext context)`
- `resolveOperand(Object operand, ExecutionContext context)`
- `compareValues(Object left, ComparisonOperator op, Object right)`

## 3) 具体方法
### 3.1 evaluate(...)
- 函数签名: `public ConditionBranch evaluate(List<ConditionBranch> branches, ExecutionContext context)`
- 入参: 分支列表、执行上下文
- 出参: 命中分支（无命中时返回 default）
- 功能含义: 按优先级评估非 default 分支并回退 default。
- 链路作用: Condition 节点 EXPRESSION 模式的核心判定引擎。

### 3.2 resolveOperand(...)
- 函数签名: `private Object resolveOperand(Object operand, ExecutionContext context)`
- 入参: 操作数、执行上下文
- 出参: 解析值
- 功能含义: 解析 `nodes.{nodeId}.{key}` 与 `inputs.{key}` 变量引用。
- 链路作用: 把图上声明条件映射为运行时真实值。

### 3.3 compareValues(...)
- 函数签名: `private boolean compareValues(Object left, ComparisonOperator op, Object right)`
- 入参: 左值/操作符/右值
- 出参: 比较结果
- 功能含义: 支持 EQUALS、CONTAINS、数值比较、IS_EMPTY、STARTS_WITH 等运算。
- 链路作用: 保证分支评估可预测且可测试。

## 4) 变更记录
- 2026-02-15: 回填结构化条件评估器蓝图语义。
- 2026-02-14: 初始化镜像蓝图（自动补缺）。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
