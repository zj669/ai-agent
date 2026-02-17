## Metadata
- file: `.blueprint/frontend/workflow/LayeredModules.md`
- version: `1.1`
- status: 修改完成
- updated_at: 2026-02-16
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: LayeredModules
- 该文件用于描述 LayeredModules 的职责边界与协作关系。

## 2) 核心方法
- `defineLayerContract(layer)`
- `validateDependencyDirection(from, to)`
- `exposeModuleEntry(layer)`

## 3) 具体方法
### 3.1 defineLayerContract(layer: string)
- 函数签名: `defineLayerContract(layer: string): LayerContract`
- 入参: `layer` - 层级名称（如 `'editor-canvas'`, `'node-config'`, `'workflow-integration'`）
- 出参: `LayerContract` 对象，包含 `{ exports: string[], allowedImports: string[], forbiddenImports: string[], responsibilities: string[] }`
- 功能含义: 定义 MVP 工作流编辑器的分层契约，命名与 `TargetStructure` 保持完全一致。
- 链路作用: 分层架构规范定义器，用于生成架构文档和 ESLint 规则，确保层级边界清晰

### 3.2 validateDependencyDirection(from: string, to: string)
- 函数签名: `validateDependencyDirection(from: string, to: string): ValidationResult`
- 入参: `from` - 源模块路径，`to` - 目标模块路径
- 出参: `ValidationResult` 包含 `{ valid: boolean, violation: string | null, suggestion: string | null }`
- 功能含义: 检查依赖方向是否满足 MVP 约束：
  - `editor-canvas` 只能依赖 `workflow-integration` 暴露入口；
  - `node-config` 只能依赖 `workflow-integration` 暴露入口；
  - 页面层不允许直接调用后端 API，必须通过 `workflow-integration`。
- 链路作用: 依赖关系守护者，在构建时或 pre-commit 阶段检测违反分层原则的导入

### 3.3 exposeModuleEntry(layer: string)
- 函数签名: `exposeModuleEntry(layer: string): string`
- 入参: `layer` - 层级名称
- 出参: 该层级入口文件路径（如 `'src/workflow/editor-canvas/index.ts'`）
- 功能含义: 暴露 MVP 必需入口，限制跨层直接依赖内部实现
- 链路作用: 模块封装强化器，防止跨层级直接导入内部实现，确保接口稳定性

### 3.4 resolveNamingMapping()
- 函数签名: `resolveNamingMapping(): Record<string, string[]>`
- 入参: 无
- 出参: 命名映射对象
- 功能含义: 统一命名映射，保证文档术语一致：
  - `editor-canvas` -> `canvas-render`, `drag-drop`
  - `node-config` -> `properties-panel`, `config-form`
  - `workflow-integration` -> `graphjson-adapter`, `metadata-adapter`
- 链路作用: 术语标准化入口，避免实现阶段出现多套命名。


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全分层模块管理方法的具体签名，明确层级契约定义、依赖方向验证和模块入口暴露规则。
- 2026-02-16: 按 MVP 目标重定义 workflow 分层，聚焦拖拽交互、卡片配置、graphJson 与 metadata 对接。
- 2026-02-16: 明确排除非 MVP 交互能力相关层级职责。
- 2026-02-16: 修复必修问题：分层命名与 TargetStructure 对齐，新增术语映射防止多命名并存。

## 5) Temp缓存区
- 本次任务流转: `待修改 -> 修改中 -> 修改完成`
- 当前状态: `修改完成`
