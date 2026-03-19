# HumanReviewPO.java 蓝图

## Metadata
- title: HumanReviewPO
- type: persistence-object
- summary: 人工审核记录持久化对象，映射 `workflow_human_review_record` 表

## 表映射
- 表名: `workflow_human_review_record`
- 主键: `id` (AUTO_INCREMENT)
- 索引: `idx_execution_id`, `idx_reviewer_id`

## 字段映射

| PO 字段 | 表字段 | 类型 | 说明 |
|---------|--------|------|------|
| id | id | Long | 记录ID |
| executionId | execution_id | String | 工作流执行ID |
| nodeId | node_id | String | 节点ID |
| reviewerId | reviewer_id | Long | 审核人ID |
| decision | decision | String | 审核决定 |
| triggerPhase | trigger_phase | TriggerPhase | 触发阶段 |
| originalData | original_data | String (JSON) | 原始数据 |
| modifiedData | modified_data | String (JSON) | 修改后数据 |
| comment | comment | String | 审核备注 |
| reviewedAt | reviewed_at | LocalDateTime | 审核时间 |

## 关联表（未使用）
- `workflow_human_review_task`：审核任务表，有 task_id/status/input_data/output_data 等字段，但代码中无对应 PO/Mapper

## 变更记录
- 2026-03-17: 初始蓝图生成
