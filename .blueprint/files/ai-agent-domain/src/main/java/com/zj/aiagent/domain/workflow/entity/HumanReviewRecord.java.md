# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/HumanReviewRecord.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/HumanReviewRecord.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/HumanReviewRecord.java
- Type: .java
- Status: 正常

## Responsibility
- 人工审核审计记录实体，记录触发审核后的决策、数据快照与审核意见。
- 作为审核历史与追踪能力的数据模型，由仓储端口持久化。

## Key Symbols / Structure
- 字段：`id`, `executionId`, `nodeId`, `reviewerId`, `decision`, `triggerPhase`, `originalData`, `modifiedData`, `comment`, `reviewedAt`。

## Dependencies
- `TriggerPhase`
- `LocalDateTime`

## Notes
- 该对象偏审计日志语义，主要由 Application/Infrastructure 协同写入与查询。
