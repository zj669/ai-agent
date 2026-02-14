## Metadata
- file: `.blueprint/frontend/workflow/LayeredModules.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
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
- `defineLayerContract()`
- `validateDependencyDirection()`

## 3) 具体方法
### 3.1 defineLayerContract(layer: string)
- 函数签名: `defineLayerContract(layer: string): LayerContract`
- 入参: `layer` - 层级名称（如 `'presentation'`, `'business'`, `'data'`）
- 出参: `LayerContract` 对象，包含 `{ exports: string[], allowedImports: string[], forbiddenImports: string[], responsibilities: string[] }`
- 功能含义: 定义指定层级的契约规范，明确该层可导出的接口、允许/禁止的依赖和职责范围
- 链路作用: 分层架构规范定义器，用于生成架构文档和 ESLint 规则，确保层级边界清晰

### 3.2 validateDependencyDirection(from: string, to: string)
- 函数签名: `validateDependencyDirection(from: string, to: string): ValidationResult`
- 入参: `from` - 源模块路径，`to` - 目标模块路径
- 出参: `ValidationResult` 包含 `{ valid: boolean, violation: string | null, suggestion: string | null }`
- 功能含义: 检查模块间依赖方向是否符合分层规则（如 presentation 不能依赖 data 层）
- 链路作用: 依赖关系守护者，在构建时或 pre-commit 阶段检测违反分层原则的导入

### 3.3 exposeModuleEntry(layer: string)
- 函数签名: `exposeModuleEntry(layer: string): string`
- 入参: `layer` - 层级名称
- 出参: 该层级的入口文件路径（如 `'src/workflow/presentation/index.ts'`）
- 功能含义: 返回指定层级的统一导出入口，强制外部通过 barrel export 访问该层
- 链路作用: 模块封装强化器，防止跨层级直接导入内部实现，确保接口稳定性


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全分层模块管理方法的具体签名，明确层级契约定义、依赖方向验证和模块入口暴露规则。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
