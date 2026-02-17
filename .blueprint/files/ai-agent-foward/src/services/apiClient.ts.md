# Blueprint Mirror: ai-agent-foward/src/services/apiClient.ts

## Source File
- Path: ai-agent-foward/src/services/apiClient.ts
- Type: .ts

## Responsibility
- HTTP 客户端基础设施，负责统一 Axios 实例、拦截器与通用请求配置。

## Key Symbols / Structure
- apiClient (axios instance)

## Dependencies
- axios
- stores/authStore

## Notes
- 基础请求地址统一通过 `VITE_API_BASE_URL` 组装：有值时走 `<VITE_API_BASE_URL>/api`，无值时回退 `/api` 代理。
- Auto-created blueprint mirror template.
