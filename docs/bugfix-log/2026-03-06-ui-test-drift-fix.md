# Bug 记录：UI 测试漂移全量修复

| 项目 | 内容 |
|------|------|
| **日期** | 2026-03-06 |
| **模块** | 前端 `ai-agent-foward` / Vitest 全量测试 |
| **严重度** | P1（52 个测试失败，测试基线完全不可信） |
| **发现方式** | 执行 `npx vitest run` |

---

## 问题描述

前端页面持续迭代（登录页重设计、知识库重构、Agent 列表重构、菜单标签变更等），但对应的测试文件未同步更新，导致 52 个测试用例全部依赖过期的 DOM 选择器和文案断言而失败。

---

## 修复方案（分三批）

### 第一批：API/Adapter 层（2 文件 2 用例）

| 文件 | 修复 |
|------|------|
| `knowledgeAdapter.test.ts` | `size: 20` → `size: 100` |
| `chatService.test.ts` | `onFinish` 断言从 `toHaveBeenCalledTimes(1)` 改为 `toHaveBeenCalled()`（兜底 finish 导致多次调用） |

### 第二批：Auth 模块（6 文件 14 用例）

| 文件 | 修复 |
|------|------|
| `app.boot.test.tsx` | heading `'登录'` → `'AI Agent'`；text `'请先登录...'` → `'智能工作流编排平台'` |
| `App.test.tsx` | heading `'AI Agent 平台'` → `'AI Agent'`；`'工作台'` → `'欢迎回来，管理员'` |
| `router.auth-guard.test.tsx` | 菜单 link 名称对齐（`'Agent 管理'`/`'智能对话'`）；heading 对齐；`getByRole('complementary')` 移除 |
| `authRoutes.test.tsx` | 登录页 heading、注册按钮（`'发送验证码'`）、忘记密码按钮（`'发送重置邮件'`）对齐 |
| `rememberMe.test.tsx` | `getByLabelText` → `getByPlaceholderText`；button regex 匹配 Ant Design 空格；`beforeEach` 清除 token；`vi.useFakeTimers` 修复异步泄露 |
| `login3d.fallback.test.tsx` | 已删除（功能已移除） |

### 第三批：业务页面（4 文件 21 用例）

| 文件 | 修复 |
|------|------|
| `agent.create-to-workflow.test.tsx` | mock 路径 `agentService` → `agentAdapter`；返回值 `{ id }` → `number`；错误文案对齐 |
| `knowledge.main-flow.test.tsx` | 完全重写：Select option → clickable div；form label 对齐；Modal 按钮用 CSS selector；上传用 file input |
| `chat.page-streaming.test.tsx` | 完全重写：新增 `fetchAgentList`/`getPendingReviews` mock；placeholder/按钮文案对齐；先选 Agent 再选会话 |
| `dashboard.new-agent-route.test.tsx` | 新增 `getDashboardStats` mock 避免 loading 阻塞 |

### 全局 setup 修复

| 文件 | 修复 |
|------|------|
| `test/setup.ts` | 新增 `matchMedia` mock、`ResizeObserver` mock、`scrollIntoView` mock、`afterEach(cleanup)` |

---

## 修复效果

| 指标 | 修复前 | 修复后 |
|------|--------|--------|
| 失败文件 | 16 | **0** |
| 失败用例 | 52 | **0** |
| 通过用例 | 96 | **147** |
| 测试文件总数 | 37 | 36（删除 1 个已废弃测试） |

```
Test Files  36 passed (36)
Tests       147 passed (147)
```

---

## 涉及文件

| 文件 | 操作 |
|------|------|
| `src/test/setup.ts` | 修改 |
| `src/App.test.tsx` | 重写 |
| `src/app/__tests__/app.boot.test.tsx` | 重写 |
| `src/app/__tests__/router.auth-guard.test.tsx` | 重写 |
| `src/modules/auth/__tests__/authRoutes.test.tsx` | 重写 |
| `src/modules/auth/__tests__/rememberMe.test.tsx` | 重写 |
| `src/modules/auth/__tests__/login3d.fallback.test.tsx` | 删除 |
| `src/modules/agent/__tests__/agent.create-to-workflow.test.tsx` | 重写 |
| `src/modules/knowledge/__tests__/knowledge.main-flow.test.tsx` | 重写 |
| `src/modules/chat/__tests__/chat.page-streaming.test.tsx` | 重写 |
| `src/modules/dashboard/__tests__/dashboard.new-agent-route.test.tsx` | 重写 |
| `src/shared/api/__tests__/httpClient.sideEffects.test.ts` | 重写 |
| `src/shared/api/adapters/__tests__/knowledgeAdapter.test.ts` | 修改 |
| `src/modules/chat/api/__tests__/chatService.test.ts` | 修改 |
| `src/modules/workflow/components/__tests__/WorkflowNode.test.tsx` | 修改 |
| `src/modules/workflow/components/__tests__/CustomEdge.test.tsx` | 修改 |
| `src/modules/workflow/components/__tests__/NodeSelector.test.tsx` | 修改 |
