# 美化前端多智能体协作聊天页面 — PRD

## 1. 目标

将 Swarm 聊天界面提升至 Claude Code CLI 的视觉体验水准：
- Markdown 流式渲染（语法高亮、表格、GFM）
- 工具调用卡片融入对话流（不突兀）
- 全方位视觉升级（动画、配色、层次）

## 2. 现状分析

| 组件 | 当前问题 |
|------|---------|
| `SwarmMessageBubble` | 直接渲染纯文本，无 Markdown，无动画 |
| `SwarmMessageList` | 消息无滑入效果 |
| `ToolCallBadge` | emoji + Tag 形式，过于朴素 |
| 工具调用展开卡 | 独立 block，与对话流割裂 |
| `WaitingCard` | 风格与其他组件不统一 |
| 全局 | 无 markdown-body 样式 |

## 3. 功能需求

### 3.1 Markdown 渲染

- **流式输出**：streamingContent 实时追加渲染 Markdown，保留 `▌` 光标
- **静态消息**：完整 Markdown 渲染，支持 GFM（表格、任务列表）
- **代码高亮**：通过 `rehype-highlight` 实现代码块语法高亮
- **共享组件**：抽取 `MarkdownRenderer.tsx` 供两模块复用

### 3.2 工具调用卡片重设计

**核心思路：从"界面元素"变为"对话参与者"**

旧方案：独立 Block → 新方案：**Inline 对话气泡**

- 工具调用以 Compact 对话气泡展示在消息流中
- 头像使用工具专属图标（如齿轮、闪电）
- 展开详情以 Drawer 形式呈现（不遮挡消息流）
- 运行中状态：加载动画 + "正在调用 xxx" 提示
- 完成状态：绿色勾选 + "已完成 xxx"

### 3.3 视觉升级

- **消息入场动画**：CSS `@keyframes slideIn` 从下方滑入 + 淡入
- **头像升级**：Agent 圆形头像带渐变背景
- **气泡美化**：阴影层次、圆角规范、间距统一
- **Markdown 样式**：定义 `.ai-markdown-body` 全局样式
- **配色体系**：建立 `swarm-colors.ts` 常量文件，统一颜色引用
- **Streaming 光标**：保留 `▌` 闪烁效果

### 3.4 组件级改进

| 组件 | 改进点 |
|------|--------|
| `SwarmMessageBubble` | Markdown 渲染 + 动画 class |
| `SwarmMessageList` | 入场动画 wrapper |
| `ToolCallBadge` | Compact Chip 风格 |
| `WaitingCard` | 统一视觉语言 |
| `CollaborationPanel` | 细化卡片视觉 |
| `WorkerCard` | 动画优化 |

## 4. 技术方案

- **框架**：React 19 + TypeScript
- **Markdown**：`react-markdown` + `remark-gfm` + `rehype-highlight`（已存在于 `package.json`）
- **动画**：纯 CSS `@keyframes`，内联 `<style>` 标签
- **样式**：`index.css` 中添加 `.ai-markdown-body` 样式
- **复用**：新建 `MarkdownRenderer.tsx` 共享组件

## 5. 文件变更

| 文件 | 变更 |
|------|------|
| `index.css` | 添加 `.ai-markdown-body` 及 markdown 样式 |
| `modules/swarm/components/chat/MarkdownRenderer.tsx` | **新建** — 共享 Markdown 渲染器 |
| `modules/swarm/components/chat/SwarmMessageBubble.tsx` | Markdown 渲染 + 工具调用新样式 |
| `modules/swarm/components/chat/SwarmMessageList.tsx` | 入场动画 |
| `modules/swarm/components/chat/ToolCallBadge.tsx` | Compact Chip 风格 |
| `modules/swarm/components/chat/WaitingCard.tsx` | 视觉升级 |
| `modules/swarm/styles/swarm-colors.ts` | **新建** — 配色常量 |

## 6. 验收标准

- [ ] 流式输出实时渲染 Markdown（代码块高亮正常）
- [ ] 工具调用以对话气泡形式展示，不再是独立 Block
- [ ] 消息入场有滑入动画
- [ ] `npm run lint` 和 `npm run typecheck` 均通过
- [ ] 视觉风格与 Claude Code CLI 聊天体验接近
