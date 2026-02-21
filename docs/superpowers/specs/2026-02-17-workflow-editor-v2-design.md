# Workflow Editor V2 设计文档（方案3：一次性重写）

## 1. 决策与范围

### 1.1 最终决策
- 采用**方案3：一次性重写 Workflow Editor**。
- 本次重写目标是替换旧编辑器的核心实现（状态、连线、映射、面板联动），并在同一切换窗口完成上线。

### 1.2 兼容性红线
- 必须保持 `graphJson` **完全兼容**（输入可读、输出可写、语义一致）。
- 后端接口协议不变；V2 仅在前端内部重构。

### 1.3 非目标
- 不引入新的节点类型协议。
- 不改变工作流执行引擎语义。
- 不做 UI 视觉大改，仅做必要反馈增强（尤其是连线失败反馈）。

---

## 2. 目标架构与模块边界

V2 按“页面壳层 + 能力模块”拆分：

1. **page（页面壳层）**
   - 负责路由参数、加载/保存触发、整体布局。
   - 不直接操作 ReactFlow 细节，不承载业务校验规则。

2. **canvas（画布层）**
   - 负责节点渲染、拖拽、连线事件分发、交互反馈入口。
   - 只做 UI 交互编排，不存放业务合法性规则。

3. **state（状态层）**
   - 管理编辑器单一事实源：nodes/edges/selection/dirty/pendingConnection。
   - 提供原子更新动作与状态机驱动入口。

4. **mapping（映射层）**
   - 负责 `graphJson <-> editorState` 双向转换。
   - 负责 ID 归一化、字段兼容填充、默认值投影。

5. **validation（校验层）**
   - 统一封装节点/边/保存前校验。
   - 输出结构化错误码与可展示消息，禁止静默失败。

6. **panels（属性面板层）**
   - 监听选中态，驱动节点属性编辑。
   - 通过状态层动作更新，不直写画布内部对象。

7. **feedback（反馈层）**
   - 承载 toast/inline/error badge 等显式反馈。
   - 统一消费 validation 返回结果，覆盖连线失败等关键路径。

---

## 3. 核心数据流

## 3.1 初始化
1. Page 拉取后端 `graphJson`。
2. Mapping 层执行 `deserializeGraphJson`，完成 ID 归一化与结构适配。
3. Validation 执行初始化校验（结构级），将警告/错误注入反馈层。
4. State 建立初始快照，Canvas 渲染。

## 3.2 连线成功
1. Canvas 触发 `onConnect`（含 sourceHandle/targetHandle）。
2. Validation 执行 `validateConnection`。
3. 通过后 State 原子追加 edge，并更新 dirty。
4. Feedback 显示可选成功提示（轻量）。

## 3.3 连线失败（显式反馈）
1. Canvas 触发连线尝试。
2. Validation 返回失败原因（如 handle 缺失、重复边、类型不匹配、违反节点约束）。
3. State 不变（拒绝提交）。
4. Feedback 必须显式提示错误（toast + 可选高亮相关节点/handle）。

## 3.4 属性面板联动
1. Canvas/Node 变化选中态。
2. Panels 读取当前选中节点快照。
3. 用户编辑字段 -> 调用 State `updateNodeConfig`。
4. Validation 做字段级校验；失败时保留用户输入并展示错误。

## 3.5 保存
1. Page 触发保存。
2. Validation 执行保存前基础校验（节点完整性、连线合法性、必填配置）。
3. 通过后 Mapping 执行 `serializeEditorState` 生成兼容 `graphJson`。
4. 调用保存接口；成功后清理 dirty 并同步版本。

---

## 4. 状态机设计

## 4.1 编辑器主状态机

状态集合：
- `BOOTSTRAPPING`：初始化中
- `READY`：可编辑稳定态
- `EDITING`：发生未保存变更
- `VALIDATING`：保存前校验中
- `SAVING`：保存请求中
- `SAVE_FAILED`：保存失败

关键流转：
- `BOOTSTRAPPING -> READY`：初始化成功
- `READY -> EDITING`：任意合法编辑动作
- `EDITING -> VALIDATING -> SAVING -> READY`：保存成功闭环
- `VALIDATING -> EDITING`：校验失败（保留编辑态）
- `SAVING -> SAVE_FAILED -> EDITING`：保存失败回退

## 4.2 连线子状态机

状态集合：
- `IDLE`：未连线
- `DRAGGING`：拖拽连接中
- `CHECKING`：校验中
- `COMMITTED`：连线提交成功
- `REJECTED`：连线拒绝（显式反馈）

关键流转：
- `IDLE -> DRAGGING`：开始拖拽
- `DRAGGING -> CHECKING`：释放到目标 handle
- `CHECKING -> COMMITTED -> IDLE`：校验通过
- `CHECKING -> REJECTED -> IDLE`：校验失败并展示错误

---

## 5. 强制技术约束（必须执行）

1. **Handle 显式 id**
   - 所有可连线 Handle 必须声明稳定 `id`，禁止依赖隐式默认。

2. **禁止 silent return**
   - 连线/保存/面板提交失败时必须返回结构化错误，并触发反馈层展示。

3. **统一校验函数**
   - 连线合法性仅由 `validateConnection(...)` 判定；保存前仅由 `validateBeforeSave(...)` 判定。

4. **ID 归一化**
   - Mapping 层统一执行 `normalizeNodeId/normalizeEdgeId`，输入输出双向一致。

5. **保存前基础校验**
   - 保存必须先过基础校验，禁止“先发请求后报错”的倒置流程。

---

## 6. 校验规则清单

## 6.1 连线校验
- source/target 节点必须存在。
- sourceHandle/targetHandle 必须存在且可连接。
- 禁止自环（除非未来明确放开）。
- 禁止重复边（同 source+target+handle 组合）。
- 起止节点类型组合必须符合约束（如 End 节点入边策略）。

## 6.2 节点配置校验
- 节点类型必填字段不能为空。
- 字段类型合法（字符串、数值、布尔、对象结构）。
- 条件/表达式类节点必须满足最小配置集。

## 6.3 保存前校验
- 图至少有起点与终点（按现有业务规则）。
- 不存在悬空关键节点（按业务定义）。
- 不存在引用缺失的变量/节点依赖（静态可检测范围内）。

---

## 7. 测试策略

1. **单测（Unit）**
   - 覆盖 validation 与 mapping 纯函数：
   - 连线合法/非法矩阵、ID 归一化、graphJson round-trip。

2. **交互测试（组件/集成）**
   - 覆盖 Canvas 连线成功与失败反馈、面板联动更新。

3. **E2E（端到端）**
   - 覆盖“加载-编辑-保存-重载”主链路。
   - 覆盖典型错误路径（无效连线、缺失必填）。

4. **兼容性回放（Replay）**
   - 用历史样本 `graphJson` 批量回放：反序列化 -> 序列化 -> 语义对比。

---

## 8. 一次性切换门禁与回退预案

## 8.1 切换门禁（Go/No-Go）
必须全部满足：
- 单测、交互、E2E 全绿。
- `graphJson` 兼容性回放通过率 100%。
- 连线失败路径均有显式反馈，无 silent return。
- 关键页面性能与稳定性不低于旧版基线。

## 8.2 回退预案
- 保留旧编辑器实现一个发布窗口周期（代码保留但默认不启用）。
- 若出现 P0/P1 兼容问题：
  1) 切回旧页面入口（路由或开关）
  2) 冻结 V2 保存入口（只读降级可选）
  3) 记录样本并补充回放集后再二次切换

---

## 9. 工作包（WP1-WP6）与验收标准

## WP1 蓝图更新（Blueprint First）
- 内容：更新 `.blueprint` 中 Workflow Editor 相关契约与模块边界。
- 验收：蓝图完成审阅，明确 page/canvas/state/mapping/validation/panels/feedback 职责。

## WP2 状态层与校验层
- 内容：建立 V2 state store 与统一 validation API。
- 验收：连线/保存/字段校验均走统一入口，具备错误码与消息。

## WP3 Canvas 连线链路重建
- 内容：改造 handle 显式 id、连接事件链路、失败反馈。
- 验收：连线成功可提交，失败必提示，无静默失败。

## WP4 Mapping 兼容层
- 内容：实现 graphJson 双向映射与 ID 归一化。
- 验收：历史样本 round-trip 结果兼容，关键字段不丢失。

## WP5 属性面板联动
- 内容：选中态驱动面板，面板编辑回写状态层。
- 验收：编辑即时生效，非法输入可见错误，状态一致。

## WP6 页面切换、测试与门禁
- 内容：旧实现替换、全量测试、切换与回退机制演练。
- 验收：门禁项全部通过，具备可执行回退流程。

---

## 10. 里程碑结论

本设计以“**一次性重写 + 完全兼容 graphJson**”为唯一主线，使用模块边界收敛复杂度，使用统一校验与显式反馈保证可观测性，并通过门禁与回退预案控制上线风险。