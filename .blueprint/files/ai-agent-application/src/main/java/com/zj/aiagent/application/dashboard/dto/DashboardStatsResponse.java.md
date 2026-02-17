# Blueprint Mirror: ai-agent-application/src/main/java/com/zj/aiagent/application/dashboard/dto/DashboardStatsResponse.java

## Metadata
- file: `ai-agent-application/src/main/java/com/zj/aiagent/application/dashboard/dto/DashboardStatsResponse.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-application/src/main/java/com/zj/aiagent/application/dashboard/dto/DashboardStatsResponse.java
- Type: .java

## Responsibility
- Dashboard 统计数据响应 DTO。

## Key Symbols / Structure
- 字段：
  - `agentCount`
  - `workflowCount`
  - `conversationCount`
  - `knowledgeDatasetCount`
  - `totalExecutions`
  - `successfulExecutions`
  - `failedExecutions`
  - `avgResponseTime`

## Dependencies
- Lombok `@Data/@Builder/@NoArgsConstructor/@AllArgsConstructor`

## Notes
- 状态: 正常
- 作为应用层到接口层的统计快照载体。
