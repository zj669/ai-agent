# AI Agent Platform - 文档索引

本文档提供项目所有文档的完整索引和导航。

## 📚 核心文档

### 项目概述
- **[README.md](.kiro/README.md)** - 项目介绍和快速开始
- **[INDUSTRIAL_DEV_GUIDE.md](.kiro/INDUSTRIAL_DEV_GUIDE.md)** - 工业级开发指南
- **[QUICK_REFERENCE.md](.kiro/QUICK_REFERENCE.md)** - 快速参考手册

### 产品与架构
- **[产品概述](.kiro/steering/product.md)** - 产品功能和目标用户
- **[技术栈](.kiro/steering/tech.md)** - 技术选型和构建指南
- **[项目结构](.kiro/steering/structure.md)** - DDD 架构和分层设计

## 🔧 开发规范

### 代码质量
- **[代码质量规范](.kiro/steering/code-quality.md)** - 编码标准和最佳实践
- **[数据库设计规范](.kiro/steering/database-standards.md)** - Schema 设计和迁移规范
- **[命令使用限制](.kiro/steering/command-restrictions.md)** - Windows 环境命令规范

### 架构原则
- **[CodeReuse.md](C:/Users/32183/.kiro/steering/CodeReuse.md)** - 架构一致性与复用优先（全局规则）

## 📖 API 文档

### 后端 API
- **[API Documentation](app/APIDocumentation.md)** - REST API 完整文档
- **[数据库 Schema](ai-agent-infrastructure/src/main/resources/db/ai_agent.sql)** - 数据库表结构

### WebSocket
- **[WebSocket Setup](WEBSOCKET_SETUP.md)** - WebSocket 配置和使用

## 🎨 前端设计

### 设计文档
- **[系统概览](app/design/00_system_overview.md)** - 整体设计概览
- **[登录页面](app/design/01_login.md)** - 登录功能设计
- **[仪表盘](app/design/02_dashboard_page.md)** - Dashboard 设计
- **[编排画布](app/design/03_orchestration_canvas.md)** - 工作流编排界面
- **[对话界面](app/design/04_chat_interface.md)** - 聊天功能设计
- **[知识库](app/design/05_knowledge_base.md)** - 知识库管理
- **[人工审核](app/design/06_human_review.md)** - 审核流程设计
- **[用户设置](app/design/07_user_settings.md)** - 设置页面设计
- **[消息重设计](app/design/08_chat_message_redesign.md)** - 消息组件优化

### 前端规范
- **[代码标准](app/frontend/design/CODE_STANDARDS.md)** - 前端编码规范
- **[字段类型](app/frontend/design/FIELD_TYPES.md)** - 表单字段类型定义

## 🔄 Specs（需求和设计）

### Redis 重构
- **[重构计划](.kiro/specs/redis-refactoring/REFACTORING_PLAN.md)** - Redis 重构总体计划
- **[需求文档](.kiro/specs/redis-refactoring/requirements.md)** - 重构需求
- **[设计文档](.kiro/specs/redis-refactoring/design.md)** - 重构设计方案
- **[任务列表](.kiro/specs/redis-refactoring/tasks.md)** - 实施任务
- **[回滚方案](.kiro/specs/redis-refactoring/reverse.md)** - 回滚策略
- **[DDD 原则](.kiro/specs/redis-refactoring/DDD_PRINCIPLES.md)** - 领域驱动设计原则

### 工作流消息持久化修复
- **[需求文档](.kiro/specs/fix-workflow-message-persistence/requirements.md)** - 问题描述和需求
- **[设计文档](.kiro/specs/fix-workflow-message-persistence/design.md)** - 解决方案设计
- **[任务列表](.kiro/specs/fix-workflow-message-persistence/tasks.md)** - 实施任务
- **[后端修复](.kiro/specs/fix-workflow-message-persistence/BACKEND_RENDERMODE_FIX.md)** - 后端实现细节
- **[实施总结](.kiro/specs/fix-workflow-message-persistence/IMPLEMENTATION_SUMMARY.md)** - 完成情况总结

### 其他 Specs
- **[移除 JPA 组件](.kiro/specs/remove-jpa-components/)** - JPA 移除方案
- **[工作流消息持久化简化](.kiro/specs/workflow-message-persistence-simplified/)** - 简化方案

## 🤖 Agent Skills & Powers

### Skills 管理
- **[Agent Skills Guide](powers/agent-skills-guide/POWER.md)** - Skills 使用完整指南
- **[Skills 转换工作流](powers/agent-skills-guide/steering/skill-to-power-conversion.md)** - 将 Skills 转换为 Powers
- **[Skills 发现指南](.kiro/steering/find-skills-guide.md)** - 如何搜索和安装 Skills
- **[Skill 转换指南](.kiro/powers/skill-converter-guide.md)** - 转换参考文档

### 已安装 Skills
- **[academic-research-writer](.kiro/skills/academic-research-writer/)** - 学术论文写作
- **[research-paper-writer](.kiro/skills/research-paper-writer/)** - 研究论文生成
- **[find-skills](.kiro/skills/find-skills/)** - Skills 搜索工具

## ⚙️ Hooks（自动化）

### 代码质量 Hooks
- **[代码质量检查](.kiro/hooks/code-quality-check.json)** - 自动代码审查
- **[Java 文件保存检查](.kiro/hooks/java-file-save-check.json)** - Java 文件保存时检查
- **[SQL 文件审查](.kiro/hooks/sql-file-review.json)** - SQL 文件审查

### Skills 自动化
- **[自动发现 Skills](.kiro/hooks/auto-discover-skills.kiro.hook)** - 根据需求自动推荐 Skills

## 🚀 部署文档

### 部署脚本
- **[cd-deploy.sh](cd-deploy.sh)** - 标准部署脚本
- **[cd-deploy-rolling.sh](cd-deploy-rolling.sh)** - 滚动部署脚本
- **[Dockerfile](Dockerfile)** - Docker 镜像构建

### 前端部署
- **[前端部署脚本](app/cd-deploy.sh)** - 前端部署
- **[前端滚动部署](app/cd-deploy-rolling.sh)** - 前端滚动部署

## 📝 其他文档

### 测试与状态
- **[基线测试状态](BASELINE_TEST_STATUS.md)** - 测试基线记录
- **[编译日志](compile_log_utf8.txt)** - 编译输出日志

### AI 助手配置
- **[Claude 配置](CLAUDE.md)** - Claude AI 使用说明

### 工具脚本
- **[文件操作脚本](.kiro/scripts/file_operations.py)** - UTF-8 文件操作工具

## 📂 文档组织结构

```
ai-agent/
├── .kiro/                          # Kiro 配置和文档
│   ├── steering/                   # 开发指导文档
│   ├── specs/                      # 需求和设计文档
│   ├── hooks/                      # 自动化 Hooks
│   ├── powers/                     # Powers 相关
│   ├── skills/                     # 已安装 Skills
│   └── scripts/                    # 工具脚本
├── app/                            # 前端应用
│   ├── design/                     # UI/UX 设计文档
│   └── frontend/design/            # 前端开发规范
├── powers/                         # 自定义 Powers
│   └── agent-skills-guide/         # Skills 使用指南
├── ai-agent-*/                     # 后端模块（代码）
└── [根目录文档]                    # 项目级文档
```

## 🔍 快速查找

### 按角色查找

**产品经理：**
- [产品概述](.kiro/steering/product.md)
- [API 文档](app/APIDocumentation.md)
- [设计文档](app/design/)

**后端开发：**
- [技术栈](.kiro/steering/tech.md)
- [项目结构](.kiro/steering/structure.md)
- [代码质量规范](.kiro/steering/code-quality.md)
- [数据库规范](.kiro/steering/database-standards.md)

**前端开发：**
- [前端代码标准](app/frontend/design/CODE_STANDARDS.md)
- [设计文档](app/design/)
- [API 文档](app/APIDocumentation.md)

**DevOps：**
- [部署脚本](cd-deploy.sh)
- [Docker 配置](Dockerfile)
- [技术栈](.kiro/steering/tech.md)

**架构师：**
- [项目结构](.kiro/steering/structure.md)
- [DDD 原则](.kiro/specs/redis-refactoring/DDD_PRINCIPLES.md)
- [架构复用原则](C:/Users/32183/.kiro/steering/CodeReuse.md)

### 按任务查找

**开始新功能开发：**
1. 阅读 [产品概述](.kiro/steering/product.md)
2. 查看 [项目结构](.kiro/steering/structure.md)
3. 遵循 [代码质量规范](.kiro/steering/code-quality.md)

**数据库变更：**
1. 查看 [数据库规范](.kiro/steering/database-standards.md)
2. 检查 [Schema](ai-agent-infrastructure/src/main/resources/db/ai_agent.sql)
3. 创建迁移文件

**前端开发：**
1. 查看 [设计文档](app/design/)
2. 遵循 [代码标准](app/frontend/design/CODE_STANDARDS.md)
3. 参考 [API 文档](app/APIDocumentation.md)

**使用 Agent Skills：**
1. 阅读 [Agent Skills Guide](powers/agent-skills-guide/POWER.md)
2. 查看 [已安装 Skills](.kiro/skills/)
3. 参考 [转换指南](powers/agent-skills-guide/steering/skill-to-power-conversion.md)

## 📌 重要提示

### 文档更新规范
- 所有架构变更必须更新相关文档
- 新增功能需要更新 API 文档
- 数据库变更需要更新 Schema 文档
- 重要决策记录在 Specs 中

### 文档维护
- 定期检查文档的准确性
- 及时更新过时的信息
- 保持文档结构清晰
- 使用中文编写文档（代码除外）

### 获取帮助
- 查看 [快速参考](.kiro/QUICK_REFERENCE.md)
- 阅读 [工业级开发指南](.kiro/INDUSTRIAL_DEV_GUIDE.md)
- 使用 Agent Skills 获取专业领域帮助

---

**最后更新：** 2026-02-03
**维护者：** AI Agent Platform Team
