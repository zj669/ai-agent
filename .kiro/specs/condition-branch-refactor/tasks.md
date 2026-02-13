# Implementation Plan: Condition Branch Refactor

## Overview

将条件分支节点从单条件边模型改造为多分支条件模型（if/else if/else），引入 ConditionBranch/ConditionGroup/ConditionItem 三层值对象，实现结构化条件评估器，修复剪枝逻辑缺陷，并保持旧模型向后兼容。实现顺序：领域模型 → 评估器 → 剪枝修复 → 执行器重构 → 兼容转换 → 蓝图同步。

## Tasks

- [x] 1. 创建领域层条件模型值对象
  - [x] 1.1 创建 ComparisonOperator 枚举和 LogicalOperator 枚举
    - 在 `ai-agent-domain/.../workflow/valobj/` 下创建
    - ComparisonOperator: EQUALS, NOT_EQUALS, CONTAINS, NOT_CONTAINS, GREATER_THAN, LESS_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL, IS_EMPTY, IS_NOT_EMPTY, STARTS_WITH, ENDS_WITH
    - LogicalOperator: AND, OR
    - _Requirements: 2.4_
  - [x] 1.2 创建 ConditionItem、ConditionGroup、ConditionBranch 值对象
    - ConditionItem: leftOperand (String), operator (ComparisonOperator), rightOperand (Object)
    - ConditionGroup: operator (LogicalOperator), conditions (List\<ConditionItem\>)
    - ConditionBranch: priority (int), targetNodeId (String), description (String), isDefault (boolean), conditionGroups (List\<ConditionGroup\>)
    - 使用 Lombok @Data @Builder @NoArgsConstructor @AllArgsConstructor
    - _Requirements: 1.1, 2.1, 6.1_
  - [x] 1.3 创建 ConditionEvaluatorPort 端口接口
    - 在 `ai-agent-domain/.../workflow/port/` 下创建
    - 方法: `ConditionBranch evaluate(List<ConditionBranch> branches, ExecutionContext context)`
    - _Requirements: 1.2_
  - [x] 1.4 创建 ConditionConfigurationException 领域异常
    - 在 `ai-agent-domain/.../workflow/exception/` 下创建
    - 用于分支配置校验失败（无 default、多 default 等）
    - _Requirements: 6.2, 6.3_

- [x] 2. 实现结构化条件评估器
  - [x] 2.1 实现 StructuredConditionEvaluator
    - 在 `ai-agent-infrastructure/.../workflow/condition/` 下创建
    - 实现 ConditionEvaluatorPort
    - 核心方法: evaluate() — 按 priority 排序，逐个评估非 default 分支，首个命中胜出，无命中返回 default
    - resolveOperand() — 解析 `nodes.{nodeId}.{key}` 和 `inputs.{key}` 格式的变量引用
    - compareValues() — 基于 ComparisonOperator 执行类型安全比较
    - evaluateGroup() — 按 LogicalOperator (AND/OR) 评估组内条件
    - validateBranches() — 校验恰好一个 default 分支，operator 非 null
    - _Requirements: 1.2, 1.4, 2.2, 2.3, 2.4, 2.5, 2.6, 3.1, 3.2, 3.3, 3.4, 5.2, 6.2, 6.3, 8.1, 8.2, 8.3_
  - [x] 2.2 编写 Property Test: 比较操作符正确性
    - **Property 5: Comparison operator correctness**
    - **Validates: Requirements 2.4**
  - [x] 2.3 编写 Property Test: 条件组逻辑评估
    - **Property 3: Condition group logical evaluation**
    - **Validates: Requirements 2.2, 2.3**
  - [x] 2.4 编写 Property Test: 多组 AND 组合
    - **Property 4: Multi-group AND combination**
    - **Validates: Requirements 2.5**
  - [x] 2.5 编写 Property Test: 变量引用解析
    - **Property 6: Variable reference resolution**
    - **Validates: Requirements 2.6, 3.3, 8.2, 8.3**
  - [x] 2.6 编写 Property Test: 分支评估优先级
    - **Property 1: Branch evaluation selects first matching by priority**
    - **Validates: Requirements 1.2, 1.4, 5.2**
  - [x] 2.7 编写 Property Test: 分支配置序列化 round-trip
    - **Property 2: Branch configuration serialization round-trip**
    - **Validates: Requirements 1.5, 5.3**
  - [x] 2.8 编写单元测试: 评估器边界情况
    - 测试无 default 分支抛异常、多 default 分支抛异常、空 branches 列表
    - 测试 null operator 的 ConditionItem 被跳过
    - 测试变量不存在时条件视为不满足
    - _Requirements: 3.3, 3.4, 6.2, 6.3_

- [x] 3. Checkpoint - 确保评估器所有测试通过
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. 修复 Execution 剪枝逻辑
  - [x] 4.1 修复 pruneUnselectedBranches 和 isNodeInSelectedBranch
    - 移除 `isNodeInSelectedBranch` 方法
    - 在 `pruneUnselectedBranches` 中直接比较 `successor.getNodeId().equals(selectedBranchId)`
    - _Requirements: 4.1, 4.4_
  - [x] 4.2 修复 skipNodeRecursively 汇聚节点逻辑
    - 单前驱节点：直接递归跳过
    - 多前驱（汇聚）节点：仅当所有前驱都是 SKIPPED 时才跳过；如果有任何前驱是 PENDING，不跳过
    - _Requirements: 4.2, 4.3_
  - [x] 4.3 编写 Property Test: 直接后继剪枝
    - **Property 7: Direct successor pruning**
    - **Validates: Requirements 4.1**
  - [x] 4.4 编写 Property Test: 汇聚节点剪枝
    - **Property 8: Convergence node pruning correctness**
    - **Validates: Requirements 4.2, 4.3**
  - [x] 4.5 编写单元测试: 剪枝边界情况
    - 测试菱形 DAG（分支后汇聚）
    - 测试多层嵌套条件分支
    - 测试条件节点只有一个后继的情况
    - _Requirements: 4.1, 4.2, 4.3_

- [x] 5. Checkpoint - 确保剪枝逻辑所有测试通过
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. 重构 ConditionNodeExecutorStrategy
  - [x] 6.1 重构 EXPRESSION 模式
    - 注入 ConditionEvaluatorPort
    - 从 NodeConfig.properties["branches"] 解析 List\<ConditionBranch\>（Jackson 反序列化）
    - 如果 branches 为空，从 `__outgoingEdges__` 做旧模型兼容转换
    - 调用 ConditionEvaluatorPort.evaluate()，返回 NodeExecutionResult.routing()
    - 移除直接使用 StandardEvaluationContext 的 SpEL 评估代码
    - _Requirements: 1.2, 3.1, 5.2, 8.1_
  - [x] 6.2 重构 LLM 模式
    - 使用 branch.description 构建 Prompt（替代 edge.condition）
    - 添加重试逻辑：LLM 返回无效 ID 时，发送澄清 prompt 重试一次
    - 重试仍失败则使用 default 分支
    - 响应解析：trim + case-insensitive 匹配
    - _Requirements: 7.1, 7.2, 7.3, 7.4_
  - [x] 6.3 编写 Property Test: LLM 响应匹配
    - **Property 9: LLM response target ID matching**
    - **Validates: Requirements 7.4**
  - [x] 6.4 编写单元测试: LLM 模式
    - 测试 prompt 包含 branch descriptions
    - 测试重试逻辑（mock LLM 返回无效 ID → 重试 → 成功/失败）
    - _Requirements: 7.2, 7.3_

- [-] 7. 实现旧模型兼容转换
  - [ ] 7.1 在 WorkflowGraphFactoryImpl 中添加 convertLegacyEdgesToBranches 方法
    - 将旧 Edge 列表转换为 List\<ConditionBranch\>
    - CONDITIONAL 边 → 非 default 分支（尝试解析 SpEL 为 ConditionItem）
    - DEFAULT 边 → default 分支
    - 无法解析的 SpEL → 作为 default 处理并 log warn
    - _Requirements: 9.1, 9.2, 9.3_
  - [ ] 7.2 在 NodeConfigConverter 中支持 branches 字段解析
    - 确保 condition 节点的 userConfig 中 branches 字段被正确传递到 NodeConfig.properties
    - _Requirements: 1.1, 5.1_
  - [ ] 7.3 编写 Property Test: 旧模型转换
    - **Property 10: Legacy edge to branch conversion**
    - **Validates: Requirements 9.1, 9.2**
  - [ ] 7.4 编写单元测试: 兼容转换边界情况
    - 测试典型 SpEL 表达式转换（如 `#input > 100`）
    - 测试无法解析的复杂 SpEL 表达式降级为 default
    - 测试混合 CONDITIONAL + DEFAULT 边的转换
    - _Requirements: 9.1, 9.2, 9.3_

- [ ] 8. 集成与蓝图同步
  - [ ] 8.1 更新 SchedulerService 中条件节点的边注入逻辑
    - 确保 `__outgoingEdges__` 仍然注入（兼容旧模型）
    - 同时注入 `__context__` 供评估器使用
    - _Requirements: 8.1_
  - [ ] 8.2 更新 `.blueprint/` 蓝图文件
    - 更新 `WorkflowEngine.md`: 新增 ConditionBranch/Group/Item 值对象描述，更新剪枝逻辑说明
    - 更新 `NodeExecutors.md`: 更新 ConditionNodeExecutorStrategy 设计，新增 StructuredConditionEvaluator
    - 更新 `_overview.md`: 新增 ConditionEvaluatorPort 端口
    - _Requirements: all_

- [ ] 9. Final checkpoint - 确保所有测试通过
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Property tests 使用 jqwik 框架，每个 test 最少 100 次迭代
- 蓝图更新（task 8.2）必须在代码修改完成后同步执行
- Edge 实体保持不变，condition 字段仅用于旧数据兼容
- 不涉及数据库 Schema 变更，条件配置存储在 graphJson 的 NodeConfig 中
