# ai-agent-foward 前端模块蓝图

## 模块职责
React 19 前端应用，提供 AI Agent 平台的用户界面。采用模块化架构，每个业务域独立为一个 module，共享层提供 API 适配、主题和反馈组件。

## 技术栈
- React 19 + TypeScript 5.8 + Vite 6
- Ant Design 6（UI 组件库）
- @xyflow/react 12（工作流画布）
- Zustand 5（状态管理）
- Tailwind CSS 4（样式）
- Zod 4 + react-hook-form 7（表单验证）
- Axios（HTTP 客户端）

## 目录结构

| 目录 | 职责 |
|------|------|
| `src/app/` | 应用壳：路由、布局页面（Login/Register/Dashboard/NotFound）、全局组件 |
| `src/modules/agent/` | Agent 管理模块：列表、创建、详情页面 |
| `src/modules/workflow/` | **工作流编辑器**：@xyflow 画布、节点组件、图验证、Zustand store |
| `src/modules/chat/` | 对话模块：聊天页面、消息流 |
| `src/modules/knowledge/` | 知识库模块：数据集/文档管理 |
| `src/modules/swarm/` | Swarm 多智能体模块：聊天、侧边栏、工作区组件 |
| `src/modules/auth/` | 认证模块 |
| `src/modules/dashboard/` | 仪表盘模块 |
| `src/modules/review/` | 人工审核模块 |
| `src/modules/llm-config/` | LLM 模型配置模块 |
| `src/modules/settings/` | 设置模块 |
| `src/shared/api/` | **API 适配层**：httpClient、errorMapper、各域 adapter（agent/auth/chat/knowledge/metadata/review/dashboard） |
| `src/shared/feedback/` | 全局反馈：toast 通知 |
| `src/shared/theme/` | 主题配置 |
| `src/lib/` | 第三方库封装 |

## 核心模块说明

### workflow（最复杂）
- `components/WorkflowNode.tsx` — 画布节点渲染（支持 7 种类型：START/END/LLM/CONDITION/TOOL/HTTP/KNOWLEDGE）
- `components/FieldRenderer.tsx` — **数据库驱动的动态配置面板**，根据后端 `/api/meta/node-templates` 返回的 `configFieldGroups` 动态渲染控件（支持 text/textarea/select/number/switch/boolean/llm_config_select/knowledge_select 等 fieldType）
- `components/CanvasToolbar.tsx` — 画布工具栏（节点类型拖拽添加）
- `components/NodeSelector.tsx` — 节点类型选择器
- `stores/` — Zustand 状态管理（图数据、节点选中、编辑状态）
- `validation/` — 图结构验证逻辑
- `api/` — 工作流相关 API 调用

### shared/api（全局共享）
- `httpClient.ts` — Axios 实例封装，统一拦截器
- `client.ts` — API 客户端
- `errorMapper.ts` — 后端错误码映射
- `response.ts` — 响应类型定义
- `adapters/` — 各域 API 适配器，封装具体接口调用

## 上下游依赖
- 上游：通过 REST API 调用 `ai-agent-interfaces` 后端
- 下游：无（最终用户界面）
