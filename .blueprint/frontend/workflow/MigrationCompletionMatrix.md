## Metadata
- file: `.blueprint/frontend/workflow/MigrationCompletionMatrix.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
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
- `evaluateModuleProgress()`
- `listNextActions()`

## 3) 具体方法
### 3.1 evaluateModuleProgress(module: string)
- 函数签名: `evaluateModuleProgress(module: string): ModuleProgress`
- 入参: `module` - 模块名称（如 `'nodes'`, `'hooks'`, `'store'`）
- 出参: `ModuleProgress` 对象，包含 `{ completionRate: number, migratedFiles: string[], pendingFiles: string[], blockers: string[] }`
- 功能含义: 评估指定模块的迁移完成度，统计已迁移/待迁移文件数量和阻塞项
- 链路作用: 迁移进度仪表盘数据源，用于生成迁移报告和决策下一步行动

### 3.2 listNextActions(module: string)
- 函数签名: `listNextActions(module: string): Action[]`
- 入参: `module` - 模块名称
- 出参: `Action[]` 数组，每个元素包含 `{ type: 'migrate' | 'test' | 'delete', target: string, priority: number, estimatedEffort: string }`
- 功能含义: 根据模块当前状态生成优先级排序的下一步行动清单
- 链路作用: 迁移任务规划器，为开发者提供可执行的任务列表，确保迁移有序推进

### 3.3 resolveAcceptanceScope()
- 函数签名: `resolveAcceptanceScope(): AcceptanceScope`
- 入参: 无（读取全局迁移配置）
- 出参: `AcceptanceScope` 对象，包含 `{ requiredModules: string[], optionalModules: string[], acceptanceCriteria: { [module: string]: string[] } }`
- 功能含义: 定义迁移验收范围，明确哪些模块必须完成、哪些可选，以及每个模块的验收标准
- 链路作用: 迁移完成度判定器，用于决策是否可以关闭迁移阶段并删除旧代码


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全迁移完成度矩阵方法的具体签名，明确进度评估、行动规划和验收范围定义契约。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
