# 蓝图变更摘要 - 工作流大尺寸节点

## 📋 变更概述

**变更类型**: 前端组件扩展 + UI 增强  
**影响范围**: WorkflowEditor 页面及相关组件  
**蓝图文件**: `.blueprint/frontend/FrontendArchitecture.md`  
**变更日期**: 2026-02-13

## 🎯 变更动机

基于用户反馈和参考截图(类似 Dify/Coze),当前工作流编辑器存在以下问题:
1. 节点信息密度不足,配置需要点击才能查看
2. 视觉设计简陋,缺少现代感
3. 编辑效率低,需要频繁切换面板

## 📐 架构变更

### 1. 新增组件

| 组件 | 路径 | 职责 |
|------|------|------|
| WorkflowNodeLarge | `components/WorkflowNodeLarge.tsx` | 大尺寸节点渲染,内嵌配置预览 |
| ConfigPreview | WorkflowNodeLarge 内部 | 节点类型特定的配置摘要展示 |
| NodeSizeToggle | `components/NodeSizeToggle.tsx` | 节点尺寸模式切换控制 |

### 2. 新增样式文件

| 文件 | 职责 |
|------|------|
| `styles/workflow-enhanced.css` | 通用增强样式(背景、工具栏、面板) |
| `styles/workflow-large-nodes.css` | 大节点专用样式(节点卡片、连接线) |

### 3. 组件依赖关系

```
WorkflowEditorPage
├─ NodeSizeToggle (新增)
│  └─ 控制 useLargeNodes 状态
├─ ReactFlow
│  ├─ nodeTypes: WorkflowNode (原有)
│  └─ nodeTypes: WorkflowNodeLarge (新增)
│     └─ ConfigPreview (新增)
│        ├─ LLMConfigPreview
│        ├─ HTTPConfigPreview
│        ├─ ConditionConfigPreview
│        └─ ToolConfigPreview
```

## 🔄 职责契约变更

### WorkflowEditorPage (扩展)

**新增职责**:
- 管理节点尺寸模式状态(useLargeNodes)
- 根据模式动态选择节点类型(largeNodeTypes vs nodeTypes)

**不变职责**:
- 工作流图管理、执行控制、画布交互

### WorkflowNodeLarge (新增)

**做什么**:
- 渲染大尺寸节点(320-400px)
- 内嵌显示节点配置摘要
- 提供快捷操作按钮(测试/配置)
- 可视化执行状态

**不做什么**:
- 不处理节点编辑逻辑(由 NodePropertiesPanel 负责)
- 不管理节点数据(由 useWorkflowEditor hook 负责)
- 不处理拖拽逻辑(由 ReactFlow 负责)

### ConfigPreview (新增)

**做什么**:
- 根据节点类型展示配置摘要
- 格式化配置数据为可读文本
- 处理长文本截断

**不做什么**:
- 不允许编辑配置
- 不发起网络请求
- 不管理状态

## 📊 依赖拓扑变更

### 新增依赖

```
WorkflowEditorPage
  → NodeSizeToggle (新增)
  → WorkflowNodeLarge (新增)
    → ConfigPreview (新增)
    → Ant Design (Button, Tag, Tooltip)
    → Lucide Icons
```

### 无循环依赖

所有新增组件遵循单向数据流:
- Page → Component → Subcomponent
- 无组件间相互依赖
- 无跨层级直接调用

## 🎨 设计约束变更

### 新增约束

1. **双节点模式共存**
   - 紧凑节点和大尺寸节点必须共存
   - 通过 NodeSizeToggle 实现运行时切换
   - 节点数据结构保持一致

2. **配置预览规范**
   - 每种节点类型必须实现 ConfigPreview
   - 预览内容限制在 200px 高度内
   - 长文本使用 line-clamp 截断

3. **样式隔离**
   - 大节点样式通过 `.workflow-node-large` 类名隔离
   - 不影响原有紧凑节点样式
   - 支持独立启用/禁用

4. **性能约束**
   - 节点数 > 50 时建议切换到紧凑节点
   - 使用 React.memo 优化渲染
   - 配置预览区域支持虚拟滚动

## 🚫 未变更内容

以下内容保持不变,确保向后兼容:

1. **数据结构**: WorkflowNode, WorkflowEdge, NodeConfig 等类型定义
2. **业务逻辑**: useWorkflowEditor hook, workflowService
3. **页面布局**: 三栏骨架(左配置/属性 + 中画布 + 右节点栏)
4. **交互流程**: 拖拽添加、连接节点、执行工作流
5. **后端接口**: 无任何后端 API 变更

## ✅ 蓝图一致性检查

### 分层边界检查

- ✅ WorkflowNodeLarge 属于 components 层,符合分层规范
- ✅ 无直接访问 services 或 stores
- ✅ 通过 props 接收数据,符合单向数据流
- ✅ 无跨层级调用

### 职责边界检查

- ✅ 节点渲染职责明确(WorkflowNodeLarge)
- ✅ 配置预览职责独立(ConfigPreview)
- ✅ 模式切换职责独立(NodeSizeToggle)
- ✅ 无职责重叠或蔓延

### 依赖方向检查

- ✅ Page → Component → Subcomponent (正确)
- ✅ 无 Component → Page 反向依赖
- ✅ 无组件间循环依赖

## 📝 代码投影清单

### 新增文件

```
ai-agent-foward/src/
├── components/
│   ├── WorkflowNodeLarge.tsx (新增)
│   └── NodeSizeToggle.tsx (新增)
└── styles/
    ├── workflow-enhanced.css (新增)
    └── workflow-large-nodes.css (新增)
```

### 修改文件

```
ai-agent-foward/src/
├── index.css (引入新样式)
└── pages/
    └── WorkflowEditorPage.tsx (待修改,添加切换逻辑)
```

### 文档文件

```
ai-agent-foward/.shared/ui-ux-pro-max/
├── WORKFLOW_REDESIGN_PLAN.md (完整方案)
├── IMPLEMENTATION_GUIDE.md (实现指南)
├── QUICK_START.md (快速开始)
└── BLUEPRINT_CHANGES.md (本文件)
```

## 🔍 验证清单

### 功能验证

- [ ] 大节点正确渲染,尺寸符合规范(320-400px)
- [ ] 配置预览正确显示各节点类型配置
- [ ] 节点尺寸切换功能正常
- [ ] 切换后节点数据保持一致
- [ ] 拖拽、连接等交互正常

### 性能验证

- [ ] 大节点模式下,30 个节点流畅运行
- [ ] 切换节点模式响应时间 < 100ms
- [ ] 无内存泄漏
- [ ] 无不必要的重渲染

### 兼容性验证

- [ ] 原有紧凑节点功能不受影响
- [ ] 现有工作流数据正常加载
- [ ] 样式不冲突
- [ ] 支持主流浏览器(Chrome, Firefox, Edge)

### 代码质量验证

- [ ] TypeScript 类型检查通过
- [ ] ESLint 检查通过
- [ ] 无 console 警告或错误
- [ ] 代码符合项目规范

## 🎯 后续优化建议

1. **节点库面板**: 替换左侧编排配置面板为节点库
2. **属性编辑器**: 替换右侧快捷添加栏为属性编辑器
3. **连接线标签**: 在连接线上显示条件标签
4. **节点分组**: 支持节点分组和折叠
5. **主题切换**: 支持亮色/暗色主题

## 📞 问题反馈

如发现蓝图与实现不一致,或有架构改进建议,请:
1. 检查本文档确认变更范围
2. 查看 IMPLEMENTATION_GUIDE.md 了解实现细节
3. 联系开发团队讨论

---

**蓝图更新人**: AI Assistant  
**审核状态**: 待用户确认  
**下一步**: 用户确认后进行代码投影(Step 3)
