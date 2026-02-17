# Blueprint Mirror: ai-agent-application/src/main/java/com/zj/aiagent/application/agent/service/AgentApplicationService.java

## Metadata
- file: `ai-agent-application/src/main/java/com/zj/aiagent/application/agent/service/AgentApplicationService.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-application/src/main/java/com/zj/aiagent/application/agent/service/AgentApplicationService.java
- Type: .java

## Responsibility
- 编排 Agent 生命周期用例：创建、更新、发布、回滚、删除、详情与版本历史查询。
- 加载并生成初始工作流模板（含唯一 `dagId`），协调领域聚合与仓储落库。

## Key Symbols / Structure
- 初始化
  - `init()`: 从 `templates/agent-initial-graph.json` 加载模板。
  - `generateInitialGraphJson()`: 注入随机 `dagId` 后生成 graphJson。
- 创建与变更
  - `createAgent(CreateAgentCmd)`
  - `updateAgent(UpdateAgentCmd)`
- 版本管理
  - `publishAgent(PublishAgentCmd)`
  - `rollbackAgent(RollbackAgentCmd)`
  - `deleteAgentVersion(DeleteVersionCmd)`
- 删除策略
  - `deleteAgent(DeleteAgentCmd)`: 软删除（`deleted=1`）
  - `forceDeleteAgent(DeleteAgentCmd)`: 先删全部版本再删主体
- 查询
  - `getAgentDetail(Long, Long)`
  - `listAgents(Long)`
  - `getVersionHistory(Long, Long)`
- 权限
  - `checkOwnership(Agent, Long)`

## Dependencies
- Domain: `Agent`, `AgentVersion`, `AgentRepository`, `GraphValidator`
- Application DTO/Cmd: `AgentCommand`, `AgentDetailResult`, `VersionHistoryResult`
- Infra libs: Jackson `ObjectMapper`, Spring `@Service/@Transactional/@PostConstruct`

## Notes
- 状态: 正常
- 发布流程中会在保存版本后回填 `publishedVersionId` 到 Agent。
