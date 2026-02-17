## Metadata
- file: `.blueprint/frontend/workflow/MigrationCompletionMatrix.md`
- version: `1.1`
- status: 修改完成
- updated_at: 2026-02-16
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: MigrationCompletionMatrix
- 该文件用于描述 MigrationCompletionMatrix 的职责边界与协作关系。

## 2) 核心方法
- `evaluateModuleProgress(module)`
- `listNextActions(module)`
- `resolveAcceptanceScope()`

## 3) 具体方法
### 3.1 evaluateModuleProgress(module: string)
- 函数签名: `evaluateModuleProgress(module: string): ModuleProgress`
- 入参: `module` - 模块名称（如 `'editor-canvas'`, `'node-config'`, `'workflow-integration'`）
- 出参: `ModuleProgress` 对象，包含 `{ completionRate: number, migratedFiles: string[], pendingFiles: string[], blockers: string[] }`
- 功能含义: 评估 MVP 三模块落地进度，重点检查拖拉拽交互、卡片配置、graphJson 对接是否已贯通
- 链路作用: 迁移进度仪表盘数据源，用于生成迁移报告和决策下一步行动

### 3.2 listNextActions(module: string)
- 函数签名: `listNextActions(module: string): Action[]`
- 入参: `module` - 模块名称
- 出参: `Action[]` 数组，每个元素包含 `{ type: 'migrate' | 'test' | 'delete', target: string, priority: number, estimatedEffort: string }`
- 功能含义: 生成 MVP 落地清单，优先级规则：
  1) 先通拖拉拽新增与连线；
  2) 再通 metadata 驱动卡片配置；
  3) 最后验收 graphJson 保存与回填一致性。
- 链路作用: 迁移任务规划器，为开发者提供可执行的任务列表，确保迁移有序推进

### 3.3 resolveAcceptanceScope()
- 函数签名: `resolveAcceptanceScope(): AcceptanceScope`
- 入参: 无（读取全局迁移配置）
- 出参: `AcceptanceScope` 对象，包含 `{ requiredModules: string[], optionalModules: string[], acceptanceCriteria: { [module: string]: string[] } }`
- 功能含义: 定义 MVP 验收范围：
  - required: `editor-canvas`, `node-config`, `workflow-integration`
  - optional: 空
  - 命名来源: `TargetStructure.defineWorkflowSkeleton`，不允许使用别名
  - acceptanceCriteria 最小标准：
    - 能拖拽节点并连线；
    - 能按 metadata 渲染卡片配置并写回节点；
    - 能稳定执行 graphJson 加载与保存。
- 链路作用: 迁移完成度判定器，用于决策是否可以关闭迁移阶段并删除旧代码


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全迁移完成度矩阵方法的具体签名，明确进度评估、行动规划和验收范围定义契约。
- 2026-02-16: 按 MVP 目标重写迁移与验收标准，去除非 MVP 能力要求。

## 5) Temp缓存区
- 本次任务流转: `待修改 -> 修改中 -> 修改完成`
- 当前状态: `修改完成`
