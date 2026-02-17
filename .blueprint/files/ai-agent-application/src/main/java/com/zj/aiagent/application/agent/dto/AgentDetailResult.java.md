# Blueprint Mirror: ai-agent-application/src/main/java/com/zj/aiagent/application/agent/dto/AgentDetailResult.java

## Metadata
- file: `ai-agent-application/src/main/java/com/zj/aiagent/application/agent/dto/AgentDetailResult.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-application/src/main/java/com/zj/aiagent/application/agent/dto/AgentDetailResult.java
- Type: .java

## Responsibility
- Agent 详情出参 DTO，隔离领域实体与接口层响应。

## Key Symbols / Structure
- 字段：`id/name/description/icon/graphJson/version/publishedVersionId/status`
- `from(Agent agent)`: 将领域实体转换为应用层响应对象。

## Dependencies
- `com.zj.aiagent.domain.agent.entity.Agent`
- Lombok `@Data`

## Notes
- 状态: 正常
- `status` 由 `AgentStatus` 映射为整型 code。
