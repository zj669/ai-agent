# Blueprint Mirror: ai-agent-foward/postcss.config.js

## Source File
- Path: ai-agent-foward/postcss.config.js
- Type: .js

## Responsibility
- 定义 PostCSS 处理流水线，启用 Tailwind 与 Autoprefixer 以生成兼容样式。

## Key Symbols / Structure
- `plugins['@tailwindcss/postcss']`
- `plugins.autoprefixer`

## Dependencies
- postcss
- @tailwindcss/postcss
- autoprefixer

## Notes
- 服务于样式构建阶段，不直接参与业务页面逻辑。
