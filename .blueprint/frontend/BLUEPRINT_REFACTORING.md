## Metadata
- file: `.blueprint/frontend/BLUEPRINT_REFACTORING.md`
- version: `1.1`
- status: 修改完成
- updated_at: 2026-02-16
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: BLUEPRINT_REFACTORING
- 该文件用于描述 BLUEPRINT_REFACTORING 的职责边界与协作关系。

## 2) 核心方法
- `recordRefactorDecision(item)`
- `summarizeStageResult(stage)`
- `archiveLegacySpec(specPath, replacement)`

## 3) 具体方法
### 3.1 recordRefactorDecision(item)
- 函数签名: `recordRefactorDecision(item: RefactorDecisionItem): void`
- 入参: `item` 包含 `timestamp`, `scope`, `decision`, `rationale`, `affectedFiles`
- 出参: 无返回值，追加记录到"决策日志"
- 功能含义: 记录蓝图重构过程中的关键决策点。
- 链路作用: 元文档写入接口，确保架构演进可追溯。

**决策日志（MVP收敛）**
- 时间: 2026-02-16
- 范围: 前端 workflow 编辑器蓝图
- 决策: 将所有 workflow 蓝图收敛为 MVP 目标，仅保留：拖拉拽编辑、卡片配置、graphJson 对接、metadata 驱动
- 理由: 产品策略调整为快速落地最小可用编辑器，非核心交互（快捷键/undo-redo/协作）延后
- 影响文件:
  - `WorkflowEditorPage.md` - 移除冲突处理外的复杂交互
  - `useWorkflowEditor.md` - 移除历史管理能力
  - `NodePanel.md` - 移除快捷键、批量操作
  - `WorkflowNodeLarge.md` - 移除执行状态、配置预览
  - `workflowService.md` - 移除执行态方法
  - 新增分层结构蓝图: `TargetStructure.md`, `LayeredModules.md`, `HooksAndStoreSlices.md`

### 3.2 summarizeStageResult(stage)
- 函数签名: `summarizeStageResult(stage: RefactorStage): StageSummary`（概念性方法，非实际代码）
- 入参: `stage` 为重构阶段标识（如 "workflow-editor-removal", "sse-integration"）
- 出参: 返回阶段总结对象，包含 `completedTasks`, `deletedFiles`, `modifiedBlueprints`, `remainingIssues`
- 功能含义: 汇总某个重构阶段的成果和遗留问题，生成结构化报告。用于向团队汇报进度和风险。
- 链路作用: 元文档的读取接口，支持阶段性回顾和里程碑验证。

### 3.3 archiveLegacySpec(specPath, replacement)
- 函数签名: `archiveLegacySpec(specPath: string, replacement: string | null): void`（概念性方法，非实际代码）
- 入参:
  - `specPath`: 被废弃的蓝图文件路径
  - `replacement`: 可选的替代方案说明（如 "功能已迁移至 ChatPage"）
- 出参: 无返回值，更新本文档的"归档清单"章节
- 功能含义: 标记已废弃的蓝图文档，说明废弃原因和替代方案。保留历史记录但明确其不再维护。
- 链路作用: 元文档的归档管理，防止团队误用过时规范。


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构。
- 2026-02-16: 按 MVP 目标记录重构决策，收敛 workflow 编辑器蓝图范围，移除非必要交互能力。

## 5) Temp缓存区
- 本次任务流转: `待修改 -> 修改中 -> 修改完成`
- 当前状态: `修改完成`
