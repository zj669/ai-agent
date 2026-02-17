# Blueprint Mirror: ai-agent-foward/src/pages/DashboardPage.tsx

## Source File
- Path: ai-agent-foward/src/pages/DashboardPage.tsx
- Type: .tsx

## Responsibility
- 仪表盘页面，负责聚合展示系统统计、趋势与关键运营指标。

## Key Symbols / Structure
- DashboardPage (React component)

## Dependencies
- react
- hooks/useDashboard
- antd

## Notes
- 「创建 Agent」入口采用快速创建流：点击后先创建默认 Agent，再直接进入 `/agents/:id/workflow` 拖拽编辑页。
- 页面内两个创建入口（快速操作、快速开始）共享同一创建并跳转逻辑，保证行为一致。
