# Blueprint Mirror: ai-agent-application/src/main/java/com/zj/aiagent/application/agent/dto/AgentRequest.java

## Metadata
- file: `ai-agent-application/src/main/java/com/zj/aiagent/application/agent/dto/AgentRequest.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-application/src/main/java/com/zj/aiagent/application/agent/dto/AgentRequest.java
- Type: .java

## Responsibility
- 定义 Agent 控制层入参 DTO 与校验规则（创建/更新/发布/回滚/调试）。

## Key Symbols / Structure
- 校验分组：`Create`, `Update`
- `SaveAgentRequest`: 创建/更新共用请求（含乐观锁 `version`）
- `PublishAgentRequest`: 发布请求
- `RollbackAgentRequest`: 回滚请求
- `DebugAgentRequest`: 调试请求

## Dependencies
- Jakarta Validation: `@NotNull/@NotEmpty/@Null`
- Lombok `@Data`

## Notes
- 状态: 正常
- 通过分组约束区分 create/update 场景。
