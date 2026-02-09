---
inclusion: always
---

# 影子架构工作流 (Shadow Architecture / Blueprint Workflow)

## 核心原则

**蓝图是唯一的真理源 (Source of Truth)，代码只是蓝图的投影。**

本项目维护一套 `.blueprint/` 目录，与 `src/` 结构镜像，包含每个核心模块的高密度架构摘要。
AI 在进行任何代码修改前，必须先查阅蓝图、更新蓝图，再投影到代码。

## .blueprint/ 目录结构

```
.blueprint/
├── _overview.md              # 系统全局依赖拓扑
├── domain/                   # 领域层蓝图
│   ├── workflow/
│   │   ├── WorkflowEngine.md
│   │   ├── NodeExecutor.md
│   │   └── HumanReview.md
│   ├── agent/
│   │   └── AgentService.md
│   ├── chat/
│   │   └── ChatService.md
│   ├── knowledge/
│   │   └── KnowledgeService.md
│   └── auth/
│       └── AuthService.md
├── application/              # 应用层蓝图
│   └── ...
├── infrastructure/           # 基础设施层蓝图
│   └── ...
└── interfaces/               # 接口层蓝图
    └── ...
```

## 蓝图文件标准格式

每个 `.blueprint/*.md` 文件必须包含以下章节：

```markdown
# [模块名] Blueprint

## 职责契约
- **做什么**: (用一两句话描述核心职责)
- **不做什么**: (明确划定边界，防止职责蔓延)

## 接口摘要
| 方法 | 输入 | 输出 | 副作用 | 约束 |
|------|------|------|--------|------|
| methodName | ParamType | ReturnType | 写DB/发事件/... | @Transactional等 |

## 依赖拓扑
- **上游** (谁调用我): XxxController, YyyService
- **下游** (我调用谁): ZzzRepository, AaaPort

## 领域事件
- 发布: EVENT_NAME — 触发条件
- 监听: EVENT_NAME — 处理逻辑

## 设计约束
- (架构决策记录，如：为什么选择这种方案)
- (性能约束、并发要求等)

## 变更日志
- [日期] 变更描述 (保持简洁，只记录架构级变更)
```

## 强制工作流：三步思考链

当收到任何涉及代码修改的需求时，AI 必须严格执行以下三步：

### Step 1: 蓝图索引 (Intent Mapping)

**严禁直接搜索或修改源代码。**

1. 先阅读 `.blueprint/_overview.md` 了解全局拓扑
2. 根据需求关键词，定位相关的 Blueprint 文件
3. 输出分析结论：
   - 本次修改涉及哪些模块？
   - 是否跨越了模块边界？
   - 是否需要新建模块？

### Step 2: 架构推演 (Architecture Evolution)

以"首席架构师"视角审视：

1. 检查修改是否违反目标模块的"职责契约"
2. 检查是否破坏依赖拓扑（禁止引入循环依赖）
3. 检查是否需要调整接口定义
4. **更新 `.blueprint/` 文件**，记录架构变更
5. 向用户展示蓝图变更摘要，等待确认

### Step 3: 代码投影 (Code Projection)

蓝图确认后，才开始修改代码：

1. 根据更新后的 Blueprint，精准定位需要修改的源文件
2. 代码实现必须严格遵循 Blueprint 中的接口定义和约束
3. 修改完成后，验证代码与蓝图的一致性

## 例外情况

以下场景可以跳过完整的三步流程：

- 纯 Bug 修复（不涉及接口变更或新增依赖）：可简化为 Step1 快速定位 + Step3 修复
- 纯格式/注释调整：直接修改
- 用户明确要求跳过蓝图流程

但即使是 Bug 修复，如果发现需要引入新依赖或改变模块职责，必须回到完整流程。

## 蓝图维护规则

1. **蓝图与代码必须同步**：修改代码时必须同步更新对应的 Blueprint
2. **蓝图优先级高于代码**：当蓝图与代码不一致时，以蓝图为准，代码需要被修正
3. **新模块必须先建蓝图**：创建新的 Service/Repository 前，先在 `.blueprint/` 创建对应文档
4. **蓝图要保持精简**：每个文件控制在 50-100 行以内，只包含 AI 决策所需的元数据
