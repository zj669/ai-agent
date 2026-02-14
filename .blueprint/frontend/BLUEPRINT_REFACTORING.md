## Metadata
- file: `.blueprint/frontend/BLUEPRINT_REFACTORING.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
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
- 函数签名: `recordRefactorDecision(item: RefactorDecisionItem): void`（概念性方法，非实际代码）
- 入参: `item` 包含 `timestamp`, `scope`, `decision`, `rationale`, `affectedFiles`
- 出参: 无返回值，追加记录到本文档的"决策日志"章节
- 功能含义: 记录蓝图重构过程中的关键决策点，包括为何删除某模块、为何合并某功能、为何调整某契约。用于后续审计和知识传承。
- 链路作用: 元文档的写入接口，确保架构演进的可追溯性。

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
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全"具体方法"细节，基于元文档职责提供重构决策记录、阶段总结、遗留规范归档的完整签名与语义。说明其在迁移链路中的定位：本文档为蓝图重构过程的元数据记录，非实现代码，用于追踪架构演进历史。
- 2026-02-14: 移除重复方法占位条目，保留唯一契约定义。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
