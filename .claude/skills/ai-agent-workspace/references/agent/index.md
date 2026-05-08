# Agent管理业务域索引

本文件为 Agent 管理业务域提供二级路由，不重复根级全局安全规则。

## 业务范围

Agent 聚合根的 CRUD 管理、模型配置、ChatModelPort 动态创建。

## 典型触发

- 创建/更新/删除 Agent
- 配置 AI 模型参数（API key、base URL、model name）
- AgentController 相关接口排查

## 核心约束

- Spring AI 自动配置已禁用，不依赖自动注入的 `ChatClient` 或 `ChatModel` bean
- 模型通过用户配置，由 `ChatModelPort` 动态创建
- `OpenAiChatModelAdapter` 是 `ChatModelPort` 的基础设施适配器

## 代码入口（支撑事实）

- 接口层：`AgentController.java`（路径 `/api/agent`）
- 领域层：`ai-agent-domain/src/main/java/.../agent/`
- 基础设施：`OpenAiChatModelAdapter.java`

## SOP 列表

| SOP | 文件 | 状态 |
|---|---|---|
| Agent 功能开发 | `references/agent/feature-delivery.md` | 待创建 |
| Agent 配置排查 | `references/agent/triage.md` | 待创建 |
