# TriggerPhase.java 蓝图

## Metadata
- title: TriggerPhase
- type: value-object (enum)
- summary: 人工审核触发阶段枚举，决定在节点执行前还是执行后暂停

## 枚举值

| 值 | 说明 | 行为 |
|---|------|------|
| `BEFORE_EXECUTION` | 执行前暂停 | 审核人可修改输入，通过后节点才开始执行 |
| `AFTER_EXECUTION` | 执行后暂停 | 审核人可查看/修改输出，通过后结果流转到下游 |

## 使用位置
- `HumanReviewConfig.triggerPhase`：节点配置中指定触发阶段
- `Execution.pausedPhase`：记录当前暂停的阶段
- `SchedulerService.checkPause()`：匹配阶段决定是否暂停
- `SchedulerService.resumeExecution()`：根据阶段决定恢复策略（BEFORE→重新执行节点，AFTER→跳过执行直接推进）

## 变更记录
- 2026-03-17: 初始蓝图生成
