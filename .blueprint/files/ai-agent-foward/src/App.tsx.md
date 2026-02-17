# Blueprint Mirror: ai-agent-foward/src/App.tsx

## Source File
- Path: ai-agent-foward/src/App.tsx
- Type: .tsx

## Responsibility
- 作为前端应用入口，初始化认证状态并组织公共与受保护路由，挂载整体页面框架。

## Key Symbols / Structure
- `App()`：调用认证初始化并声明全量路由。
- 路由分层：公开页（登录/重置密码）与受保护业务页。
- `MainLayout` + `ProtectedRoute`：统一业务页面壳层与鉴权入口。

## Dependencies
- react-router-dom (`Routes`, `Route`, `Navigate`)
- antd `ConfigProvider`
- `useAuthStore`、`ProtectedRoute`、`MainLayout` 与各页面组件

## Notes
- Auto-created blueprint mirror template.
