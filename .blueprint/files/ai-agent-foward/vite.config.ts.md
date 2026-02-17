# Blueprint Mirror: ai-agent-foward/vite.config.ts

## Source File
- Path: ai-agent-foward/vite.config.ts
- Type: .ts

## Responsibility
- 配置 Vite 开发与构建行为：本地代理、插件、环境变量注入、别名解析和产物分包策略。

## Key Symbols / Structure
- `defineConfig(({ mode }) => ...)`
- `server.proxy`：`/api` 与 `/client` 转发
- `plugins: [react()]`
- `build.rollupOptions.manualChunks`

## Dependencies
- vite
- @vitejs/plugin-react
- node `path`

## Notes
- 作为前端工程化入口配置，决定 dev/prod 构建行为。
- 开发代理目标从 `VITE_API_BASE_URL` 读取，默认 `http://localhost:8080`。
- 开发服务地址与端口从环境变量读取：`VITE_DEV_HOST`、`VITE_DEV_PORT`、`VITE_DEV_STRICT_PORT`，避免代码中写死。
