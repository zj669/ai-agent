# 前端开发业务域索引

本文件为前端开发业务域提供二级路由，不重复根级全局安全规则。

## 业务范围

React 19 + Vite 前端应用（`ai-agent-foward/`）：页面、组件、Hooks、状态管理（Zustand）、
工作流画布（@xyflow/react）、Ant Design UI、API 集成。

## 重要约定

- **活跃前端**：`ai-agent-foward/`
- **禁止修改**：`app/frontend/`（遗留骨架，无效果）

## 前端目录结构

```text
ai-agent-foward/src/
├── components/        # UI 组件
├── hooks/             # 业务逻辑 Hook
├── pages/             # 页面
├── services/          # API 请求
├── stores/            # Zustand 状态管理
├── styles/            # 全局样式
├── types/             # 类型定义
└── index.tsx          # 入口
```

## 关键依赖

| 依赖 | 版本 | 用途 |
|---|---|---|
| React | 19.2.1 | UI 框架 |
| Vite | 6.2.0 | 构建工具 |
| TypeScript | 5.8.2 | 类型检查 |
| Ant Design | 6.1.1 | UI 组件库 |
| @xyflow/react | 12.10.0 | 工作流画布 |
| Zustand | 5.0.9 | 状态管理 |
| Tailwind CSS | 4.1.18 | 样式 |
| Zod | 4.2.1 | Schema 验证 |
| react-hook-form | 7.69.0 | 表单管理 |

## 开发命令

```bash
cd ai-agent-foward
npm install
npm run dev      # http://localhost:5173
npm run build
npm run preview
```

## 验证要求

前端变更必须在 `http://localhost:5173` 真实浏览器验证：
1. 打开实际页面
2. 走一遍被修改的工作流
3. 检查 browser console 和 network tab
4. 布局变更时增加移动端视口检查

如果浏览器验证受阻，明确报告为未验证。

## SOP 列表

| SOP | 文件 | 状态 |
|---|---|---|
| 前端功能开发 | `references/frontend/feature-delivery.md` | 待创建 |
| 工作流画布开发 | `references/frontend/workflow-canvas.md` | 待创建 |
