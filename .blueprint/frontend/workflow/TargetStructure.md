## Metadata
- file: `.blueprint/frontend/workflow/TargetStructure.md`
- version: `1.1`
- status: 修改完成
- updated_at: 2026-02-16
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

## 3) 具体方法
### 3.0 命名基线
- 本文件是 workflow 领域命名基线，统一使用：`editor-canvas`、`node-config`、`workflow-integration`。
- 其他 workflow 蓝图若出现别名，必须通过映射回上述三个标准名。

### 3.1 defineWorkflowSkeleton()
- 函数签名: `defineWorkflowSkeleton(): WorkflowStructure`
- 入参: 无（读取目标架构配置）
- 出参: `WorkflowStructure` 对象，包含 `{ directories: string[], modules: { [name: string]: { path: string, exports: string[] } }, entryPoints: string[] }`
- 功能含义: 定义 MVP 目标目录结构，仅保留三类模块：
  - `editor-canvas`（拖拉拽与连线）
  - `node-config`（卡片配置面板）
  - `workflow-integration`（graphJson + metadata 对接）
- 链路作用: 架构初始化器，为迁移提供目标结构蓝图，确保新代码按统一规范组织

### 3.2 validateModuleBoundary(moduleName: string, deps: string[])
- 函数签名: `validateModuleBoundary(moduleName: string, deps: string[]): BoundaryValidation`
- 入参: `moduleName` - 模块名称（如 `'editor-canvas'`, `'node-config'`），`deps` - 该模块的依赖列表
- 出参: `BoundaryValidation` 包含 `{ valid: boolean, violations: Array<{ dep: string, reason: string }>, allowedDeps: string[] }`
- 功能含义: 验证模块依赖是否符合 MVP 边界规则：
  - `editor-canvas` 仅依赖 `workflow-integration` 暴露的数据模型；
  - `node-config` 仅依赖 metadata schema 与选中节点状态；
  - 页面层不得绕过模块直接访问底层请求。
- 链路作用: 模块边界守护者，在代码审查或构建时检测违反架构约束的导入关系

### 3.3 mapPageToWorkflowContainer(pageFile: string)
- 函数签名: `mapPageToWorkflowContainer(pageFile: string): ContainerMapping`
- 入参: `pageFile` - 页面文件路径（如 `'src/pages/WorkflowEditorPage.tsx'`）
- 出参: `ContainerMapping` 对象，包含 `{ containerPath: string, requiredHooks: string[], requiredStores: string[] }`
- 功能含义: 将页面映射到 MVP 容器组合，至少包含：
  - 画布容器（拖拉拽、连线、节点落点）；
  - 配置容器（metadata 驱动表单）；
  - 对接容器（graphJson 读写与提交）。
- 链路作用: 页面重构指南生成器，帮助开发者将旧页面迁移到新的容器化架构


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全目标结构定义方法的具体签名，明确工作流骨架生成、模块边界验证和页面容器映射契约。
- 2026-02-16: 按 MVP 目标收敛目标结构，仅保留拖拽画布、卡片配置、graphJson 与 metadata 对接。
- 2026-02-16: 修复必修问题：声明本文件为 workflow 命名基线，要求其他蓝图统一术语。

## 5) Temp缓存区
- 本次任务流转: `待修改 -> 修改中 -> 修改完成`
- 当前状态: `修改完成`
