# HumanReviewRepository.java 蓝图

## Metadata
- title: HumanReviewRepository
- type: port
- summary: 人工审核记录仓储接口，负责审核记录的持久化和查询

## 关键方法单元

### save
- purpose: 保存审核记录（审计日志）
- input: HumanReviewRecord
- 调用时机: `SchedulerService.resumeExecution()` 中保存审核决策

### findByExecutionId
- purpose: 根据执行ID查询审核记录列表
- input: executionId (String)
- output: List<HumanReviewRecord>

### findReviewHistory
- purpose: 分页查询审核历史
- input: userId (Long), pageable (Pageable)
- output: Page<HumanReviewRecord>
- 调用时机: `HumanReviewController.getHistory()`

## 实现
- `HumanReviewRepositoryImpl`：MyBatis Plus 实现，映射 `workflow_human_review_record` 表

## 变更记录
- 2026-03-17: 初始蓝图生成
