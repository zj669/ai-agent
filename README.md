# AI Agent Platform

> 基于 Spring Boot 和 Spring AI 构建的企业级 AI 智能体编排平台

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.9-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19.2.0-blue.svg)](https://reactjs.org/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

## 🚀 快速开始

### 前置要求

- Java 21+
- Node.js 16+
- Docker & Docker Compose
- Maven 3.8+

### 启动服务

```bash
# 1. 启动依赖服务（MySQL, Redis, Milvus, MinIO）
cd ai-agent-infrastructure/src/main/resources/docker
docker-compose up -d

# 2. 启动后端
mvn spring-boot:run -pl ai-agent-interfaces -Dspring-boot.run.profiles=local

# 3. 启动前端
cd app/frontend
npm install
npm run dev
```

访问：
- 前端：http://localhost:5173
- 后端 API：http://localhost:8080
- API 文档：http://localhost:8080/swagger-ui.html

## 📖 文档

- **[📚 完整文档索引](DOCUMENTATION_INDEX.md)** - 所有文档的导航
- **[🏭 开发环境配置](.kiro/README.md)** - Kiro 工业级开发环境
- **[📋 产品概述](.kiro/steering/product.md)** - 核心功能和特性
- **[🔧 技术栈](.kiro/steering/tech.md)** - 技术选型和构建指南
- **[🏗️ 项目结构](.kiro/steering/structure.md)** - DDD 架构设计
- **[📡 API 文档](app/APIDocumentation.md)** - REST API 完整文档

## ✨ 核心功能

### 🔄 工作流编排
- DAG 执行引擎
- 可视化拖拽设计
- 多节点类型支持

### 🤖 AI 智能体管理
- 动态模型配置
- 多模型支持
- Agent 生命周期管理

### 📚 知识库系统
- 文档管理和解析
- 向量检索（Milvus）
- RAG 集成

### 💬 对话系统
- 流式响应（SSE）
- 上下文管理
- 可视化追踪

### 👥 人工审核
- 暂停/恢复机制
- 审核队列
- 审批流程

## 🏗️ 架构

### 后端架构（DDD + 六边形架构）

```
┌─────────────────────────────────────────┐
│         Interfaces Layer                │  REST API, WebSocket
│    (ai-agent-interfaces)                │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│       Application Layer                 │  用例编排, DTO
│    (ai-agent-application)               │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│         Domain Layer                    │  核心业务逻辑
│      (ai-agent-domain)                  │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│     Infrastructure Layer                │  数据库, 缓存
│   (ai-agent-infrastructure)             │
└─────────────────────────────────────────┘
```

### 前端架构（Feature-Based）

```
src/
├── app/                    # 应用层（路由）
├── features/               # 功能模块
│   ├── auth/              # 认证
│   ├── agent/             # Agent 管理
│   ├── orchestration/     # 工作流编排
│   ├── chat/              # 对话
│   └── knowledge/         # 知识库
└── shared/                # 共享组件
```

## 🛠️ 技术栈

### 后端
- **框架**: Spring Boot 3.4.9, Spring AI 1.0.1
- **数据库**: MySQL 8.0, Redis 7.x, Milvus 2.3
- **存储**: MinIO
- **ORM**: MyBatis Plus 3.5.5
- **认证**: JWT 0.12.3

### 前端
- **框架**: React 19.2.0, TypeScript 5.9.3
- **构建**: Vite 7.2.4
- **样式**: Tailwind CSS 3.4.17
- **工作流**: React Flow 11.11.4
- **路由**: React Router DOM 7.12.0

## 📊 项目状态

- ✅ 核心功能完成
- ✅ 工作流引擎稳定
- ✅ 知识库集成完成
- 🚧 性能优化进行中
- 🚧 测试覆盖率提升中

## 🤝 贡献

欢迎贡献！请查看 [贡献指南](CONTRIBUTING.md)。

## 📝 开发规范

本项目使用工业级开发规范：

- **代码质量**: [代码质量规范](.kiro/steering/code-quality.md)
- **数据库设计**: [数据库规范](.kiro/steering/database-standards.md)
- **架构原则**: [项目结构](.kiro/steering/structure.md)
- **自动化检查**: [Hooks 配置](.kiro/hooks/)

## 🔗 相关链接

- [在线演示](https://demo.ai-agent-platform.com)
- [问题反馈](https://github.com/your-org/ai-agent/issues)
- [更新日志](CHANGELOG.md)

## 📄 许可证

[MIT License](LICENSE)

## 👥 团队

由 AI Agent Platform Team 开发和维护。

---

**最后更新**: 2026-02-03
