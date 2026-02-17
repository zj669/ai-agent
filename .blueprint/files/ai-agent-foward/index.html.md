# Blueprint Mirror: ai-agent-foward/index.html

## Source File
- Path: ai-agent-foward/index.html
- Type: .html

## Responsibility
- 定义前端 HTML 宿主页结构，声明 `#root` 挂载点并加载入口脚本与基础页面元信息。

## Key Symbols / Structure
- `<div id="root"></div>`
- `<script type="module" src="/src/index.tsx"></script>`
- 基础 `meta/title/importmap` 配置

## Dependencies
- Vite HTML 入口约定
- 浏览器原生 HTML/CSS/ESM

## Notes
- 负责应用页面壳层，不承载业务逻辑。
