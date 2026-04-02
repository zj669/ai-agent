# 美化 Workflow Agent 聊天页面

## Goal

将 Workflow 执行聊天页面（`ChatPage.tsx`）从简陋样式升级至 Swarm 多智能体聊天的视觉水准，覆盖四个维度：流式输出动画、节点执行状态框、Markdown 渲染、审核点卡片。

## Background — UI 现状差距

| 维度 | Swarm (参考) | Workflow (现状) |
|------|-------------|----------------|
| 消息气泡 | 渐变 Avatar、角色名气泡上方、精心阴影、圆角气泡 | 纯色 Avatar、简单背景、无角色名 |
| 流式动画 | `swarmSlideIn` 入场 + `swarm-cursor` 闪烁光标 | 老式 typing dots（三个跳动圆点） |
| 节点执行状态 | 渐变思考气泡 + LoadingOutlined + 标题 | 简陋折叠块 + emoji 图标 |
| Markdown 渲染 | `react-markdown` + `rehype-highlight` + 深色代码块 | `.markdown-body` 类但样式不完整 |
| 审核卡片 | Alert 样式内联 | 已较完整，可统一颜色系统 |

## Requirements

### 1. 消息气泡美容（ChatPage.tsx）
- 引入 `SWARM_COLORS` 颜色系统或创建 `chat-colors.ts`
- Agent 消息气泡增加阴影（`boxShadow`）和细边框
- Avatar 改为圆形渐变（参考 `SwarmMessageBubble.tsx` 第 92-108 行）
- 气泡上方显示 Agent 角色名
- 用户消息保持 Ant Design 蓝色风格

### 2. 流式输出动画
- 移除老式 typing dots（`typingBounce` CSS）
- 引入 `swarmSlideIn` 入场动画（`index.css`）
- 引入 `swarm-cursor` 闪烁光标（inline `<span>` 替代 dots）
- 动画作用于每条新消息的入场

### 3. 节点执行状态框（ThinkingStepsBlock）
- 引入渐变背景色（参考 `SWARM_COLORS.thinkingBubbleBg`）
- 思考步骤项增加彩色左侧边框（按类型配色）
- LoadingOutlined 替代 emoji 图标
- "正在思考..." 等标题样式与 Swarm 保持一致

### 4. Markdown 渲染
- 引入 `MarkdownRenderer.tsx` 组件（从 swarm 复用）
- 引入 `.ai-markdown-body` 完整样式（`index.css`）
- 统一代码块样式为 GitHub Dark 主题
- 支持流式闪烁光标（参考 `MarkdownRenderer.tsx` 流式模式）

### 5. 审核点卡片（HumanReviewModal / 审核块）
- 统一颜色系统与 Swarm 保持一致
- 审核卡片增加顶部彩色条（参考 `CollaborationPanel.tsx`）
- 状态 Tag 样式与 Swarm `SWARM_COLORS` 对齐

### 6. 可选增强（根据时间）
- 右下角 Toast 通知（参考 `SwarmNotification.tsx`）
- 协作者/Worker 状态卡片（参考 `WorkerCard.tsx`）

## Technical Notes

### 核心文件
- **修改**: `ai-agent-foward/src/modules/chat/pages/ChatPage.tsx`（主战场）
- **复用**: `ai-agent-foward/src/modules/swarm/styles/swarm-colors.ts`
- **复用**: `ai-agent-foward/src/modules/swarm/components/chat/MarkdownRenderer.tsx`
- **修改**: `ai-agent-foward/src/index.css`（新增动画和样式）
- **新建**: `ai-agent-foward/src/modules/chat/components/`（新组件）

### 颜色系统
```ts
// 复用或扩展 swarm-colors.ts
SWARM_COLORS = {
  agentBubbleBg, agentBubbleBorder,
  humanBubbleBg, humanShadow, agentShadow,
  thinkingBubbleBg, thinkingBorder,
  toolCardBg, toolCardBorder,
  waitingBg,
}
```

## Acceptance Criteria

- [ ] 消息气泡有阴影、边框、Agent 角色名显示
- [ ] 流式输出使用闪烁光标而非 typing dots
- [ ] 新消息有 slide-in 入场动画
- [ ] 节点执行状态有渐变背景和 LoadingOutlined
- [ ] Markdown 渲染样式完整（代码块有语法高亮）
- [ ] 审核卡片与整体颜色系统一致
- [ ] `npm run build` 通过，无 lint 错误

## Out of Scope
- 后端逻辑变更
- 工作流画布（Flow Editor）UI 变更
- Swarm 聊天页面改动（已单独维护）
