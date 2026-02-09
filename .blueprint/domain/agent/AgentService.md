# AgentService Blueprint

## 职责契约
- **做什么**: 管理 Agent 的完整生命周期——创建、配置、版本管理、发布、回滚；维护 Agent 与 WorkflowGraph 的关联
- **不做什么**: 不负责工作流的执行调度（那是 WorkflowEngine 的职责）；不负责对话管理；不直接操作数据库

## 核心聚合根

### Agent
- Agent 聚合根，持有工作流图定义（JSON）、版本指针、发布状态
- 状态: DRAFT → PUBLISHED → ARCHIVED
- 每次发布创建 AgentVersion 快照，支持回滚

### AgentVersion
- 版本快照实体，记录图定义、版本号、发布时间
- 不可变，创建后不允许修改

## 接口摘要

| 方法 | 输入 | 输出 | 副作用 | 约束 |
|------|------|------|--------|------|
| createAgent | CreateAgentCmd | Agent | 写DB | userId 必须有效 |
| updateAgent | UpdateAgentCmd | Agent | 写DB | 仅 DRAFT 状态可修改 |
| publishAgent | agentId | AgentVersion | 创建版本快照, 更新状态 | 图必须通过校验 |
| rollbackAgent | agentId, versionId | Agent | 恢复图定义, 更新版本指针 | 目标版本必须存在 |
| getAgent | agentId | Agent | 无 | - |
| listAgents | userId, pagination | Page<Agent> | 无 | 分页查询 |

## 依赖拓扑
- **上游**: AgentController, AgentApplicationService
- **下游**: AgentRepository(端口)

## 领域事件
- 发布: 无（当前设计）
- 监听: 无

## 设计约束
- Agent 的图定义以 JSON 存储在 agent_info 表
- 版本快照存储在 agent_version 表
- WorkflowGraph 值对象从 JSON 解析，提供图校验能力（环检测等）

## 变更日志
- [初始] 从现有代码逆向生成蓝图
