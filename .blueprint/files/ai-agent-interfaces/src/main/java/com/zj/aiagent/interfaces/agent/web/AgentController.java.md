## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/agent/web/AgentController.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: AgentController.java
- 智能体聚合的接口层控制器，负责将 HTTP 请求转换为 `AgentCommand` 并调用 `AgentApplicationService` 执行创建、更新、发布、回滚、删除与查询。

## 2) 核心方法
- `createAgent(SaveAgentRequest req)`
- `updateAgent(SaveAgentRequest req)`
- `publishAgent(PublishAgentRequest req)`
- `rollbackAgent(RollbackAgentRequest req)`
- `listAgents()`
- `getAgent(Long id)`

## 3) 具体方法
### 3.1 createAgent(SaveAgentRequest req)
- 函数签名: `createAgent(AgentRequest.SaveAgentRequest req) -> Response<Long>`
- 入参: 智能体基础信息
- 出参: 新建智能体 ID
- 功能含义: 从 `UserContext` 获取用户，组装 `CreateAgentCmd`，交由应用服务完成创建。
- 链路作用: 前端创建请求 -> Command 映射 -> 应用层编排 -> 领域写入。

## 4) 变更记录
- 2026-02-15: 基于源码回填 Agent 控制器命令/查询职责与主链路。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
