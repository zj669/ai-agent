## Metadata
- file: `.blueprint/domain/agent/AgentService.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: AgentService
- 该文件用于描述 AgentService 的职责边界与协作关系。

## 2) 核心方法
- `createAgent()`
- `updateAgent()`
- `publishAgent()`
- `rollbackAgent()`

## 3) 具体方法
### 3.1 createAgent()
- 函数签名: `Long createAgent(AgentCommand.CreateAgentCmd cmd)`（AgentApplicationService）
- 入参:
  - `cmd`: 创建 Agent 命令（包含 userId、name、description、icon）
- 出参: Agent ID（Long）
- 功能含义: 创建 Agent，生成初始化 graphJson（包含唯一 dagId），构建 Agent 实体并持久化。
- 链路作用: 在 AgentController 中调用，执行 Agent 创建逻辑。

### 3.2 updateAgent()
- 函数签名: `void updateAgent(AgentCommand.UpdateAgentCmd cmd)`（AgentApplicationService）
- 入参:
  - `cmd`: 更新 Agent 命令（包含 id、userId、name、description、icon、graphJson、version）
- 出参: 无（void）
- 功能含义: 更新 Agent 配置，检查所有权，调用 Agent.updateConfig() 更新配置（包含乐观锁版本检查），持久化。
- 链路作用: 在 AgentController 中调用，执行 Agent 配置更新逻辑。

### 3.3 publishAgent()
- 函数签名: `void publishAgent(AgentCommand.PublishAgentCmd cmd)`（AgentApplicationService）
- 入参:
  - `cmd`: 发布 Agent 命令（包含 id、userId）
- 出参: 无（void）
- 功能含义: 发布 Agent 版本，检查所有权，调用 Agent.publish() 创建版本快照，保存版本记录，更新 Agent 的 publishedVersionId。
- 链路作用: 在 AgentController 中调用，执行 Agent 版本发布逻辑。

### 3.4 rollbackAgent()
- 函数签名: `void rollbackAgent(AgentCommand.RollbackAgentCmd cmd)`（AgentApplicationService）
- 入参:
  - `cmd`: 回滚 Agent 命令（包含 id、userId、targetVersion）
- 出参: 无（void）
- 功能含义: 回滚 Agent 到指定版本，检查所有权，查询目标版本，调用 Agent.rollbackTo() 恢复配置，持久化。
- 链路作用: 在 AgentController 中调用，执行 Agent 版本回滚逻辑。


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全方法签名与语义，从 AgentApplicationService.java 提取真实实现契约。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
