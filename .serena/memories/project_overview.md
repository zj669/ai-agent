# 项目概览

- 项目名：ai-agent（Maven 多模块后端 + React 前端）
- 目标：构建企业级 AI 智能体编排平台，覆盖工作流编排、对话、知识库、人工审核。
- 后端架构：DDD + 六边形/端口适配思想。
- 前端架构：React + TypeScript + Feature-Based 目录。

## 后端模块
- ai-agent-shared：共享常量、上下文、响应模型、工具类。
- ai-agent-domain：领域模型与端口（agent/auth/chat/dashboard/knowledge/memory/user/workflow）。
- ai-agent-application：应用编排服务（agent/chat/dashboard/knowledge/user/workflow）。
- ai-agent-infrastructure：端口实现（持久化、Redis、Milvus、MinIO、JWT、工作流执行器等）。
- ai-agent-interfaces：Spring Boot 启动与控制器入口。

## 前端
- 目录：ai-agent-foward/src
- 子目录：components/pages/services/hooks/stores/styles/types/utils。
- 路由：登录、仪表盘、智能体、工作流、聊天、知识库、人工审核等页面。

## 核心工作流（蓝图）
- SchedulerService 负责执行编排、节点调度、记忆水合、人工审核暂停/恢复、流式推送协同。
- Execution 是领域聚合根，管理执行状态机、节点推进、分支剪枝、检查点。
- NodeExecutorStrategy 在基础设施层有 START/END/LLM/CONDITION/HTTP/TOOL 实现。
- Redis + SSE 负责流式输出；Redis/MySQL 协同存储执行状态与日志；Milvus 支持向量检索。