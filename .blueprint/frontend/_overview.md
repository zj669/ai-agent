# 前端蓝图总览（Workflow Dify 模式）

## 职责契约
- **做什么**: 定义 `ai-agent-foward` 工作流前端重构的目标结构、职责边界、迁移顺序。
- **不做什么**: 不描述具体组件实现细节，不替代模块级蓝图。

## 对齐来源
- 规范来源：`E:\WorkSpace\repo\ai-agent\ai-agent-foward\docs\agent.md`
- 对齐基线：Dify workflow 目录组织（index/constants/types/style/custom-edge/custom-connection-line + hooks/store/nodes/panel/operator/features/utils）

## 唯一生效蓝图集合（当前有效）
- `E:\WorkSpace\repo\ai-agent\.blueprint\frontend\workflow\TargetStructure.md`
- `E:\WorkSpace\repo\ai-agent\.blueprint\frontend\workflow\HooksAndStoreSlices.md`
- `E:\WorkSpace\repo\ai-agent\.blueprint\frontend\workflow\LayeredModules.md`
- `E:\WorkSpace\repo\ai-agent\.blueprint\frontend\workflow\LegacyMigrationMap.md`
- `E:\WorkSpace\repo\ai-agent\.blueprint\frontend\workflow\MigrationCompletionMatrix.md`
- `E:\WorkSpace\repo\ai-agent\.blueprint\frontend\pages\WorkflowEditorPage.md`

## workflow 目录总原则
1. `WorkflowEditorPage` 仅做装配，不承载编排逻辑。
2. 核心交互逻辑全部下沉到 hooks。
3. 编辑态统一收敛到 store slices。
4. 节点体系按类型目录化，不再按尺寸分叉。
5. 草稿同步、运行态、历史系统必须互相解耦。

## 依赖边界
| 层级 | 可以依赖 | 禁止依赖 |
|---|---|---|
| pages | workflow/index、services 装配接口 | 直接改写 nodes/edges 细节 |
| workflow hooks | workflow store、utils、services | pages 路由逻辑 |
| workflow store | types、纯函数 utils | HTTP/SSE、DOM 操作 |
| workflow modules | hooks 暴露接口、types/constants | 跨层反向调用 |

## 执行顺序（Blueprint → Serena → Coding）
1. 在 `workflow/*.md` 固化目录、hooks、slices、职责边界。
2. 用 Serena 校验现有实现与蓝图差异，输出迁移影响面。
3. 代码实施按顺序推进：store → hooks → modules → page 装配。

## 交付验收点
- 覆盖 `agent.md` 第 5 章目录规范。
- 覆盖第 6~8 章（架构、状态、交互）并可验证。
- 覆盖第 9~11 章（视觉、节点、数据契约）并可落地。
- 覆盖第 12~15 章（阶段、门禁、简报模板、偏差管理）。

## 遗留蓝图处理策略（处置结论）
| 蓝图路径 | 处置状态 | 用途 |
|---|---|---|
| `components/WorkflowNodeLarge.md` | 历史保留（非生效） | 历史设计回溯 |
| `components/NodePanel.md` | 历史保留（非生效） | 历史设计回溯 |
| `hooks/useWorkflowEditor.md` | 废弃（历史保留） | 单体 Hook 历史参考 |
| `services/workflowService.md` | 历史保留（非生效） | 服务契约参考 |
| `BLUEPRINT_REFACTORING.md` | 历史保留（非生效） | 重构过程记录 |

说明：后续新增或变更 workflow 相关能力，仅允许更新“唯一生效蓝图集合”中的文档。

## 变更摘要
- 将前端蓝图入口切换为 workflow 域索引，统一 Dify 模式结构。
- 强化 Page/Hook/Store/Module 四层边界，避免页面承载业务逻辑。
- 新增“唯一生效蓝图集合”与“遗留蓝图处置结论”表，解决新旧文档并存歧义。
- 新增迁移完成矩阵入口，迁移状态统一从矩阵读取。

## 变更日志
- [2026-02-13] 重写前端总览为 Dify 模式入口索引。
- [2026-02-13] 增加唯一生效蓝图集合与遗留蓝图处置结论。
