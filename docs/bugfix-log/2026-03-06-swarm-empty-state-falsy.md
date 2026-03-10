# Bug 记录：发送消息后页面不显示消息和 thinking 气泡

| 项目 | 内容 |
|------|------|
| **日期** | 2026-03-06 |
| **模块** | 前端 `ai-agent-foward` / Swarm 聊天模块 |
| **严重度** | P0（消息完全不可见，核心功能阻塞） |
| **发现方式** | 用户测试反馈 |

---

## 问题描述

用户在 Swarm 聊天页面发送消息后，消息不显示在聊天区域。页面仍然展示空状态文案「向 assistant 发送消息开始对话」，但终止按钮（红色）已经出现，说明 streaming 状态已激活。

## 根因

`SwarmChatPanel.tsx` 第 59 行的空状态条件使用了 `!streamingContent`：

```tsx
{messages.length === 0 && !streamingContent ? (
  // 空状态
) : (
  // 消息列表
)}
```

当 `handleSend` 设置 `streamingContent = ''`（空字符串，表示 thinking 状态）时：
- `!''` 在 JavaScript 中为 `true`
- 因此 `messages.length === 0 && !streamingContent` 为 `true`
- 空状态持续展示，消息列表和 thinking 气泡被遮挡

而 `isStreaming = streamingContent !== null`（`'' !== null` 为 `true`），所以终止按钮能正确显示。

**空字符串 `''` 的双重语义冲突**：作为 streaming 标志它是"已激活"，但作为布尔判断它是 falsy。

---

## 修复方案

将空状态条件从 truthy/falsy 判断改为严格的 `null` 检查：

```tsx
// 修复前（空字符串被视为 falsy）
{messages.length === 0 && !streamingContent ? (

// 修复后（只有 null 才是"无 streaming"）
{messages.length === 0 && streamingContent === null ? (
```

---

## 涉及文件

| 文件 | 操作 | 改动要点 |
|------|------|----------|
| `ai-agent-foward/src/modules/swarm/components/chat/SwarmChatPanel.tsx` | 修改 | 第 59 行：`!streamingContent` → `streamingContent === null` |

---

## 验证

- `npx tsc --noEmit` → 零错误
- Vite 热更新生效，刷新页面后发送消息：消息立即出现 + thinking 气泡立即出现
