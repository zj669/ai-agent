## Metadata
- file: `.blueprint/domain/workflow/HumanReview.md`
- version: `1.0`
- status: 修改完成
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: HumanReview
- 该文件用于描述 HumanReview 的职责边界与协作关系。

## 2) 核心方法
- `save()`
- `findByExecutionId()`
- `findReviewHistory()`
- `addToPendingQueue()`
- `removeFromPendingQueue()`

## 3) 具体方法
### 3.1 save()
- 函数签名: `void save(HumanReviewRecord record)`
- 入参:
  - `record`: 人工审核记录实体（包含 executionId、nodeId、reviewerId、decision、triggerPhase、modifiedData、comment 等）
- 出参: 无（void）
- 功能含义: 保存人工审核记录到数据库，记录审核决策和修改内容，用于审计追踪。
- 链路作用: 在 SchedulerService.resumeExecution() 中调用，持久化审核日志。

### 3.2 findByExecutionId()
- 函数签名: `List<HumanReviewRecord> findByExecutionId(String executionId)`
- 入参:
  - `executionId`: 执行ID
- 出参: 审核记录列表（按时间排序）
- 功能含义: 查询指定执行的所有审核记录，用于审核历史展示。
- 链路作用: 在 HumanReviewController 中调用，提供审核历史查询接口。

### 3.3 findReviewHistory()
- 函数签名: `Page<HumanReviewRecord> findReviewHistory(Long userId, Pageable pageable)`
- 入参:
  - `userId`: 审核人ID
  - `pageable`: 分页参数
- 出参: 审核记录分页结果
- 功能含义: 查询指定审核人的历史审核记录（分页），用于个人审核历史查询。
- 链路作用: 在 HumanReviewController 中调用，提供审核人历史记录接口。

### 3.4 addToPendingQueue()
- 函数签名: `void addToPendingQueue(String executionId)`（HumanReviewQueuePort 接口方法）
- 入参:
  - `executionId`: 执行ID
- 出参: 无（void）
- 功能含义: 将执行添加到 Redis 待审核队列（Set 结构），用于待审核列表查询。
- 链路作用: 在 SchedulerService.checkPause() 中调用，标记执行进入待审核状态。

### 3.5 removeFromPendingQueue()
- 函数签名: `void removeFromPendingQueue(String executionId)`（HumanReviewQueuePort 接口方法）
- 入参:
  - `executionId`: 执行ID
- 出参: 无（void）
- 功能含义: 从 Redis 待审核队列移除执行，标记审核完成。
- 链路作用: 在 SchedulerService.resumeExecution() 中调用，清理待审核标记。


## 4) 变更记录
- 2026-02-15: 后端MVP修复（审核恢复）：`SchedulerService.resumeExecution` 与 `Execution.resume` 双重校验 `pausedNodeId` 与请求 `nodeId` 一致，避免错误节点恢复。
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全方法签名与语义，从 HumanReviewRepository.java 和 HumanReviewQueuePort.java 提取真实实现契约。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
