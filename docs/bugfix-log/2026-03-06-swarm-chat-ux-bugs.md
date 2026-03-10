# Bug 记录：Swarm 聊天消息发送 UX 问题

| 项目 | 内容 |
|------|------|
| **日期** | 2026-03-06 |
| **模块** | 前端 `ai-agent-foward` / Swarm 聊天模块 |
| **严重度** | P1（交互体验严重降级，用户不知道消息是否发送成功） |
| **发现方式** | 用户测试反馈 |

---

## 问题描述

### Bug 1：消息发送后不及时出现

**现象**：用户在输入框输入消息点击发送后，自己发送的消息**不会立即出现**在聊天列表中，需要等待后端 API 返回后才渲染，造成明显的卡顿感（通常延迟 200ms~1s）。

**根因**：`useSwarmMessages.ts` 的 `send` 方法先等待 `sendMessage` API 调用完成，再调用 `setMessages` 更新列表。

```ts
// 修复前：等待 API 返回才更新 UI
const send = async (senderId, content) => {
  const msg = await sendMessage(groupId, senderId, content) // 等待网络
  setMessages(prev => [...prev, msg])                       // 才渲染
}
```

### Bug 2：Agent 收到消息后没有"正在思考"指示

**现象**：消息发送成功后，页面没有任何 loading 状态，用户不知道 Agent 是否收到消息并正在处理。需要等到 LLM 开始输出（SSE `stream.start`）后，才出现流式气泡，期间可能有数秒空白。

**根因**：
1. `handleSend` 仅调用 `send`，未在发送后立即设置 `streamingContent` 和 `streamingAgentId`
2. `SwarmMessageList` 只在 `streamingContent` 有实际文字时才渲染流式气泡（空字符串时不显示任何内容）
3. `SwarmMessageBubble` 没有"思考中"的专用渲染逻辑

---

## 修复方案

### 修复一：乐观更新（Optimistic Update）

`useSwarmMessages.ts` 中，发送前立即插入一条临时消息（负数 ID），API 返回后替换为真实消息；失败时回滚移除：

```ts
const send = async (senderId, content) => {
  const optimistic: SwarmMessage = {
    id: -Date.now(),  // 临时负数 ID
    groupId, senderId, content,
    contentType: 'text',
    sendTime: new Date().toISOString(),
  }
  setMessages(prev => [...prev, optimistic])  // 立即渲染
  try {
    const msg = await sendMessage(groupId, senderId, content)
    setMessages(prev => prev.map(m => m.id === optimistic.id ? msg : m))  // 替换真实消息
  } catch {
    setMessages(prev => prev.filter(m => m.id !== optimistic.id))  // 失败时回滚
  }
}
```

### 修复二：发送后立即显示 thinking 气泡

`SwarmMainPage.tsx` 的 `handleSend` 中，在 `await send()` 之前设置 `streamingContent=''` 和 `streamingAgentId`，让 thinking 气泡立刻出现：

```ts
const handleSend = async (content) => {
  if (!humanAgentId || !selectedAgentId) return
  setStreamingContent('')          // 立即触发 thinking 气泡
  setStreamingAgentId(selectedAgentId)
  await send(humanAgentId, content)
}
```

### 修复三：`thinking` 类型气泡渲染

**`SwarmMessageList.tsx`**：`streamingContent` 为空字符串时标记 `contentType: 'thinking'`：

```ts
content: streamingContent === '' ? '正在思考...' : streamingContent + '▌',
contentType: streamingContent === '' ? 'thinking' : 'text',
```

**`SwarmMessageBubble.tsx`**：新增 `thinking` 类型渲染，展示旋转加载图标 + 灰色文字：

```tsx
} : isThinking ? (
  <div style={{ padding: '8px 12px', borderRadius: 8, background: '#f0f0f0',
                color: '#8c8c8c', display: 'flex', alignItems: 'center', gap: 8 }}>
    <LoadingOutlined style={{ fontSize: 14 }} />
    <span>正在思考...</span>
  </div>
) : (
```

---

## 修复后交互时序

```
用户点击发送
  └→ 消息立即以蓝色气泡出现（乐观更新，无网络延迟感）
  └→ Agent 灰色气泡显示「⟳ 正在思考...」（立即出现）
        ↓（Agent 被唤醒 → LLM 调用开始）
  └→ SSE ui.agent.stream.start → 切换为流式文字逐字输出（▌光标）
  └→ SSE ui.agent.stream.done  → 完成，消息固定，刷新列表
```

---

## 涉及文件

| 文件 | 操作 | 改动要点 |
|------|------|----------|
| `ai-agent-foward/src/modules/swarm/hooks/useSwarmMessages.ts` | 修改 | `send` 方法增加乐观更新 + 错误回滚 |
| `ai-agent-foward/src/modules/swarm/pages/SwarmMainPage.tsx` | 修改 | `handleSend` 发送前立即设置 streamingContent/AgentId |
| `ai-agent-foward/src/modules/swarm/components/chat/SwarmMessageList.tsx` | 修改 | 空 streamingContent 时使用 `thinking` contentType |
| `ai-agent-foward/src/modules/swarm/components/chat/SwarmMessageBubble.tsx` | 修改 | 新增 `thinking` 类型气泡渲染（LoadingOutlined + 灰色文字） |

---

## 验证

- `npx tsc --noEmit` → 零错误
- Vite 热更新生效，刷新页面即可验证
