## Metadata
- file: `.blueprint/frontend/workflow/LegacyMigrationMap.md`
- version: `1.1`
- status: 修改完成
- updated_at: 2026-02-16
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: LegacyMigrationMap
- 该文件用于描述 LegacyMigrationMap 的职责边界与协作关系。

## 2) 核心方法
- `resolveMigrationTarget(legacyPath)`
- `validateDeletePrerequisites(legacyPath)`
- `planDeletionOrder(legacyPaths)`
- `resolveMigrationTarget()`
- `validateDeletePrerequisites()`

## 3) 具体方法
### 3.1 resolveMigrationTarget(legacyPath: string)
- 函数签名: `resolveMigrationTarget(legacyPath: string): MigrationTarget | null`
- 入参: `legacyPath` - 旧代码文件路径（如 `src/components/workflow/index.tsx`）
- 出参: `MigrationTarget` 对象包含 `{ targetPath: string, targetModule: string, migrationStatus: 'pending' | 'in-progress' | 'completed' }` 或 `null`（无迁移目标）
- 功能含义: 根据旧文件路径查找 MVP 目标模块映射（`editor-canvas` / `node-config` / `workflow-integration`）。
- 链路作用: 在删除旧文件前确认功能已迁移至新模块，防止功能丢失

### 3.2 validateDeletePrerequisites(legacyPath: string)
- 函数签名: `validateDeletePrerequisites(legacyPath: string): ValidationResult`
- 入参: `legacyPath` - 待删除的旧文件路径
- 出参: `ValidationResult` 包含 `{ canDelete: boolean, blockers: string[], warnings: string[] }`
- 功能含义: 检查旧文件是否可安全删除，MVP 判定必须满足：
  - 拖拉拽新增与连线已在新模块可用；
  - 卡片配置可由 metadata 驱动渲染并写回；
  - graphJson 加载/保存链路已打通。
- 链路作用: 在执行删除操作前的安全检查点，确保不破坏现有功能

### 3.3 planDeletionOrder(legacyPaths: string[])
- 函数签名: `planDeletionOrder(legacyPaths: string[]): string[]`
- 入参: `legacyPaths` - 待删除文件路径数组
- 出参: 按依赖关系排序的删除顺序数组（叶子节点优先，根节点最后）
- 功能含义: 按 MVP 迁移优先级计算删除顺序：先删除非核心交互模块，再删除被新模块完全替代的流程文件。
- 链路作用: 批量清理旧代码时的执行计划生成器，确保删除过程不破坏构建


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全具体方法签名、参数类型、返回值结构和业务含义，移除模板占位文案，明确迁移映射契约。
- 2026-02-16: 按 MVP 目标收敛 legacy 迁移判定，仅围绕拖拽、卡片配置、graphJson 对接三条主链路。

## 5) Temp缓存区
- 本次任务流转: `待修改 -> 修改中 -> 修改完成`
- 当前状态: `修改完成`
