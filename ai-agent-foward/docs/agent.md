# 前端重建开发规范（Dify 对齐版）

## 1. 文档定位
- 本规范用于指导后续前端“推倒重建”。
- 目标是对齐 `langgenius/dify` 的工作流编排实现思路与工程组织方式。
- 本规范优先级高于当前仓库已有前端结构；允许完全删除现有前端后按本规范重建。

## 2. 强制流程（必须执行）
1. 先读蓝图：先阅读 `.blueprint/_overview.md` 与 `.blueprint/frontend/*`，确认功能职责、历史决策、边界约束。
2. 再做上下文扫描：使用 Serena 对目标模块做符号级/模式级检索，确认复用点与影响面。
3. 最后编码：仅在蓝图与上下文确认后开始实现。
4. 蓝图同步：任何架构、目录、职责调整，必须同步更新 `.blueprint/frontend/`。

## 3. 对齐基线（参照 Dify）
- 参照仓库：`https://github.com/langgenius/dify`
- 参照范围（工作流前端）：
  - `web/app/components/workflow/index.tsx`
  - `web/app/components/workflow/constants.ts`
  - `web/app/components/workflow/types.ts`
  - `web/app/components/workflow/style.css`
  - `web/app/components/workflow/custom-edge.tsx`
  - `web/app/components/workflow/custom-connection-line.tsx`
  - `web/app/components/workflow/hooks/*`
  - `web/app/components/workflow/store/*`
  - `web/app/components/workflow/nodes/*`
  - `web/app/components/workflow/panel/*`
  - `web/app/components/workflow/operator/*`
  - `web/app/components/workflow/features/*`

## 4. 目标技术与工程基线
- TypeScript 严格模式：禁止 `any` 漫延，所有节点数据、事件、面板参数均需强类型。
- React + React Flow：API 与 Dify 基线保持一致（含自定义 edge、connection line、viewport 控制）。
- Zustand 分片状态：采用 slice 组织 workflow 编辑态，禁止页面大状态堆叠。
- 样式体系：组件内 Tailwind/类名组合 + workflow 域样式文件；禁止无边界全局样式污染。
- 事件体系：使用统一事件总线/常量事件名传递画布更新、运行态更新等事件。

## 5. 目录规范（重建后应接近如下结构）
```text
frontend/
  app/
    components/
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
        utils/
          index.ts
          layout.ts
          validation.ts
        operator/
          header.tsx
          control.tsx
          zoom.tsx
        panel/
          index.tsx
          config-panel.tsx
          env-panel/
        features/
          debug-and-preview/
          run-history/
        nodes/
          _base/
            node.tsx
            node-handle.tsx
            panel.tsx
          start/
          end/
          llm/
          tool/
          if-else/
          loop/
          iteration/
          knowledge-retrieval/
```

## 6. 架构原则
1. Page 只做装配，不承载核心编排逻辑。
2. 交互逻辑下沉到 `hooks`，状态归拢到 `store`。
3. 节点按“类型目录化”组织，每个节点拥有独立 `node/panel/default/schema`。
4. 连线、连接线、布局、校验是独立子模块，不和页面代码耦合。
5. 运行态、调试态、草稿同步逻辑必须与编辑态解耦。

## 7. 状态模型规范
- 工作流编辑态必须至少包含：
  - 控制模式：`pointer | hand`
  - 面板开关：节点配置、环境变量、调试预览、历史记录
  - 布局与画布：viewport、maximize 状态、缩放档位
  - 历史：undo/redo 栈、操作事件类型
  - 运行态：节点运行状态、边运行状态、是否只读
- 持久化策略：
  - 只持久化用户偏好与必要 UI 状态。
  - 草稿数据同步通过显式 `sync` 钩子，不允许隐式散落在页面内。

## 8. 交互规范
- 必须支持：
  - 拖拽加节点
  - 就地加节点（从 handle/边旁触发）
  - 连接校验（`isValidConnection`）
  - 自动布局（支持主图 + 子图场景）
  - 撤销/重做
  - 缩放/适配视图/手型平移模式
- 连接与布局操作必须纳入历史系统，保证可回退。
- 编辑与运行冲突时必须进入只读保护，不允许 silent fail。

## 9. UI/视觉规范（严格）
- 整体风格：克制、专业、信息优先，接近 Dify。
- 禁止：
  - 大面积“炫光/漂浮/重渐变/过度动效”
  - 多处全局覆写 React Flow 默认类导致不可控叠加
  - 线框粗重、箭头夸张、强发光描边
- 推荐：
  - 统一设计 token（颜色、圆角、边框、阴影、间距、字号）
  - 边/节点/面板使用同一语义色阶
  - 动效只用于状态反馈（连接中、运行中、可放置）

## 10. 节点体系规范
- 新增节点必须提供：
  - `node.tsx`（画布节点）
  - `panel.tsx`（配置面板）
  - `default.ts`（默认配置）
  - `types.ts`（类型）
  - `schema.ts`（校验）
- 节点能力定义必须包含：
  - 输入端口/输出端口契约
  - 变量引用规则
  - 运行状态映射
  - 错误与空配置兜底

## 11. 数据同步与后端契约规范
- 前端草稿模型与后端 DSL 必须有显式转换层。
- 禁止在 UI 层直接拼接后端请求体。
- 所有接口必须有类型定义与错误码处理。
- SSE/流式事件处理必须具备：
  - 断线重连策略
  - 事件去重/乱序保护
  - 终止态收敛

## 12. 开发阶段规划（执行顺序）
1. Phase A：骨架与基建
2. Phase B：画布与交互内核（节点、边、历史、布局、缩放）
3. Phase C：节点体系（Start/End/LLM/工具/分支/循环等）
4. Phase D：面板体系（配置、环境变量、调试预览、运行历史）
5. Phase E：运行联调（执行、日志、状态回放、错误处理）
6. Phase F：性能与稳定性（大图性能、内存、边界场景）

## 13. 质量门禁
- 提交前必须通过：
  - TypeScript 类型检查
  - Lint
  - Build
  - 关键交互回归（拖拽、连线、撤销重做、自动布局、运行）
- PR 必须包含：
  - 蓝图变更说明
  - 架构对齐简报（复用策略、差异说明）
  - 关键页面截图/GIF

## 14. 架构对齐简报模板（必须附在实现说明）
```markdown
**架构对齐检查**
- **发现可用组件**：...
- **发现关联实体**：...
- **复用策略**：[完全复用 / 扩展现有 / 新建关联]
- **与 Dify 差异**：...
- **差异处理理由**：...
```

## 15. 偏差处理规则
- 若与 Dify 完全一致会破坏业务需求，可局部偏离，但必须：
  - 在蓝图中记录偏差点
  - 说明偏差原因、影响范围、回收计划
  - 不得以“临时方案”长期占位

---

最后更新：2026-02-13  
维护人：前端编排重建负责人（Agent）
