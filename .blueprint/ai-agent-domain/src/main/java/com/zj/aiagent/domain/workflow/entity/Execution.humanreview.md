# Execution.java 人工审核蓝图（补充）

## Metadata
- title: Execution（人工审核相关）
- type: entity (aggregate-root)
- summary: Execution 聚合根中与人工审核相关的字段和方法，管理暂停/恢复状态机

## 审核相关字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `pausedNodeId` | String | 当前暂停的节点ID，暂停时设置，恢复时清空 |
| `pausedPhase` | TriggerPhase | 当前暂停的阶段，暂停时设置，恢复时清空 |
| `reviewedNodes` | Set<String> | 已通过人工审核的节点集合，防止 resume 后重复触发暂停 |

## 关键方法单元

### advance（暂停分支）
- location: Execution.advance(nodeId, result)
- 触发条件: `result.isPaused()` 为 true
- 行为:
  1. `nodeStatuses.put(nodeId, PAUSED_FOR_REVIEW)`
  2. 如果 `result.getOutputs() != null`，存入 `context.setNodeOutput(nodeId, outputs)`
  3. `this.status = PAUSED_FOR_REVIEW`
  4. 记录 `pausedNodeId` 和 `pausedPhase`
  5. 返回空列表（不再调度后续节点）

### resume
- location: Execution.resume(nodeId, additionalInputs)
- 前置校验:
  - 状态必须是 `PAUSED_FOR_REVIEW` 或 `PAUSED`
  - `pausedNodeId` 必须与传入的 `nodeId` 匹配
- 行为:
  1. 合并 `additionalInputs` 到 `context.sharedState`
  2. 获取 `pausedPhase`（兼容旧数据默认 AFTER_EXECUTION）
  3. 重置状态为 `RUNNING`，清空 `pausedNodeId` / `pausedPhase`
  4. `reviewedNodes.add(nodeId)` — 标记已审核
  5. 返回值取决于 phase:
     - `BEFORE_EXECUTION` → `[pausedNode]`（需要重新执行）
     - `AFTER_EXECUTION` → `[]`（已有结果，由 Scheduler 手动推进）

### isNodeReviewed
- location: Execution.isNodeReviewed(nodeId)
- purpose: 检查节点是否已通过审核，`SchedulerService.checkPause()` 用此避免重复暂停

## 状态流转

```
RUNNING → advance(paused) → PAUSED_FOR_REVIEW
PAUSED_FOR_REVIEW → resume() → RUNNING
```

## 变更记录
- 2026-03-17: 初始蓝图生成
