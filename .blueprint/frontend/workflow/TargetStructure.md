## Metadata
- file: `.blueprint/frontend/workflow/TargetStructure.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: TargetStructure
- 该文件用于描述 TargetStructure 的职责边界与协作关系。

## 2) 核心方法
- `defineWorkflowSkeleton()`
- `validateModuleBoundary(moduleName, deps)`
- `mapPageToWorkflowContainer(pageFile)`
- `validateModuleBoundary()`
- `mapPageToWorkflowContainer()`

## 3) 具体方法
### 3.1 defineWorkflowSkeleton()
- 函数签名: `defineWorkflowSkeleton(): WorkflowStructure`
- 入参: 无（读取目标架构配置）
- 出参: `WorkflowStructure` 对象，包含 `{ directories: string[], modules: { [name: string]: { path: string, exports: string[] } }, entryPoints: string[] }`
- 功能含义: 定义工作流模块的目标目录结构和模块划分，生成新架构的骨架定义
- 链路作用: 架构初始化器，为迁移提供目标结构蓝图，确保新代码按统一规范组织

### 3.2 validateModuleBoundary(moduleName: string, deps: string[])
- 函数签名: `validateModuleBoundary(moduleName: string, deps: string[]): BoundaryValidation`
- 入参: `moduleName` - 模块名称（如 `'nodes'`, `'hooks'`），`deps` - 该模块的依赖列表
- 出参: `BoundaryValidation` 包含 `{ valid: boolean, violations: Array<{ dep: string, reason: string }>, allowedDeps: string[] }`
- 功能含义: 验证模块依赖是否符合目标架构的边界规则，检测非法跨层依赖
- 链路作用: 模块边界守护者，在代码审查或构建时检测违反架构约束的导入关系

### 3.3 mapPageToWorkflowContainer(pageFile: string)
- 函数签名: `mapPageToWorkflowContainer(pageFile: string): ContainerMapping`
- 入参: `pageFile` - 页面文件路径（如 `'src/pages/WorkflowEditorPage.tsx'`）
- 出参: `ContainerMapping` 对象，包含 `{ containerPath: string, requiredHooks: string[], requiredStores: string[] }`
- 功能含义: 将页面级组件映射到对应的工作流容器组件，明确页面需要的 Hook 和 Store 依赖
- 链路作用: 页面重构指南生成器，帮助开发者将旧页面迁移到新的容器化架构


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全目标结构定义方法的具体签名，明确工作流骨架生成、模块边界验证和页面容器映射契约。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
