# HumanReviewConfig.java 蓝图

## Metadata
- title: HumanReviewConfig
- type: value-object
- summary: 节点级人工审核配置，控制是否启用审核、审核提示语、可编辑字段列表和触发阶段（执行前/执行后）

## 字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `enabled` | boolean | 是否开启人工审核 |
| `prompt` | String | 审核提示信息（展示给审核人） |
| `editableFields` | String[] | 可编辑字段列表 |
| `triggerPhase` | TriggerPhase | 触发阶段：BEFORE_EXECUTION / AFTER_EXECUTION |

## 使用方式
- 嵌套在 `NodeConfig.humanReviewConfig` 中，每个节点独立配置
- `NodeConfig.requiresHumanReview()` 委托给 `enabled` 字段判断
- `SchedulerService.checkPause()` 读取 `triggerPhase` 决定在哪个阶段暂停

## 变更记录
- 2026-03-17: 初始蓝图生成
