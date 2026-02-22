# AI Agent Platform - 开发计划

## 目标
将平台打造成类似 Dify 的 AI Agent 编排平台，核心差异化功能：人工审核节点。
要求：几天内可演示，功能完整，UI 现代化。

## 当前状态
- 后端：DDD 架构完整，工作流引擎基本可用，但 LLM 调用、人工审核流程未完全跑通
- 前端：6 个模块骨架在，但 UI 粗糙，聊天页 userId/agentId 硬编码，缺少 Agent 选择器
- 基础设施：MySQL/Redis/Milvus/MinIO Docker 容器已就绪

## LLM 配置
- 使用 Claude Opus 4.6，通过本地代理 http://127.0.0.1:8084 访问
- 这是一个 OpenAI 兼容接口（aether 代理）
- Embedding 使用阿里云 DashScope text-embedding-v4

## 优先级排序
1. **P0 - 核心流程端到端跑通**
2. **P1 - 人工审核流程**
3. **P2 - UI 全面升级**
4. **P3 - 知识库完善**

## 技术栈
- 后端: Java 21, Spring Boot 3.4.9, Spring AI 1.0.1, MyBatis Plus
- 前端: React 19, TypeScript, Vite, Tailwind CSS, @xyflow/react, Ant Design 6, Zustand
- 数据库: MySQL 8.0 (13306), Redis 7 (16379), Milvus 2.3 (19530), MinIO (9000)
