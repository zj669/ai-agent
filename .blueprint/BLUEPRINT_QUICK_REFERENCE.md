# 蓝图快速参考指南

## 🎯 核心原则

**蓝图是唯一的真理源 (Source of Truth)，代码只是蓝图的投影。**

## 📋 三步思考链

### Step 1: 蓝图索引
1. 读取 `.blueprint/_overview.md` (后端) 或 `.blueprint/frontend/_overview.md` (前端)
2. 定位相关的蓝图文件
3. 分析涉及的模块和边界

### Step 2: 架构推演
1. 检查职责契约
2. 检查依赖拓扑
3. 更新蓝图文件
4. 展示变更摘要

### Step 3: 代码投影
1. 根据蓝图定位源文件
2. 严格遵循蓝图实现
3. 验证代码与蓝图一致性

## 🗂️ 蓝图目录结构

### 后端蓝图
```
.blueprint/
├── _overview.md              # 系统全局依赖拓扑
├── domain/                   # 领域层
│   ├── workflow/
│   │   ├── WorkflowEngine.md
│   │   ├── NodeExecutor.md
│   │   └── HumanReview.md
│   ├── agent/AgentService.md
│   ├── chat/ChatService.md
│   └── ...
├── application/              # 应用层
│   ├── SchedulerService.md
│   └── ...
├── infrastructure/           # 基础设施层
│   ├── adapters/
│   └── persistence/
└── interfaces/               # 接口层
    └── Controllers.md
```

### 前端蓝图
```
.blueprint/frontend/
├── _overview.md              # 前端架构总览
├── pages/                    # 页面级蓝图
│   ├── WorkflowEditorPage.md
│   └── ...
├── components/               # 组件级蓝图
│   ├── WorkflowNodeLarge.md
│   └── ...
├── hooks/                    # Hook 级蓝图
│   ├── useWorkflowEditor.md
│   └── ...
├── services/                 # 服务级蓝图
│   ├── workflowService.md
│   └── ...
├── stores/                   # 状态级蓝图
│   ├── authStore.md
│   └── ...
└── types/                    # 类型级蓝图
    └── workflow.md
```

## 📝 蓝图标准格式

### 后端蓝图
```markdown
# [模块名] Blueprint

## 职责契约
- **做什么**: 核心职责
- **不做什么**: 边界约束

## 接口摘要
| 方法 | 输入 | 输出 | 副作用 | 约束 |
|------|------|------|--------|------|

## 依赖拓扑
- **上游**: 谁调用我
- **下游**: 我调用谁

## 领域事件
- 发布: EVENT_NAME
- 监听: EVENT_NAME

## 设计约束
- 架构决策记录

## 变更日志
- [日期] 变更描述
```

### 前端蓝图
```markdown
# [组件/模块名] Blueprint

## 职责契约
- **做什么**: 核心职责
- **不做什么**: 边界约束

## 接口摘要 (Props/Methods/API)
| 名称 | 类型 | 说明 | 必需 |
|------|------|------|------|

## 依赖拓扑
- **上游**: 谁使用我
- **下游**: 我使用谁

## 状态管理 (如适用)
- 本地状态
- 全局状态
- 副作用

## 设计约束
- 性能/交互/样式约束

## 变更日志
- [日期] 变更描述
```

## 🔍 快速查找

### 后端模块查找
| 需求 | 蓝图文件 |
|------|---------|
| 工作流执行 | `domain/workflow/WorkflowEngine.md` |
| 节点执行器 | `domain/workflow/NodeExecutor.md` |
| 人工审核 | `domain/workflow/HumanReview.md` |
| 智能体管理 | `domain/agent/AgentService.md` |
| 对话管理 | `domain/chat/ChatService.md` |
| 知识库 | `domain/knowledge/KnowledgeService.md` |
| 工作流调度 | `application/SchedulerService.md` |
| 控制器 | `interfaces/Controllers.md` |

### 前端模块查找
| 需求 | 蓝图文件 |
|------|---------|
| 工作流编辑器 | `frontend/pages/WorkflowEditorPage.md` |
| 大尺寸节点 | `frontend/components/WorkflowNodeLarge.md` |
| 节点面板 | `frontend/components/NodePanel.md` |
| 编辑器 Hook | `frontend/hooks/useWorkflowEditor.md` |
| 工作流服务 | `frontend/services/workflowService.md` |
| 认证状态 | `frontend/stores/authStore.md` |
| 类型定义 | `frontend/types/workflow.md` |

## ⚡ 常见场景

### 场景 1: 新增后端 Service
1. 读取 `.blueprint/_overview.md` 确认领域
2. 创建 `.blueprint/domain/xxx/XxxService.md`
3. 填写职责契约、接口摘要、依赖拓扑
4. 实现 `src/main/java/.../XxxService.java`
5. 更新 `_overview.md` 的依赖拓扑图

### 场景 2: 新增前端组件
1. 读取 `.blueprint/frontend/_overview.md` 确认分层
2. 创建 `.blueprint/frontend/components/XxxComponent.md`
3. 填写职责契约、Props、依赖拓扑
4. 实现 `ai-agent-foward/src/components/XxxComponent.tsx`
5. 更新父组件的蓝图依赖拓扑

### 场景 3: 修改接口
1. 读取对应的蓝图文件
2. 更新"接口摘要"章节
3. 在"变更日志"记录变更
4. 修改代码实现
5. 更新依赖该接口的上游模块蓝图

### 场景 4: Bug 修复
- 不涉及接口变更: 直接修改代码
- 涉及接口变更: 先更新蓝图,再修改代码

## 🚫 禁止行为

1. ❌ 未读蓝图直接改代码
2. ❌ 修改代码后不更新蓝图
3. ❌ 创建新模块不先建蓝图
4. ❌ 蓝图与代码不一致时以代码为准
5. ❌ 跨层级直接调用(绕过分层边界)

## ✅ 最佳实践

1. ✅ 每次修改前先读蓝图
2. ✅ 修改代码后同步更新蓝图
3. ✅ 新模块先建蓝图再写代码
4. ✅ 蓝图与代码不一致时以蓝图为准
5. ✅ 遵循分层边界和依赖方向

## 📊 蓝图检查清单

### 编码前
- [ ] 是否读取了相关蓝图?
- [ ] 是否理解了职责契约?
- [ ] 是否检查了依赖拓扑?
- [ ] 是否有循环依赖?

### 编码中
- [ ] 是否遵循了接口定义?
- [ ] 是否遵循了设计约束?
- [ ] 是否违反了职责边界?

### 编码后
- [ ] 是否更新了蓝图?
- [ ] 是否记录了变更日志?
- [ ] 是否验证了一致性?
- [ ] 是否更新了依赖模块的蓝图?

## 🔗 相关文档

- `.blueprint/_overview.md` - 系统全局依赖拓扑
- `.blueprint/frontend/_overview.md` - 前端架构总览
- `.blueprint/frontend/BLUEPRINT_REFACTORING.md` - 前端蓝图重构总结
- `.kiro/steering/blueprint-workflow.md` - 蓝图工作流详细说明

## 📞 获取帮助

1. 查看对应的 `_overview.md` 了解整体架构
2. 查看具体模块的蓝图文件
3. 查看 `BLUEPRINT_REFACTORING.md` 了解重构历史
4. 联系开发团队讨论

---

**最后更新**: 2026-02-13  
**维护者**: 开发团队
