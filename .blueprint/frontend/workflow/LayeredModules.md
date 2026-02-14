# Workflow 模块分层 Blueprint（nodes/panel/operator/features/utils）

## 职责契约
- **做什么**: 定义 workflow 子域模块职责、输入输出边界、依赖方向。
- **不做什么**: 不定义具体样式 token 值，不替代节点类型实现细节。

## 子域职责边界
| 子域 | 主要职责 | 禁止事项 |
|---|---|---|
| `nodes/` | 节点渲染、默认配置、schema、变量引用契约 | 直接请求后端、写面板全局状态 |
| `panel/` | 节点配置面板、工作流配置、环境变量面板 | 承载图结构算法 |
| `operator/` | 头部工具栏、缩放控件、画布控制 | 节点业务逻辑与 DSL 转换 |
| `features/` | 调试预览、运行历史、执行日志等功能子系统 | 与节点渲染耦合 |
| `utils/` | 布局、校验、图转换、纯函数工具 | 可变状态与副作用 |

## 推荐目录（示意）
```text
workflow/
  nodes/
    _base/{node.tsx,node-handle.tsx,panel.tsx}
    start/{node.tsx,panel.tsx,default.ts,types.ts,schema.ts}
    end/{node.tsx,panel.tsx,default.ts,types.ts,schema.ts}
    llm/{...}
    tool/{...}
    if-else/{...}
  panel/
    index.tsx
    workflow-config-panel.tsx
    node-config-panel.tsx
    env-panel/
  operator/
    header.tsx
    control.tsx
    zoom.tsx
  features/
    debug-and-preview/
    run-history/
  utils/
    index.ts
    layout.ts
    validation.ts
    graph-transformer.ts
```

## 依赖方向（强约束）
1. `nodes` 仅依赖 `types/constants/utils`，不得依赖 `features`。
2. `panel/operator/features` 通过 hooks API 获取状态，不反向依赖 pages。
3. `utils` 可被全域复用，但必须保持纯函数、可单测。
4. `features` 可以读取 store 与 services，但不能直接改节点组件结构。

## WorkflowEditorPage 装配原则
- 页面只负责 route params、初始化数据、权限与只读态注入。
- 页面不直接持有 `onNodesChange/onEdgesChange/onConnect` 算法。
- 页面不实现 undo/redo、布局、草稿同步逻辑。
- 页面通过 `workflow/index.tsx` 完成模块装配与事件桥接。

## 可验证检查清单
- 页面文件中不存在图编辑核心逻辑。
- hooks 文件中不存在 JSX。
- store 文件中不存在网络调用。
- utils 文件可以在无 React 环境下运行测试。

## 与 Dify 差异管理
- 若业务必须偏离 Dify，需在蓝图记录偏差点、影响范围、回收计划。
- 偏差不允许以“临时方案”长期占位。

## 变更摘要
- 固化 nodes/panel/operator/features/utils 五层分工与禁止事项。
- 强制 `WorkflowEditorPage` 退化为装配层，剥离编排细节。
- 增加可验证清单与差异管理规则，支持重构验收。

## 变更日志
- [2026-02-13] 重写模块分层蓝图，统一 Dify 模式职责边界。
