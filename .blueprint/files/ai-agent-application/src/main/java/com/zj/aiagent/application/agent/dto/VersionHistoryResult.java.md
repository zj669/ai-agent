# Blueprint Mirror: ai-agent-application/src/main/java/com/zj/aiagent/application/agent/dto/VersionHistoryResult.java

## Metadata
- file: `ai-agent-application/src/main/java/com/zj/aiagent/application/agent/dto/VersionHistoryResult.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-application/src/main/java/com/zj/aiagent/application/agent/dto/VersionHistoryResult.java
- Type: .java

## Responsibility
- Agent 版本历史出参 DTO，提供领域版本对象到接口响应的转换。

## Key Symbols / Structure
- `VersionHistoryResult.versions`: `List<VersionItem>`
- `from(List<AgentVersion>)`: 批量转换
- `VersionItem`: `id/agentId/version/description/createTime`
- `VersionItem.from(AgentVersion)`: 单项转换

## Dependencies
- `com.zj.aiagent.domain.agent.entity.AgentVersion`
- Lombok `@Data/@Builder/@NoArgsConstructor/@AllArgsConstructor`

## Notes
- 状态: 正常
- 仅承载查询结果，不参与版本逻辑。
