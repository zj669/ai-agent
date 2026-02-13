# HumanReview Blueprint

## 职责契约
- **做什么**: 管理工作流中的人工审核环节——暂停执行、创建审核记录、处理审批决定（通过/拒绝/修改）、恢复执行
- **不做什么**: 不负责工作流调度逻辑（那是 SchedulerService 的职责）；不负责用户权限验证

## 核心实体

### HumanReviewRecord (实体 / 审计日志)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 记录ID |
| executionId | String | 执行ID |
| nodeId | String | 触发审核的节点ID |
| reviewerId | Long | 审核人ID |
| decision | String | 审核决策: `APPROVE` / `REJECT` |
| triggerPhase | TriggerPhase | 触发阶段: `BEFORE_EXECUTION` / `AFTER_EXECUTION` |
| originalData | String (JSON) | 原始数据快照 |
| modifiedData | String (JSON) | 修改后的数据 |
| comment | String | 审核意见 |
| reviewedAt | LocalDateTime | 审核时间 |

### HumanReviewConfig (配置)
- 嵌套在 NodeConfig 中
- 字段: `enabled`(是否启用), `prompt`(审核提示词), `editableFields`(可编辑字段列表)
- 通过 `Node.requiresHumanReview()` 快捷判断

## 触发阶段

### BEFORE_EXECUTION (审核输入)
1. 节点执行前暂停，创建 HumanReviewRecord
2. 审核人可修改节点输入参数
3. 审核通过后，用修改后的输入执行节点
4. `Execution.resume()` 返回待执行节点列表 → SchedulerService 调度执行

### AFTER_EXECUTION (审核输出)
1. 节点执行后暂停，创建 HumanReviewRecord
2. 审核人可修改节点输出结果
3. 审核通过后，用修改后的输出推进 DAG
4. `Execution.resume()` 返回空列表 → SchedulerService 手动调用 `advance()` 推进

## 端口接口

### HumanReviewRepository (Domain Port)
| 方法 | 输入 | 输出 | 说明 |
|------|------|------|------|
| save | HumanReviewRecord | void | 持久化审核记录到 MySQL |
| findByExecutionId | executionId | List\<HumanReviewRecord\> | 查询执行的所有审核记录 |
| findReviewHistory | userId, Pageable | Page\<HumanReviewRecord\> | 分页查询审核历史 |

### HumanReviewQueuePort (Domain Port)
| 方法 | 输入 | 输出 | 说明 |
|------|------|------|------|
| addToPendingQueue | executionId | void | 加入待审核队列 (Redis) |
| removeFromPendingQueue | executionId | void | 从待审核队列移除 |
| isInPendingQueue | executionId | boolean | 检查是否在待审核队列中 |

### WorkflowCancellationPort (Domain Port)
| 方法 | 输入 | 输出 | 说明 |
|------|------|------|------|
| markAsCancelled | executionId | void | 标记执行为已取消 |
| isCancelled | executionId | boolean | 检查执行是否已取消 |

## 依赖拓扑
- **上游**: HumanReviewController, SchedulerService
- **下游**: HumanReviewRepository(端口), HumanReviewQueuePort(端口)

## 审核流程 (完整时序)

```
1. SchedulerService.checkPause(node, phase)
   → 检查 node.requiresHumanReview()
   → 创建 HumanReviewRecord (originalData = 当前输入/输出)
   → humanReviewRepository.save(record)
   → humanReviewQueuePort.addToPendingQueue(executionId)
   → execution.advance(nodeId, NodeExecutionResult.paused(phase))
   → 保存检查点到 Redis

2. HumanReviewController.resumeExecution(executionId, nodeId, edits, decision)
   → SchedulerService.resumeExecution(...)
   → 加载检查点
   → 创建审核记录 (decision, modifiedData)
   → humanReviewQueuePort.removeFromPendingQueue(executionId)
   → execution.resume(nodeId, edits)
   → 根据 phase 决定后续:
     - BEFORE: 用修改后的输入调度节点执行
     - AFTER: 用修改后的输出调用 advance() 推进 DAG
```

## 数据持久化
- 待审核队列: Redis (`human_review:pending`)
- 审核记录: MySQL (`workflow_human_review_record` 表)
- 检查点: Redis (`execution:checkpoint:*`)

## 设计约束
- 审核记录作为不可变审计日志，创建后不修改
- 审核决定: `APPROVE`(通过) / `REJECT`(拒绝，标记执行失败)
- REJECT 时 SchedulerService 将执行标记为 FAILED
- 检查点通过 Redis 临时存储，保证暂停/恢复的状态一致性

## 安全约束
- 审核操作需要权限验证（审核员角色或工作流所有者）
- 审核操作具有幂等性（重复提交不会重复执行）
- 审核记录不可篡改（审计日志）
- 支持审核超时自动处理（可选）

## 权限模型
- **审核员角色**: 具有全局审核权限的用户（user_account.status 字段扩展或新增 role 字段）
- **工作流所有者**: 可以审核自己创建的工作流（通过 Execution.userId 判断）
- **权限检查**: HumanReviewController 在 resumeExecution 前验证权限

## 变更日志
- [初始] 从现有代码逆向生成蓝图
- [2026-02-08] 补充触发阶段详细流程、完整时序、WorkflowCancellationPort、数据持久化策略
- [2026-02-10] 新增安全约束、权限模型、审核超时机制；优化待审核列表查询性能
