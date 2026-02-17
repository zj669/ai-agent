# Blueprint Mirror: ai-agent-application/src/main/java/com/zj/aiagent/application/agent/cmd/AgentCommand.java

## Metadata
- file: `ai-agent-application/src/main/java/com/zj/aiagent/application/agent/cmd/AgentCommand.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-application/src/main/java/com/zj/aiagent/application/agent/cmd/AgentCommand.java
- Type: .java

## Responsibility
- 定义 Agent 应用层命令对象，承载控制层入参到应用服务的传输契约。

## Key Symbols / Structure
- `CreateAgentCmd`: `userId/name/description/icon`
- `UpdateAgentCmd`: `id/userId/name/description/icon/graphJson/version`
- `PublishAgentCmd`: `id/userId`
- `RollbackAgentCmd`: `id/userId/targetVersion`
- `DeleteAgentCmd`: `id/userId`
- `DeleteVersionCmd`: `agentId/userId/version`
- `DebugAgentCmd`: `agentId/userId/inputMessage/debugMode`

## Dependencies
- Lombok `@Data`。

## Notes
- 状态: 正常
- 仅作命令载体，不包含业务行为。
