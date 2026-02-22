# UI 美化项目 - 共享上下文文档

> 本文档为 UI 美化项目的统一参考，所有子任务共享此上下文，避免重复调研。

---

## 1. 项目背景

- 项目：AI Agent Platform 前端
- 技术栈：React 19 + Vite + TypeScript + Tailwind CSS
- 当前 UI 状态：功能可用但视觉极简，像纯 HTML 页面，缺少现代 UI 设计感
- 目标：对登录后所有业务页面进行现代化 UI 重构，提升视觉品质和用户体验
- 范围：仅涉及登录后的业务页面（Dashboard、Agent 列表、知识库、聊天、404），不涉及登录/注册页

---

## 2. 技术栈与约束

### 实际版本（来自 package.json）

| 依赖 | 版本 |
|------|------|
| React | ^19.2.0 |
| Vite | ^7.1.3 |
| TypeScript | ~5.9.2 |
| Tailwind CSS | ^3.4.17 |
| @xyflow/react | ^12.10.1 |
| Axios | ^1.11.0 |
| clsx | ^2.1.1 |
| tailwind-merge | ^3.3.1 |
| react-router-dom | ^7.13.0 |
| vitest | ^3.2.4 |

### 约束规则

- 已有 `cn()` 工具函数（`src/lib/utils.ts`），基于 clsx + tailwind-merge，所有动态 class 合并使用此函数
- 已安装但未使用 Ant Design（本次美化不使用 Ant Design）
- 图标统一使用 inline SVG，不引入图标库
- 不引入新的 npm 依赖
- 保持现有组件的 props 接口和业务逻辑不变，仅修改 UI 层

---

## 3. 设计规范

### 3.1 CSS 变量体系（来自 index.css :root）

```css
/* 背景与前景 */
--background: 0 0% 100%;           /* 纯白 hsl(0, 0%, 100%) */
--foreground: 222.2 84% 4.9%;      /* 深蓝黑 hsl(222.2, 84%, 4.9%) */

/* 卡片 */
--card: 0 0% 100%;                 /* 纯白 */
--card-foreground: 222.2 84% 4.9%; /* 同 foreground */

/* 弹出层 */
--popover: 0 0% 100%;
--popover-foreground: 222.2 84% 4.9%;

/* 主色（蓝色系） */
--primary: 221.2 83.2% 53.3%;           /* hsl(221.2, 83.2%, 53.3%) 亮蓝 */
--primary-foreground: 210 40% 98%;       /* 近白 */

/* 次要色 */
--secondary: 210 40% 96.1%;             /* 浅灰蓝 */
--secondary-foreground: 222.2 47.4% 11.2%;

/* 静音色 */
--muted: 210 40% 96.1%;                 /* 同 secondary */
--muted-foreground: 215.4 16.3% 46.9%;  /* 中灰 */

/* 强调色 */
--accent: 210 40% 96.1%;                /* 同 secondary */
--accent-foreground: 222.2 47.4% 11.2%;

/* 危险色 */
--destructive: 0 84.2% 60.2%;           /* 红色 */
--destructive-foreground: 210 40% 98%;

/* 边框与输入 */
--border: 214.3 31.8% 91.4%;            /* 浅灰边框 */
--input: 214.3 31.8% 91.4%;             /* 同 border */
--ring: 221.2 83.2% 53.3%;              /* 同 primary，用于 focus ring */

/* 圆角 */
--radius: 0.5rem;                        /* 8px */
```

### 3.2 Tailwind 扩展配置（来自 tailwind.config.ts）

- 颜色映射：所有语义色通过 `hsl(var(--xxx))` 引用 CSS 变量
- 圆角：`lg = var(--radius)` = 0.5rem, `md = calc(var(--radius) - 2px)`, `sm = calc(var(--radius) - 4px)`
- 自定义动画：`float-slow`（6s 上下浮动 20px）、`float-mid`（4s 上下浮动 14px，延迟 1s）
- 暗色模式：配置为 `class` 策略（当前未启用）

### 3.3 字体栈（来自 index.css body）

```css
font-family: Inter, 'PingFang SC', 'Microsoft YaHei', system-ui, sans-serif;
```

### 3.4 统一组件样式规范

所有子任务必须遵循以下样式约定，确保全局一致性：

**卡片**
```
rounded-xl bg-white border border-slate-200 shadow-sm hover:shadow-md transition
```

**输入框**
```
rounded-lg border border-slate-200 bg-slate-50/50 px-4 py-2.5
focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none
transition text-sm
```

**主按钮**
```
rounded-lg bg-primary text-primary-foreground px-4 py-2.5 text-sm font-medium
hover:bg-primary/90 transition
disabled:opacity-50 disabled:cursor-not-allowed
```

**次要按钮**
```
rounded-lg border border-slate-200 px-4 py-2.5 text-sm font-medium
hover:bg-slate-50 transition
disabled:opacity-50 disabled:cursor-not-allowed
```

**危险按钮**
```
rounded-lg bg-destructive text-destructive-foreground px-4 py-2.5 text-sm font-medium
hover:bg-destructive/90 transition
```

**页面标题**
```
text-2xl font-bold tracking-tight text-foreground
```

**页面副标题/描述**
```
text-sm text-muted-foreground mt-1
```

**空状态**
```
flex flex-col items-center justify-center py-16 text-center
图标 + 标题 + 描述 + 操作按钮
```

---

## 4. 关键布局参数（来自 AppShell.tsx）

| 元素 | 样式 | 说明 |
|------|------|------|
| Header | `fixed top-0 h-14 z-30 bg-white/80 backdrop-blur-md` | 固定顶部，高度 3.5rem |
| Sidebar | `fixed top-14 bottom-0 left-0 w-60 bg-slate-50/80` | 固定左侧，宽度 15rem |
| Content | `ml-60 min-h-[calc(100vh-3.5rem)] p-6 bg-slate-50/40` | 右侧内容区 |
| Header Logo | `h-8 w-8 rounded-lg bg-primary` | 蓝色方块 + "AI" 文字 |
| Nav Item (active) | `bg-primary/10 text-primary font-medium` | 左侧蓝色指示条 |
| Nav Item (inactive) | `text-muted-foreground hover:bg-slate-100` | 灰色文字 |

### 布局结构

```
+--------------------------------------------------+
|  Header (h-14, fixed, z-30)                       |
+--------+-----------------------------------------+
| Sidebar | Content Area                            |
| (w-60)  | (ml-60, p-6)                           |
| fixed   | min-h-[calc(100vh-3.5rem)]             |
|         |                                         |
+---------+-----------------------------------------+
```

---

## 5. 页面清单与任务

### Task 1: Dashboard 工作台页面

**当前状态**：仅一个标题 + 一行描述 + 一个按钮，几乎空白
**文件**：`src/modules/dashboard/pages/DashboardPage.tsx`
**设计要点**：
- 统计卡片区（Agent 数量、知识库数量、对话数量、工作流执行数）
- 快捷操作区（新建 Agent、上传知识、开始对话）
- 最近活动列表
- 统计数据可用静态 mock，后续接入 API

### Task 2: Agent 列表页面

**当前状态**：纯文字列表，`<ul>` + `<li>`，无视觉层次
**文件**：`src/modules/agent/pages/AgentListPage.tsx`
**设计要点**：
- 卡片网格布局（grid-cols-1 md:grid-cols-2 lg:grid-cols-3）
- 每张卡片：Agent 名称、描述、创建时间、操作按钮
- 空状态：机器人图标 + 引导文案 + 创建按钮
- 顶部搜索/筛选栏 + 新建按钮

### Task 3: 知识库页面

**当前状态**：三个堆叠的表单区块，纯功能性布局
**文件**：`src/modules/knowledge/pages/KnowledgePage.tsx`
**设计要点**：
- 左右分栏：左侧知识库列表，右侧文档管理
- 知识库列表项可点击选中，高亮当前选中项
- 创建知识库改为模态框或抽屉
- 文档上传区域使用拖拽上传样式
- 文档状态用 Badge 展示

### Task 4: 聊天页面

**当前状态**：暴露 userId/agentId 调试输入框，消息无气泡样式，显示原始 role/status 文字
**文件**：`src/modules/chat/pages/ChatPage.tsx`
**设计要点**：
- 隐藏调试输入框（userId/agentId），从 context 或默认值获取
- 消息气泡样式：用户消息靠右蓝色，AI 消息靠左灰色
- 隐藏原始 status 文字，改用视觉指示（加载动画、错误图标）
- 会话列表美化：当前选中项高亮，时间格式化
- 输入区域：圆角输入框 + 发送按钮图标化
- 空状态：欢迎语 + 引导提示

### Task 5: 404 页面

**当前状态**：仅一行 "页面不存在" 文字
**文件**：`src/app/pages/NotFoundPage.tsx`
**设计要点**：
- 居中大号 "404" 数字
- 友好的提示文案
- 返回首页按钮
- 可选：简单的插图或图标

### Task 6: AppShell 布局微调

**当前状态**：已有较好的布局基础，需要微调细节
**文件**：`src/app/AppShell.tsx`
**设计要点**：
- Header 和 Sidebar 已有不错的基础样式
- 可能需要配合子页面调整 Content 区域的 padding/背景
- 确保整体视觉一致性

---

## 6. 关键文件路径

### 全局配置

| 文件 | 说明 |
|------|------|
| `ai-agent-foward/src/index.css` | CSS 变量定义（颜色、圆角） |
| `ai-agent-foward/tailwind.config.ts` | Tailwind 扩展配置 |
| `ai-agent-foward/src/lib/utils.ts` | cn() 工具函数 |
| `ai-agent-foward/src/app/auth.ts` | Token 管理（localStorage/sessionStorage） |
| `ai-agent-foward/src/app/router.tsx` | 路由配置 |
| `ai-agent-foward/src/app/AppShell.tsx` | 主布局（Header + Sidebar + Content） |

### 业务页面

| 文件 | 说明 |
|------|------|
| `ai-agent-foward/src/modules/dashboard/pages/DashboardPage.tsx` | 工作台 |
| `ai-agent-foward/src/modules/agent/pages/AgentListPage.tsx` | Agent 列表 |
| `ai-agent-foward/src/modules/knowledge/pages/KnowledgePage.tsx` | 知识库 |
| `ai-agent-foward/src/modules/chat/pages/ChatPage.tsx` | 聊天 |
| `ai-agent-foward/src/app/pages/NotFoundPage.tsx` | 404 页面 |

### API 服务

| 文件 | 说明 |
|------|------|
| `ai-agent-foward/src/modules/agent/api/agentService.ts` | Agent API |
| `ai-agent-foward/src/modules/knowledge/api/knowledgeService.ts` | 知识库 API |
| `ai-agent-foward/src/modules/chat/api/chatService.ts` | 聊天 API |

---

## 7. 当前页面问题总结

| 页面 | 问题 |
|------|------|
| Dashboard | 几乎空白，仅标题 + 描述 + 一个按钮，无任何数据展示 |
| Agent 列表 | 纯 `<ul>/<li>` 文字列表，无卡片、无图标、无视觉层次 |
| 知识库 | 三个表单区块垂直堆叠，像后台管理的原型页面 |
| 聊天 | 暴露 userId/agentId 调试输入框，消息无气泡，显示原始 role/status |
| 404 | 仅一行 "页面不存在" 文字，无任何设计 |
| AppShell | 布局基础较好，但子页面内容区缺乏统一的视觉规范 |

---

## 8. 测试要求

- 每个子任务修改后，必须运行 `npx vitest --run` 确保所有测试通过
- 当前测试数量：94 个
- 测试框架：Vitest + @testing-library/react + jsdom
- 测试配置：`ai-agent-foward/vite.config.ts`
- 运行命令：`cd ai-agent-foward && npx vitest --run`
- 注意：UI 美化仅修改样式和布局，不应破坏现有业务逻辑和测试

---

## 9. 实施原则

1. **仅改 UI，不改逻辑**：保持所有 state、effect、handler、API 调用不变
2. **渐进式修改**：每个页面独立一个任务，互不影响
3. **样式一致性**：严格遵循 3.4 节的组件样式规范
4. **使用 cn() 函数**：所有动态 class 合并使用 `cn()` 而非字符串拼接
5. **无新依赖**：不引入任何新的 npm 包
6. **测试不能挂**：每次修改后验证 94 个测试全部通过
