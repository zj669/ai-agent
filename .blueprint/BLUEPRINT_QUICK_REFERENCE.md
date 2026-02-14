## Metadata
- file: `.blueprint/BLUEPRINT_QUICK_REFERENCE.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: BLUEPRINT_QUICK_REFERENCE
- 该文件是蓝图系统的快速参考手册，提供蓝图定位方法、使用流程、状态机规则和最佳实践。作为开发者速查文档，帮助快速理解蓝图优先协议和 Blueprint-Lite 规范。

## 2) 核心方法
- `locateBlueprint()`
- `runBlueprintFirstFlow()`

## 3) 具体方法
### 3.1 locateBlueprint()
- 函数签名: `locateBlueprint(module: String, layer: String) → String` (概念性流程，非实际代码)
- 入参: `module` 模块名（如 workflow/agent/knowledge），`layer` 分层名（如 domain/application/infrastructure）
- 出参: `String` 蓝图文件路径（如 `.blueprint/domain/workflow/WorkflowEngine.md`）
- 功能含义: 根据模块和分层快速定位蓝图文件，遵循目录结构约定（domain/application/infrastructure/interfaces/frontend）
- 链路作用: 开发者查询 → locateBlueprint(workflow, domain) → 返回 WorkflowEngine.md 路径 → 读取蓝图内容

### 3.2 runBlueprintFirstFlow()
- 函数签名: `runBlueprintFirstFlow(task: String) → WorkflowSteps` (概念性流程，非实际代码)
- 入参: `task` 开发任务描述（如"添加新节点类型"）
- 出参: `WorkflowSteps` 工作流步骤列表（Step1: 蓝图索引 → Step2: 架构推演 → Step3: 代码投影）
- 功能含义: 执行蓝图优先协议三步流程，确保架构变更先更新蓝图再修改代码，维护蓝图与代码一致性
- 链路作用: 开发任务启动 → runBlueprintFirstFlow() → Step1: 读取 _overview.md 定位相关蓝图 → Step2: 更新蓝图并展示变更摘要 → Step3: 确认后修改代码


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全概念性流程方法，描述蓝图定位和蓝图优先流程的契约级功能，作为快速参考文档的指导性说明。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
