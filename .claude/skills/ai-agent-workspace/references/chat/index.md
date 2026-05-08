# 对话管理业务域索引

本文件为对话管理业务域提供二级路由，不重复根级全局安全规则。

## 业务范围

Conversation 聚合根、Message 管理、对话历史（STM）、与工作流执行的集成。

## Workflow 启动契约

- Chat UI 调用 workflow 时，用户输入必须放在 `inputs.inputMessage`。
- `inputs.query`、`inputs.input`、`inputs.message` 只用于后端读取旧数据时的兼容，不是新 UI 契约。
- 如果 workflow 图中下游引用 `start.output.inputMessage`，真实 UI payload 缺少 `inputMessage` 应当失败；不要通过后端任意兜底掩盖前端契约错误。
- 验证聊天页工作流执行必须使用 Browser Relay 走真实 UI：新建对话、输入消息、发送、处理人工审核弹窗、确认最终答案。

## 典型触发

- 创建/查询对话
- 消息发送与历史加载
- ChatController 相关接口排查
- 工作流完成后 `completeAssistantMessage` 相关问题

## 代码入口（支撑事实）

- 接口层：`ChatController.java`（路径 `/api/chat`）
- 应用层：`ChatApplicationService.java`
- 领域层：`ai-agent-domain/src/main/java/.../chat/`

## SOP 列表

| SOP | 文件 | 状态 |
|---|---|---|
| 对话功能开发 | `references/chat/feature-delivery.md` | 待创建 |
