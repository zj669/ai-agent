# Workflow 目标目录结构 Blueprint

## 职责契约
- **做什么**: 定义 workflow 域最终目录结构与顶层文件职责。
- **不做什么**: 不定义节点内部 UI 细节，不定义后端实现。

## 目标结构（必须对齐）
```text
workflow/
  index.tsx
  constants.ts
  types.ts
  style.css
  custom-edge.tsx
  custom-connection-line.tsx
  hooks/
    use-workflow-interactions.ts
    use-nodes-interactions.ts
    use-edges-interactions.ts
    use-workflow-history.ts
    use-nodes-sync-draft.ts
  store/
    index.ts
    workflow/
      workflow-slice.ts
      layout-slice.ts
      panel-slice.ts
      history-slice.ts
  nodes/
  panel/
  operator/
  features/
  utils/
```

## 顶层文件职责矩阵
| 文件 | 必要职责 | 禁止事项 |
|---|---|---|
| `index.tsx` | 装配 ReactFlow 容器与子模块 | 直接写业务规则 |
| `constants.ts` | 交互常量、事件名、默认参数 | 写可变运行态 |
| `types.ts` | workflow 域公共类型入口 | 泄漏 UI 实现类型 |
| `style.css` | workflow 域样式收口 | 污染全局通用样式 |
| `custom-edge.tsx` | 边渲染、边交互入口 | 编排节点配置逻辑 |
| `custom-connection-line.tsx` | 连线中临时视觉反馈 | 管理全局状态 |

## 目录落地规则
1. hooks 只提供交互能力，不输出 JSX。
2. store 只管理状态，不发请求、不碰 DOM。
3. nodes/panel/operator/features 按职责隔离，不跨域混写。
4. utils 只保留纯函数，支持独立测试。

## 迁移阶段建议
- Phase A：先建 `constants/types/store/index`。
- Phase B：拆出 5 个 hooks，建立历史与同步主链路。
- Phase C：迁移 nodes/panel/operator/features。
- Phase D：页面切换到 `workflow/index.tsx` 装配入口。

## 验收标准
- 目录中包含 6 个顶层核心文件与 5 个 hooks。
- store 中存在 `workflow/layout/panel/history` 四个 slices。
- 页面不再直接承载 nodes/edges 交互细节。

## 变更摘要
- 明确 workflow 的 Dify 对齐目录骨架与文件责任矩阵。
- 强制约束 `index/constants/types/style/custom-edge/custom-connection-line` 六要素。
- 定义可验证的迁移与验收步骤，支持分阶段实施。

## 变更日志
- [2026-02-13] 重构为 Dify 模式目标结构蓝图。
