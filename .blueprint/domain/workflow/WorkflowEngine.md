# WorkflowEngine Blueprint

## 职责契约
- **做什么**: 管理工作流执行的完整生命周期——创建 Execution、DAG 拓扑排序、节点调度、状态流转、暂停/恢复、检查点管理、条件分支剪枝
- **不做什么**: 不负责具体节点的执行逻辑（那是 NodeExecutorStrategy 的职责）；不负责 Agent 的配置管理；不直接操作数据库

## 核心聚合根

### Execution (聚合根)
- 工作流执行的聚合根，管理状态一致性和节点调度
- 状态机: `PENDING(0) → RUNNING(1) → PAUSED(5)/PAUSED_FOR_REVIEW(10) → SUCCEEDED(2) / FAILED(3) / CANCELLED(6)`
- 持有 WorkflowGraph（DAG 结构）和 ExecutionContext（智能黑板）
- 维护 `nodeStatuses: Map<String, ExecutionStatus>` 跟踪每个节点状态
- 支持乐观锁 (`version` 字段) 保证并发安全
- 关联 `assistantMessageId` 用于执行完成后更新聊天消息

#### 核心方法
| 方法 | 输入 | 输出 | 说明 |
|------|------|------|------|
| start | Map<String, Object> inputs | List\<Node\> (就绪节点) | 校验环、初始化上下文和节点状态、返回入度为0的节点 |
| advance | nodeId, NodeExecutionResult | List\<Node\> (下一批就绪节点) | 更新节点状态、存储输出、处理条件剪枝、检查暂停/完成 |
| resume | nodeId, Map additionalInputs | List\<Node\> | BEFORE_EXECUTION→返回待执行节点; AFTER_EXECUTION→返回空列表 |
| getReadyNodes | - | List\<Node\> | 计算有效入度，返回所有依赖已满足的 PENDING 节点 |
| createCheckpoint | nodeId | Checkpoint | 创建执行快照（普通/暂停点） |

#### 条件分支剪枝
- `pruneUnselectedBranches`: 条件节点完成后，递归跳过未选中分支的所有下游节点
- `skipNodeRecursively`: 将节点标记为 SKIPPED，递归处理下游（汇聚节点需所有前驱都已处理）

### WorkflowGraph (值对象)
- DAG 结构定义，从 Agent 的 graphJson 解析（由 WorkflowGraphFactory 负责）
- 不可变对象，创建后不允许修改
- 数据结构: `nodes: Map<String, Node>`, `edges: Map<String, Set<String>>`, `edgeDetails: Map<String, List<Edge>>`

#### 核心能力
| 方法 | 说明 |
|------|------|
| hasCycle | DFS 环检测 |
| topologicalSort | Kahn 算法拓扑排序 |
| calculateInDegrees | 计算所有节点入度 |
| getSuccessors / getPredecessors | 获取下游/上游节点 |
| getStartNodes | 获取所有 START 类型节点 |
| getOutgoingEdges | 获取节点出边详情（含条件表达式） |

### Node (实体)
- 工作流节点，持有 `nodeId`, `name`, `type: NodeType`, `config: NodeConfig`
- 输入映射 `inputs: Map<String, Object>` 支持 SpEL 表达式引用
- 输出映射 `outputs: Map<String, String>` 定义结果提取路径
- 便捷方法: `isStartNode()`, `isEndNode()`, `isConditionNode()`, `requiresHumanReview()`

### Edge (实体)
- 节点间连接，持有 `source`, `target`, `condition`, `edgeType`
- EdgeType: `DEPENDENCY`(标准依赖) / `CONDITIONAL`(条件边) / `DEFAULT`(兜底路径)
- `condition` 字段: EXPRESSION 模式存 SpEL 表达式，LLM 模式存决策描述

## 枚举类型

### NodeType
`START` | `END` | `LLM` | `HTTP` | `CONDITION` | `TOOL`

### ExecutionStatus
`PENDING(0)` | `RUNNING(1)` | `SUCCEEDED(2)` | `FAILED(3)` | `SKIPPED(4)` | `PAUSED(5)` | `CANCELLED(6)` | `PAUSED_FOR_REVIEW(10)`

### ExecutionMode
`STANDARD` | `DEBUG`(详细日志) | `DRY_RUN`(模拟执行，不调用外部服务)

### TriggerPhase
`BEFORE_EXECUTION`(审核输入) | `AFTER_EXECUTION`(审核输出)

## 依赖拓扑
- **上游**: SchedulerService (应用层编排)
- **下游**: NodeExecutorStrategy(端口), ExecutionRepository(端口), CheckpointRepository(端口), StreamPublisher(端口), WorkflowNodeExecutionLogRepository(端口)

## 领域事件
- 发布: `NodeCompletedEvent` — 节点执行完成时 (executionId, nodeId, result)
- 发布: `ExecutionCompletedEvent` — 整个工作流执行完成时 (executionId, status)

## 设计约束
- 节点执行必须异步，通过线程池调度 (`nodeExecutorThreadPool`)
- 执行上下文通过 Redis 临时存储（检查点），完成后持久化到 MySQL
- 人工审核节点触发 `PAUSED_FOR_REVIEW` 状态，需要外部 `resumeExecution` 恢复
- 条件分支剪枝在 `advance()` 中自动处理，未选中分支递归标记为 SKIPPED
- WorkflowGraph 由 WorkflowGraphFactory 从 JSON 解析，支持版本化（草稿/已发布/指定版本）

## 变更日志
- [初始] 从现有代码逆向生成蓝图
- [2026-02-08] 补充 Execution 核心方法、条件剪枝、枚举类型、ExecutionMode 等完整细节
