# AI Agent Platform 蓝图

## 项目概述
- 类型：AI Agent 编排平台（SaaS）
- 技术栈：Spring Boot 3.4.9 + Java 21（后端）、React 19 + Vite 6 + TypeScript（前端）
- 核心职责：动态创建 AI Agent、工作流编排与执行（条件分支/人工审核）、知识库管理（向量检索）、实时 SSE 流式推送
- 架构风格：DDD 分层架构（interfaces → application → domain ← infrastructure），Ports & Adapters 模式

## 模块地图

| 模块 | 路径 | 职责 |
|------|------|------|
| **ai-agent-shared** | `ai-agent-shared/` | 公共工具、设计模式（DAG/策略树/工作流状态机）、常量、响应体 |
| **ai-agent-domain** | `ai-agent-domain/` | 核心业务逻辑：实体、值对象、领域服务、仓储接口、端口定义 |
| **ai-agent-application** | `ai-agent-application/` | 用例编排：应用服务、DTO、命令、事件监听、跨域协调 |
| **ai-agent-infrastructure** | `ai-agent-infrastructure/` | 技术实现：MyBatis Plus 持久化、Redis/Milvus/MinIO 适配器、工作流执行器、SSE 流 |
| **ai-agent-interfaces** | `ai-agent-interfaces/` | REST 控制器、WebSocket、Spring 配置、应用入口、拦截器 |
| **ai-agent-foward** | `ai-agent-foward/` | React 前端：模块化页面、API 适配层、Zustand 状态管理、@xyflow 工作流画布 |

## 依赖关系

```
interfaces ──→ application ──→ domain ←── infrastructure
                                 ↑
                              shared
```

前端 `ai-agent-foward` 通过 REST API 与后端 `ai-agent-interfaces` 通信。

## 核心模块（建议优先 explore）

1. **ai-agent-domain/workflow** — 工作流执行引擎，Execution 聚合根，条件分支/剪枝/人工审核
2. **ai-agent-infrastructure/workflow** — 工作流执行器策略实现（Start/End/LLM/Condition/Http/Tool/Knowledge 共 7 种）
3. **ai-agent-application/workflow** — SchedulerService 调度编排，串联执行生命周期
4. **ai-agent-domain/swarm** — Swarm 多智能体协作领域模型
5. **ai-agent-foward/modules/workflow** — 前端工作流画布编辑器

## 领域边界

- **workflow**: 工作流执行引擎（可依赖 agent 读取图定义、knowledge 做检索）
- **agent**: Agent 管理（独立，不依赖其他业务域）
- **chat**: 对话管理（独立）
- **knowledge**: 知识库（独立）
- **user/auth**: 用户认证（独立）
- **swarm**: 多智能体协作（可依赖 agent、llm）
- **llm**: LLM 模型配置管理（独立）
- **dashboard**: 统计面板（只读聚合）

## 蓝图使用说明

- 熟悉代码上下文时，使用 `/claude-shadow-context:explore` 先通过蓝图理解职责和边界
- 代码改动后，使用 `/claude-shadow-context:align` 检查蓝图是否仍然对齐
- 文件级蓝图由 align 在会话结束时按需沉淀，init 只负责根蓝图和模块入口蓝图
