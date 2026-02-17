# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/HumanReviewRepository.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/HumanReviewRepository.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/HumanReviewRepository.java
- Type: .java (interface)
- Status: 正常

## Responsibility
- 人工审核记录仓储端口，定义审核记录写入、按执行查询与分页历史查询契约。

## Key Symbols / Structure
- `save(HumanReviewRecord record)`
- `findByExecutionId(String executionId)`
- `findReviewHistory(Long userId, Pageable pageable)`

## Dependencies
- `HumanReviewRecord`
- `List`
- Spring Data 分页类型：`Page`, `Pageable`

## Notes
- 领域接口中保留分页查询契约以支持审核历史检索场景。
