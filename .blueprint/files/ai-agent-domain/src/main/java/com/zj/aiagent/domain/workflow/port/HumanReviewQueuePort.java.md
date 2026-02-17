# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/HumanReviewQueuePort.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/HumanReviewQueuePort.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/HumanReviewQueuePort.java
- Type: .java (interface)
- Status: 正常

## Responsibility
- 人工审核待处理队列端口，定义待审核执行的入队、出队与查询能力。
- 为人工审核流程提供轻量队列契约，具体存储由 Infrastructure 实现。

## Key Symbols / Structure
- `addToPendingQueue(String executionId)`
- `removeFromPendingQueue(String executionId)`
- `isInPendingQueue(String executionId)`
- `getPendingExecutionIds()`

## Dependencies
- `Set<String>`

## Notes
- 该端口仅描述队列操作语义，不绑定 Redis/DB 等具体实现。
