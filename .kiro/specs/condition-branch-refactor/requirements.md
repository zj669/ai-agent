# Requirements Document

## Introduction

本需求文档描述对 AI Agent 编排平台中工作流条件分支节点（Condition Node）的全面改造。目标是将当前简单的单条件边模型升级为类似 Coze 等平台的多分支条件节点模型，支持 if / else if / else 多分支组合、条件组（AND/OR）、多种比较操作符，同时修复当前实现中已排查出的 7 个关键缺陷。改造以后端为主，前端暂不涉及，但需保证 API 兼容性。

## Glossary

- **Condition_Node**: 工作流中的条件分支节点，负责根据条件评估结果选择下游执行路径
- **Branch**: 条件节点的一个分支，包含优先级、条件组和目标节点 ID
- **Condition_Group**: 一组通过 AND 或 OR 逻辑连接的条件项
- **Condition_Item**: 单个条件比较，包含左操作数、操作符和右操作数
- **Condition_Evaluator**: 负责评估条件表达式的领域服务，替代直接使用 SpEL
- **Pruning_Engine**: 负责在条件分支选中后剪枝未选中分支的组件
- **Edge**: 工作流图中两个节点之间的连接
- **Execution**: 工作流执行聚合根，管理执行生命周期和状态流转
- **WorkflowGraph**: 工作流图实体，表示 DAG 结构
- **NodeConfig**: 节点配置，使用 Map 存储动态字段
- **SchedulerService**: 应用层调度服务，编排节点执行流程
- **SpEL**: Spring Expression Language，Spring 表达式语言
- **SimpleEvaluationContext**: SpEL 的受限评估上下文，仅允许属性访问和方法调用白名单

## Requirements

### Requirement 1: 多分支条件模型

**User Story:** As a 工作流设计者, I want to 在条件节点上定义多个 if / else if / else 分支, so that I can 实现复杂的多路条件路由逻辑。

#### Acceptance Criteria

1. THE Condition_Node SHALL support defining an ordered list of branches, where each Branch contains a priority, a list of Condition_Groups, and a target node ID
2. WHEN the Condition_Node is evaluated, THE Condition_Evaluator SHALL evaluate branches in priority order and select the first Branch whose conditions are satisfied
3. THE Condition_Node SHALL require exactly one default (else) Branch that has no conditions and is evaluated last
4. WHEN no non-default Branch matches, THE Condition_Evaluator SHALL select the default Branch as the routing target
5. WHEN the branch list is serialized to JSON and deserialized back, THE Condition_Node SHALL produce an equivalent branch configuration (round-trip property)

### Requirement 2: 条件组合与比较操作符

**User Story:** As a 工作流设计者, I want to 在每个分支中使用 AND/OR 组合多个条件, so that I can 表达复杂的业务判断逻辑。

#### Acceptance Criteria

1. THE Condition_Group SHALL combine multiple Condition_Items using a logical operator (AND or OR)
2. WHEN a Condition_Group uses AND logic, THE Condition_Evaluator SHALL return true only when all Condition_Items in the group evaluate to true
3. WHEN a Condition_Group uses OR logic, THE Condition_Evaluator SHALL return true when at least one Condition_Item in the group evaluates to true
4. THE Condition_Item SHALL support the following comparison operators: EQUALS, NOT_EQUALS, CONTAINS, NOT_CONTAINS, GREATER_THAN, LESS_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL, IS_EMPTY, IS_NOT_EMPTY, STARTS_WITH, ENDS_WITH
5. WHEN a Branch contains multiple Condition_Groups, THE Condition_Evaluator SHALL combine them using AND logic (all groups must be satisfied)
6. THE Condition_Item SHALL support referencing upstream node outputs as operand values using a structured variable reference format

### Requirement 3: 条件评估安全性

**User Story:** As a 平台管理员, I want to 确保条件评估过程安全可控, so that I can 防止恶意表达式注入和执行。

#### Acceptance Criteria

1. THE Condition_Evaluator SHALL use a structured condition model (Condition_Item with operator enum) instead of raw SpEL expressions for the EXPRESSION routing strategy
2. WHEN the Condition_Evaluator resolves variable references, THE Condition_Evaluator SHALL only access values from the ExecutionContext shared state and node outputs
3. IF a Condition_Item references a variable that does not exist in the ExecutionContext, THEN THE Condition_Evaluator SHALL treat the condition as not satisfied and log a warning
4. THE Condition_Evaluator SHALL validate all Condition_Items before evaluation, rejecting items with null operators or invalid operand types

### Requirement 4: 分支剪枝逻辑修复

**User Story:** As a 工作流引擎开发者, I want to 修复条件分支的剪枝逻辑, so that I can 确保未选中分支的节点被正确跳过而不影响汇聚节点。

#### Acceptance Criteria

1. WHEN a Condition_Node selects a Branch, THE Pruning_Engine SHALL skip all direct successors of the Condition_Node that are not the selected Branch target
2. WHEN the Pruning_Engine encounters a convergence node (a node with multiple predecessors), THE Pruning_Engine SHALL skip the convergence node only when all of the convergence node predecessors are in SKIPPED or SUCCEEDED status
3. WHEN the Pruning_Engine encounters a convergence node where at least one predecessor is still PENDING, THE Pruning_Engine SHALL not skip the convergence node
4. THE Pruning_Engine SHALL use the WorkflowGraph edge topology to determine branch membership instead of string-based node ID matching

### Requirement 5: 条件评估顺序确定性

**User Story:** As a 工作流引擎开发者, I want to 确保条件分支的评估顺序稳定且可预测, so that I can 保证相同输入产生相同的路由结果。

#### Acceptance Criteria

1. THE Condition_Node SHALL store branches with explicit priority values (integer, starting from 0)
2. WHEN evaluating branches, THE Condition_Evaluator SHALL process branches strictly in ascending priority order
3. WHEN the branch configuration is loaded from JSON, THE WorkflowGraph factory SHALL preserve the branch priority order regardless of JSON serialization order

### Requirement 6: Default 分支语义明确化

**User Story:** As a 工作流引擎开发者, I want to 明确区分 default 分支和无条件边, so that I can 避免将普通依赖边误判为 default 分支。

#### Acceptance Criteria

1. THE Branch model SHALL use an explicit boolean field (isDefault) to mark the default branch, instead of inferring default status from empty conditions
2. WHEN a Condition_Node has no Branch explicitly marked as default, THE Condition_Evaluator SHALL report a configuration error
3. WHEN a Condition_Node has more than one Branch marked as default, THE Condition_Evaluator SHALL report a configuration error

### Requirement 7: LLM 路由模式兼容性

**User Story:** As a 工作流设计者, I want to 继续使用 LLM 语义路由模式, so that I can 利用 AI 进行智能条件判断。

#### Acceptance Criteria

1. THE Condition_Node SHALL continue to support both EXPRESSION and LLM routing strategies through the routingStrategy configuration field
2. WHEN using LLM routing strategy, THE Condition_Evaluator SHALL construct the LLM prompt using Branch descriptions instead of Edge conditions
3. IF the LLM returns an invalid or unrecognizable target ID, THEN THE Condition_Evaluator SHALL retry once with a clarified prompt before falling back to the default Branch
4. WHEN the LLM response is parsed, THE Condition_Evaluator SHALL trim whitespace and perform case-insensitive matching against valid Branch target IDs

### Requirement 8: 条件上下文一致性

**User Story:** As a 工作流引擎开发者, I want to 统一条件评估的变量上下文, so that I can 确保条件节点和其他节点使用一致的变量引用方式。

#### Acceptance Criteria

1. THE Condition_Evaluator SHALL resolve variable references using the same ExecutionContext that other node executors use for input resolution
2. WHEN a Condition_Item references an upstream node output, THE Condition_Evaluator SHALL use the format `nodes.{nodeId}.{outputKey}` to locate the value
3. THE Condition_Evaluator SHALL support referencing global inputs using the format `inputs.{key}`

### Requirement 9: 向后兼容与数据迁移

**User Story:** As a 平台管理员, I want to 确保现有工作流定义在升级后仍能正常运行, so that I can 平滑过渡到新的条件分支模型。

#### Acceptance Criteria

1. WHEN the WorkflowGraph factory encounters an Edge with a legacy condition string (plain SpEL expression), THE WorkflowGraph factory SHALL convert the legacy condition into the new Branch model with a single Condition_Group containing one Condition_Item
2. WHEN the WorkflowGraph factory encounters an Edge with edgeType DEFAULT, THE WorkflowGraph factory SHALL convert the Edge into a default Branch
3. IF the legacy condition string cannot be parsed into the new model, THEN THE WorkflowGraph factory SHALL log a warning and treat the Edge as a default Branch
