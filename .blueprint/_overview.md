## Metadata
- file: `.blueprint/_overview.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: _overview
- 该文件是蓝图系统的总览文档，描述整个 AI Agent 平台的架构全景、核心流程、模块边界和数据流向。作为蓝图索引入口，指导开发者快速定位相关蓝图文件，理解系统整体设计。

## 2) 核心方法
- `BlueprintFlow()`
- `ValidateStatus()`

## 3) 具体方法
### 3.1 BlueprintFlow()
- 函数签名: `BlueprintFlow(requirement: String) → List<BlueprintFile>` (概念性流程，非实际代码)
- 入参: `requirement` 需求描述（如"修改工作流执行逻辑"）
- 出参: `List<BlueprintFile>` 相关蓝图文件路径列表（如 [WorkflowEngine.md, NodeExecutors.md, ExecutionContext.md]）
- 功能含义: 根据需求定位相关蓝图文件，遵循"蓝图优先协议"三步思考链（Step1: 蓝图索引 → Step2: 架构推演 → Step3: 代码投影）
- 链路作用: 开发者入口 → 读取 _overview.md → 定位子蓝图 → 分析职责契约 → 更新蓝图 → 修改代码

### 3.2 ValidateStatus()
- 函数签名: `ValidateStatus(blueprintFile: String) → StatusReport` (概念性流程，非实际代码)
- 入参: `blueprintFile` 蓝图文件路径
- 出参: `StatusReport` 状态报告（包含 status、inconsistencies、recommendations）
- 功能含义: 校验蓝图状态机合法性（正常/待修改/修改中/修改完成），检查蓝图与代码一致性，识别架构漂移
- 链路作用: 蓝图维护流程 → ValidateStatus() → 检查状态流转合法性 → 对比代码实现 → 生成差异报告


## 4) 变更记录
- 2026-02-23: 条件分支改造完成，新增 ConditionEvaluatorPort 端口、ConditionBranch/ConditionGroup/ConditionItem 值对象、StructuredConditionEvaluator 实现、旧模型兼容转换、剪枝逻辑修复。
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全概念性流程方法，描述蓝图索引和状态校验的契约级功能，作为文档型蓝图的指导性说明。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
