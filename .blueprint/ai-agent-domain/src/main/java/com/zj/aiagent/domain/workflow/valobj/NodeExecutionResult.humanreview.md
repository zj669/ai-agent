# NodeExecutionResult.java 蓝图（暂停相关补充）

## Metadata
- title: NodeExecutionResult（暂停相关）
- type: value-object
- summary: 节点执行结果值对象中与人工审核暂停相关的字段和工厂方法

## 暂停相关字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `triggerPhase` | TriggerPhase | 暂停阶段，仅 paused 结果使用 |

## 工厂方法

### paused(phase)
- 构造: `status=PAUSED_FOR_REVIEW, triggerPhase=phase, outputs=null`
- 用途: BEFORE_EXECUTION 暂停（无输出）

### paused(phase, outputs)
- 构造: `status=PAUSED_FOR_REVIEW, triggerPhase=phase, outputs=outputs`
- 用途: AFTER_EXECUTION 暂停（携带节点执行输出）
- 调用方: `SchedulerService.checkPause()` L506

## 判断方法

### isPaused()
- 逻辑: `status == PAUSED || status == PAUSED_FOR_REVIEW`
- 调用方: `Execution.advance()` L157 判断是否进入暂停分支

## 变更记录
- 2026-03-17: 初始蓝图生成
