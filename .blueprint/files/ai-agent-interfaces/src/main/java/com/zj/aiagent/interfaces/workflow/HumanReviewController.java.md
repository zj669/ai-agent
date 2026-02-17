## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/HumanReviewController.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: HumanReviewController.java
- 人工审核控制器，提供待审核列表、审核详情、恢复执行与审核历史查询接口，桥接 `SchedulerService` 与审核仓储。

## 2) 核心方法
- `getPendingReviews()`
- `getReviewDetail(String executionId)`
- `resumeExecution(ResumeExecutionRequest request)`
- `getHistory(Long userId, Pageable pageable)`

## 3) 具体方法
### 3.1 resumeExecution(ResumeExecutionRequest request)
- 函数签名: `resumeExecution(HumanReviewDTO.ResumeExecutionRequest request) -> ResponseEntity<Void>`
- 入参: 执行 ID、节点 ID、编辑内容与审核意见
- 出参: `200 OK`/`401`
- 功能含义: 从 `UserContext` 读取审核人并调用 `schedulerService.resumeExecution` 恢复工作流。
- 链路作用: 人工审核通过后恢复执行主链路。

## 4) 变更记录
- 2026-02-15: 基于源码回填人工审核控制器接口职责与恢复链路。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
