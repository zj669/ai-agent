# 美化 Swarm 协作看板

## Goal
优化 `CollaborationPanel` 组件：移除无用的草案展示区，采用更现代的布局和间距美化 agent 协作卡片。

## Requirements
- [x] 删除顶部的「当前草稿」Card 区域（`latestDraft` 相关代码）
- [x] 美化 `WritingCollaborationCard` agent 卡片：更现代的布局、更好的间距和排版
- [x] 保持卡片交互功能（点击打开 Drawer 查看详情）
- [x] 确保组件 Props 接口移除 `latestDraft` 相关字段

## Acceptance Criteria
- [x] 页面顶部不再显示草稿概览卡片
- [x] Agent 卡片列表采用更现代的视觉设计（间距、圆角、阴影、层次）
- [x] 代码中无 `latestDraft` 相关逻辑残留
- [x] 相关调用方（如 `SwarmMainPage`）同步更新

## Technical Notes
- 文件：`ai-agent-foward/src/modules/swarm/components/panel/CollaborationPanel.tsx`
- 样式：优先使用 Tailwind CSS 工具类，替代内联样式
- 测试：确认 `CollaborationPanel` 渲染正常，无 TypeScript 错误
