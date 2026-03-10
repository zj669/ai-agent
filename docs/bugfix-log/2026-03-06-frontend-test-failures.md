# Bug 记录：前端测试大面积失败修复

| 项目 | 内容 |
|------|------|
| **日期** | 2026-03-06 |
| **模块** | 前端 `ai-agent-foward` / Vitest 测试 |
| **严重度** | P1（测试基线不可信，无法作为回归参考） |
| **发现方式** | 执行 `npx vitest run`，52/148 失败 |

---

## 问题描述

执行前端全量 Vitest 测试，初始状态 **16 文件失败、52 用例失败**。经分析分为三类根因。

### 根因一：`window.matchMedia is not a function`

Ant Design 6 的 `responsiveObserver` 在 jsdom 中调用 `window.matchMedia`，但 jsdom 不实现此 API。所有渲染了 Ant Design `Grid`/`Layout` 组件的测试均受影响（约 8 个用例）。

### 根因二：`ResizeObserver is not defined`

`@xyflow/react` 和部分 Ant Design 组件依赖 `ResizeObserver`，jsdom 不提供（1 个用例）。

### 根因三：`@xyflow/react` mock 不完整

`WorkflowNode.tsx` 和 `CustomEdge.tsx` 使用了 `useReactFlow` hook，但测试文件的 `vi.mock('@xyflow/react')` 未导出此函数，导致运行时报 `No "useReactFlow" export is defined on the mock`。

### 根因四：测试预期与代码不匹配

- `NodeSelector.test.tsx`：预期 4 种节点类型，实际代码已增加 `KNOWLEDGE`（共 5 种）
- `WorkflowNode.test.tsx`：预期 START 节点不显示展开箭头，但代码已改为 `canExpand = true`
- `httpClient.sideEffects.test.ts`：测试预期调用 `showToast` 和 `clearAccessToken`，但 httpClient 已重构为直接操作 localStorage

---

## 修复方案

### 修复一：test/setup.ts 添加全局 mock

```ts
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    matches: false, media: query, onchange: null,
    addListener: () => {}, removeListener: () => {},
    addEventListener: () => {}, removeEventListener: () => {},
    dispatchEvent: () => false,
  }),
})

global.ResizeObserver = class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}
```

### 修复二：workflow 测试补全 @xyflow/react mock

- `WorkflowNode.test.tsx`：mock 增加 `useReactFlow` 返回 `{ setNodes, setEdges, getEdges, getNodes }`
- `CustomEdge.test.tsx`：mock 增加 `useReactFlow` 和 `EdgeLabelRenderer`
- `NodeSelector.test.tsx`：预期从 4 改为 5，增加知识库断言
- `WorkflowNode.test.tsx`：START 节点展开箭头断言从 `not.toBeInTheDocument` 改为 `toBeInTheDocument`

### 修复三：httpClient 测试适配当前实现

重写 `httpClient.sideEffects.test.ts`，移除对已删除 API（`showToast`、`clearAccessToken`）的断言，改为验证 `localStorage` 清除和 `location.href` 跳转。

---

## 修复效果

| 指标 | 修复前 | 修复后 | 改善 |
|------|--------|--------|------|
| 失败文件数 | 16 | 12 | -4 |
| 失败用例数 | 52 | 37 | -15 |
| 通过用例数 | 96 | 111 | +15 |

### 剩余 12 个失败文件（UI 集成测试漂移）

| 文件 | 失败数 | 原因 |
|------|--------|------|
| `chat.page-streaming.test.tsx` | 9 | 聊天页 UI 结构变更，找不到「默认会话」等文案 |
| `knowledge.main-flow.test.tsx` | 6 | 知识库页面重构，找不到「技术文档库」option |
| `agent.create-to-workflow.test.tsx` | 5 | Agent 列表页重构，找不到「新建 Agent」按钮 |
| `rememberMe.test.tsx` | 5 | 登录页表单结构变更 |
| `authRoutes.test.tsx` | 4 | 找不到「登录」heading |
| `router.auth-guard.test.tsx` | 2 | 找不到「AI Agent 平台」heading |
| `chatService.test.ts` | 1 | SSE 流解析 API 变更 |
| `knowledgeAdapter.test.ts` | 1 | 分页响应结构变更 |
| `app.boot.test.tsx` | 1 | 登录页 heading 变更 |
| `App.test.tsx` | 1 | 布局结构变更 |
| `login3d.fallback.test.tsx` | 1 | 降级背景渲染变更 |
| `dashboard.new-agent-route.test.tsx` | 1 | 路由跳转逻辑变更 |

这些属于 **UI-测试漂移**（页面持续迭代但测试未同步更新），需要后续按模块逐一重写。

---

## 涉及文件

| 文件 | 操作 | 改动要点 |
|------|------|----------|
| `src/test/setup.ts` | 修改 | 添加 `matchMedia` 和 `ResizeObserver` 全局 mock |
| `src/modules/workflow/components/__tests__/WorkflowNode.test.tsx` | 修改 | 补全 `useReactFlow` mock + START 节点断言修正 |
| `src/modules/workflow/components/__tests__/CustomEdge.test.tsx` | 修改 | 补全 `useReactFlow` + `EdgeLabelRenderer` mock |
| `src/modules/workflow/components/__tests__/NodeSelector.test.tsx` | 修改 | 节点数 4→5，增加知识库断言 |
| `src/shared/api/__tests__/httpClient.sideEffects.test.ts` | 重写 | 适配 httpClient 当前实现 |

---

## 验证

```
npx vitest run
  Test Files  12 failed | 25 passed (37)
  Tests       37 failed | 111 passed (148)
```
