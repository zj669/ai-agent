# AgentApplicationService Blueprint

## 职责契约
- **做什么**: Agent 用例编排——协调领域对象完成 Agent 的创建、更新、发布、回滚等用例；处理 DTO 转换
- **不做什么**: 不包含核心业务逻辑（那是 Domain 层的职责）；不直接操作数据库

## 接口摘要

| 方法 | 输入 | 输出 | 副作用 | 约束 |
|------|------|------|--------|------|
| createAgent | CreateAgentCmd | AgentDTO | 调用领域服务创建 | @Transactional |
| updateAgent | UpdateAgentCmd | AgentDTO | 调用领域服务更新 | @Transactional |
| publishAgent | agentId | AgentVersionDTO | 创建版本快照 | @Transactional |
| rollbackAgent | agentId, versionId | AgentDTO | 恢复历史版本 | @Transactional |
| getAgent | agentId | AgentDTO | 查询 | - |
| listAgents | userId, page | PageDTO<AgentDTO> | 查询 | 分页 |

## 依赖拓扑
- **上游**: AgentController
- **下游**: Agent(聚合根), AgentRepository(端口)

## 设计约束
- 所有写操作必须在 @Transactional 中执行
- DTO 与领域对象的转换在此层完成
- 不允许将领域对象直接暴露给接口层

## 变更日志
- [初始] 从现有代码逆向生成蓝图
