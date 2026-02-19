# AI Agent Platform - 后端 API 总览

## 系统概述
AI Agent Platform 提供智能体管理、工作流执行、知识库、对话管理等核心能力。基于 DDD 架构设计，支持动态创建智能体、条件分支工作流、向量检索知识库和实时流式响应。

## 认证机制
所有接口需要 JWT Token 认证：
- Header: `Authorization: Bearer {token}`
- 获取 Token: POST /api/user/login
- Token 有效期：24 小时（可配置）

## 通用响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": {...}
}
```

**错误码说明**：
- `200`: 成功
- `400`: 请求参数错误
- `401`: 未认证或 Token 失效
- `403`: 无权限访问
- `404`: 资源不存在
- `500`: 服务器内部错误

## API 模块

### 1. Agent 管理 (`/api/agent`)
智能体 CRUD、版本管理、发布回滚

**核心功能**：
- 创建/更新/删除智能体
- 工作流图配置（节点、边、条件分支）
- 版本管理（草稿、发布、回滚）
- 智能体列表查询

**详细文档**: [Agent API](./agent.md)

### 2. 用户管理 (`/api/user`)
注册、登录、用户信息

**核心功能**：
- 用户注册（邮箱验证）
- 登录认证（JWT Token）
- 用户信息查询/更新
- 密码修改

**详细文档**: [User API](./user-api.md)

### 3. 对话管理 (`/api/chat`)
创建对话、发送消息、历史记录

**核心功能**：
- 创建对话会话
- 发送消息（触发工作流执行）
- 查询对话历史
- 删除对话

**详细文档**: [Chat API](./chat.md)

### 4. 工作流执行 (`/api/workflow/execution`)
启动、监控、控制工作流

**核心功能**：
- 启动工作流执行
- **SSE 流式输出**: GET /api/workflow/execution/{executionId}/stream
- 暂停/恢复/取消执行
- 查询执行状态和历史

**详细文档**: [Workflow API](./workflow.md)

### 5. 人工审核 (`/api/workflow/reviews`)
审核任务查询、批准/拒绝

**核心功能**：
- 查询待审核任务
- 批准/拒绝审核
- 审核历史记录

**详细文档**: [Review API](./review.md)

### 6. 知识库 (`/api/knowledge`)
数据集管理、文档上传、向量检索

**核心功能**：
- 数据集 CRUD
- 文档上传（支持 TXT、PDF、Markdown）
- 文档分块和向量化
- 语义检索（基于 Milvus）

**详细文档**: [Knowledge API](./knowledge.md)

### 7. 元数据 (`/api/meta`)
节点类型、模板查询

**核心功能**：
- 查询可用节点类型（START、END、LLM、CONDITION、HTTP、TOOL）
- 获取节点配置模板
- 查询支持的 LLM 模型列表

**详细文档**: [Meta API](./meta.md)

### 8. 仪表盘 (`/api/dashboard`)
统计数据概览

**核心功能**：
- 智能体数量统计
- 工作流执行统计
- 对话数量统计
- 知识库容量统计

**详细文档**: [Dashboard API](./dashboard.md)

## 技术栈
- **后端框架**: Spring Boot 3.4.9 + Java 21
- **数据库**: MySQL 8.0 + Redis 7.0
- **向量数据库**: Milvus 2.3
- **对象存储**: MinIO
- **AI 集成**: Spring AI (OpenAI、Ollama)
- **实时通信**: SSE (Server-Sent Events)

## 开发环境
- **后端服务**: http://localhost:8080
- **前端服务**: http://localhost:5173
- **MySQL**: localhost:13306
- **Redis**: localhost:6379
- **Milvus**: localhost:19530
- **MinIO Console**: http://localhost:9001

## 快速开始

### 1. 用户注册和登录
```bash
# 注册
curl -X POST http://localhost:8080/api/user/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@example.com","password":"123456"}'

# 登录获取 Token
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"123456"}'
```

### 2. 创建智能体
```bash
curl -X POST http://localhost:8080/api/agent \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "客服助手",
    "description": "智能客服",
    "workflowGraph": {...}
  }'
```

### 3. 启动对话
```bash
# 创建对话
curl -X POST http://localhost:8080/api/chat/conversations \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"agentId":"agent-123","title":"测试对话"}'

# 发送消息
curl -X POST http://localhost:8080/api/chat/conversations/{conversationId}/messages \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"content":"你好"}'
```

### 4. 监听流式响应
```bash
curl -N http://localhost:8080/api/workflow/execution/{executionId}/stream \
  -H "Authorization: Bearer {token}"
```

## 详细文档索引
- [Agent API](./agent.md) - 智能体管理
- [User API](./user-api.md) - 用户管理
- [Chat API](./chat.md) - 对话管理
- [Workflow API](./workflow.md) - 工作流执行
- [Review API](./review.md) - 人工审核
- [Knowledge API](./knowledge.md) - 知识库管理
- [Meta API](./meta.md) - 元数据查询
- [Dashboard API](./dashboard.md) - 仪表盘统计

## 相关资源
- [系统架构文档](../../.blueprint/_overview.md)
- [工作流引擎设计](../../.blueprint/domain/workflow/WorkflowEngine.md)
- [前端开发指南](../frontend/README.md)
- [部署指南](../deployment/README.md)
