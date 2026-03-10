# Bug 记录：Agent 思考/流式输出时无终止按钮

| 项目 | 内容 |
|------|------|
| **日期** | 2026-03-06 |
| **模块** | 前端 `ai-agent-foward` / Swarm 聊天模块 |
| **严重度** | P1（用户无法中断长时间运行的 Agent） |
| **发现方式** | 用户反馈 |

---

## 问题描述

当 Agent 正在思考或流式输出回复时（`streamingContent !== null`），输入区域仅有一个 disabled 的发送按钮，用户**无法主动终止** Agent 的推理过程。

后端 `POST /api/swarm/agent/{id}/stop` API 和前端 `stopAgent()` 函数均已存在，但仅在 `WaitingCard`（等待子 Agent 回复）中使用，主聊天输入区域缺少终止入口。

---

## 修复方案

当 Agent 处于 thinking/streaming 状态时，将发送按钮替换为红色终止按钮：

```
正常状态:  [____输入框____] [发送]
思考/流式: [____输入框(disabled)____] [终止(红色)]
```

点击终止后：调用 `stopAgent` -> 清除流式状态 -> 刷新消息和 Agent 列表。

---

## 涉及文件

| 文件 | 操作 | 改动要点 |
|------|------|----------|
| `ai-agent-foward/src/modules/swarm/components/chat/SwarmComposer.tsx` | 修改 | Props 增加 `isStreaming` / `onStop`；streaming 时渲染红色终止按钮（`StopOutlined` + `danger`） |
| `ai-agent-foward/src/modules/swarm/components/chat/SwarmChatPanel.tsx` | 修改 | Props 增加 `onStop` / `isStreaming`，透传给 `SwarmComposer` |
| `ai-agent-foward/src/modules/swarm/pages/SwarmMainPage.tsx` | 修改 | 新增 `handleStop` 回调（stopAgent -> 清除状态 -> 刷新）；计算 `isStreaming`；传递到 `SwarmChatPanel` |

---

## 验证

- `npx tsc --noEmit` → 零错误
- Lint 检查 → 零错误
