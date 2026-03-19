# HumanReviewRecord.java 蓝图

## Metadata
- title: HumanReviewRecord
- type: entity
- summary: 人工审核记录实体，不可变审计日志，记录每次审核的决策、修改数据和审核意见

## 字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | String | 记录ID |
| `executionId` | String | 工作流执行ID |
| `nodeId` | String | 触发审核的节点ID |
| `reviewerId` | Long | 审核人ID |
| `decision` | String | 审核决策：APPROVE / REJECT |
| `triggerPhase` | TriggerPhase | 触发阶段 |
| `originalData` | String | 原始数据快照（JSON） |
| `modifiedData` | String | 修改后的数据（JSON） |
| `comment` | String | 审核意见 |
| `reviewedAt` | LocalDateTime | 审核时间 |

## 持久化
- 对应表：`workflow_human_review_record`
- PO 类：`HumanReviewPO`
- 仓储：`HumanReviewRepository` → `HumanReviewRepositoryImpl`

## 已知问题
- 当前 `decision` 字段在 `resumeExecution` 中硬编码为 `"APPROVE"`，拒绝功能未实现

## 变更记录
- 2026-03-17: 初始蓝图生成
