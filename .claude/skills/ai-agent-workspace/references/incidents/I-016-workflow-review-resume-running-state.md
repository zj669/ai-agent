# I-016 workflow-review-resume-running-state

## 故障签名

- 工作流在 LLM 节点人工审核前暂停后，UI 点击 `修改并恢复执行`，接口返回成功但节点没有继续跑。
- 后端日志出现 `Accepted 0/1 pending nodes`，随后没有 `Dispatching node: <llm-node>`。
- 人工审核队列移除了 pending 项，但 execution 没有进入后续节点执行。

## 根因

`Execution.resume(...)` 在 `BEFORE_EXECUTION` 恢复 paused node 时返回了待恢复节点，但没有把该节点状态恢复为 `PENDING`。调度器只接受 `PENDING` 节点进入 `markRunning(...)`，因此被过滤成 0 个可派发节点。

## 修复原则

- `BEFORE_EXECUTION` 恢复时，paused node 应先回到 `PENDING`，再交给调度器标记为 `RUNNING` 并执行。
- `AFTER_EXECUTION` 恢复不能重新执行已经完成的节点，应保持原有结果继续推进后继节点。
- 不要直接绕过状态机派发节点；必须让 Execution 聚合根维护节点状态转换。

## 验证记录

- 修复后 Browser Relay 真实 UI 恢复 executionId=`8445e5fe-d9b5-496e-a584-22316e284fb5`。
- 后端日志出现 `Dispatching node: llm-1773740239536-2`、`Node llm completed with status: SUCCEEDED`。

