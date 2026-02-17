# Blueprint Mirror: ai-agent-foward/tsconfig.json

## Source File
- Path: ai-agent-foward/tsconfig.json
- Type: .json

## Responsibility
- 定义前端 TypeScript 编译与类型检查策略，包括目标语法、模块解析、JSX 规范与路径别名。

## Key Symbols / Structure
- `compilerOptions.target/module/lib`
- `moduleResolution: bundler`
- `jsx: react-jsx`
- `paths: { "@/*": ["./src/*"] }`

## Dependencies
- TypeScript 编译器
- Vite bundler 解析策略

## Notes
- 控制类型系统行为，不参与运行时业务逻辑。
