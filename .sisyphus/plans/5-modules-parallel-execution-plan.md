# AI Agent Platform - 5个核心模块并行验证执行计划

> **创建时间**: 2026-02-10  
> **状态**: 🚀 执行中  
> **决策**: A, A, A, A（全部推荐选项）

---

## 📋 执行摘要

基于用户决策，本计划将：
1. **立即修复所有 P0 数据库表**（Wave 0）
2. **并行验证3个无阻塞模块**（Wave 1）
3. **实现缺失功能并验证**（Wave 2）
4. **验证高级功能**（Wave 3）

**预计总时间**: 4-5 小时（考虑并行执行）

---

## 🎯 关键发现总结

### 后端实现现状
- ✅ **Workflow Editor**: 完整（AgentController + WorkflowController，SSE 流式）
- ✅ **Knowledge**: 完整（知识库 + 文档 + 向量检索）
- ✅ **Human Review**: 完整（审核队列 + 审批流程）
- ⚠️ **Chat**: 部分实现（消息发送通过 Workflow API）
- ❌ **Dashboard**: 完全缺失（无 DashboardController）

### 前端实现现状
- ✅ **Workflow Editor**: 完整（React Flow 可视化编辑器）
- ✅ **Knowledge**: 完整（知识库管理 + 文档上传）
- ✅ **Chat**: 完整（会话管理 + SSE 流式消息）
- ⚠️ **Dashboard**: 部分实现（UI 完整，使用 Mock 数据）
- ❌ **Human Review**: 完全缺失（无审核页面）

### 数据库关键问题
- ❌ `workflow_execution` 表缺失（阻塞 Workflow、Dashboard、Human Review）
- ❌ `knowledge_chunk` 表缺失（阻塞 Knowledge 向量检索）
- ❌ `knowledge_vector_index` 表缺失（阻塞向量检索）
- ❌ `workflow_human_review_task` 表缺失（阻塞 Human Review）
- ❌ `conversations` 表数据类型不一致（user_id/agent_id 应为 bigint）

---

## 🌊 Wave 0: 数据库修复（前置条件）

**目标**: 修复所有 P0 关键表，为后续验证扫清障碍

### 任务 0.1: 创建 workflow_execution 表

**优先级**: 🔴 P0（阻塞多个模块）

**SQL 定义**:
```sql
CREATE TABLE `workflow_execution` (
  `execution_id` varchar(36) NOT NULL COMMENT '执行ID (UUID)',
  `agent_id` bigint NOT NULL COMMENT 'Agent ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `conversation_id` varchar(36) NULL COMMENT '关联的会话ID',
  `status` varchar(20) NOT NULL DEFAULT 'RUNNING' COMMENT '执行状态: RUNNING, COMPLETED, FAILED, CANCELLED',
  `mode` varchar(20) NOT NULL DEFAULT 'STANDARD' COMMENT '执行模式: STANDARD, DEBUG, DRY_RUN',
  `input_data` json NULL COMMENT '输入数据',
  `output_data` json NULL COMMENT '输出数据',
  `error_message` text NULL COMMENT '错误信息',
  `started_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `completed_at` datetime NULL COMMENT '完成时间',
  `duration_ms` bigint NULL COMMENT '执行时长(毫秒)',
  PRIMARY KEY (`execution_id`),
  INDEX `idx_agent_id`(`agent_id`),
  INDEX `idx_user_id`(`user_id`),
  INDEX `idx_conversation_id`(`conversation_id`),
  INDEX `idx_status`(`status`),
  INDEX `idx_started_at`(`started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流执行主表';
```

**执行方式**: 直接修改 `ai_agent.sql` 并执行

**验证**: 
```sql
SHOW CREATE TABLE workflow_execution;
SELECT COUNT(*) FROM workflow_execution;
```

---

### 任务 0.2: 创建 knowledge_chunk 表

**优先级**: 🔴 P0（阻塞 Knowledge 向量检索）

**SQL 定义**:
```sql
CREATE TABLE `knowledge_chunk` (
  `chunk_id` varchar(36) NOT NULL COMMENT '分块ID (UUID)',
  `document_id` varchar(36) NOT NULL COMMENT '所属文档ID',
  `dataset_id` varchar(36) NOT NULL COMMENT '所属知识库ID',
  `content` text NOT NULL COMMENT '分块内容',
  `chunk_index` int NOT NULL COMMENT '分块序号',
  `token_count` int NULL COMMENT 'Token 数量',
  `metadata` json NULL COMMENT '元数据',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`chunk_id`),
  INDEX `idx_document_id`(`document_id`),
  INDEX `idx_dataset_id`(`dataset_id`),
  CONSTRAINT `fk_chunk_document` FOREIGN KEY (`document_id`) REFERENCES `knowledge_document` (`document_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识分块表';
```

**执行方式**: 直接修改 `ai_agent.sql` 并执行

**验证**: 
```sql
SHOW CREATE TABLE knowledge_chunk;
```

---

### 任务 0.3: 创建 knowledge_vector_index 表

**优先级**: 🔴 P0（阻塞向量检索）

**SQL 定义**:
```sql
CREATE TABLE `knowledge_vector_index` (
  `vector_id` varchar(36) NOT NULL COMMENT '向量ID (UUID)',
  `chunk_id` varchar(36) NOT NULL COMMENT '关联的分块ID',
  `milvus_id` bigint NOT NULL COMMENT 'Milvus 向量ID',
  `collection_name` varchar(100) NOT NULL COMMENT 'Milvus Collection 名称',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`vector_id`),
  UNIQUE INDEX `uk_chunk_id`(`chunk_id`),
  INDEX `idx_milvus_id`(`milvus_id`),
  CONSTRAINT `fk_vector_chunk` FOREIGN KEY (`chunk_id`) REFERENCES `knowledge_chunk` (`chunk_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='向量索引映射表';
```

**执行方式**: 直接修改 `ai_agent.sql` 并执行

**验证**: 
```sql
SHOW CREATE TABLE knowledge_vector_index;
```

---

### 任务 0.4: 创建 workflow_human_review_task 表

**优先级**: 🔴 P0（阻塞 Human Review）

**SQL 定义**:
```sql
CREATE TABLE `workflow_human_review_task` (
  `task_id` varchar(36) NOT NULL COMMENT '任务ID (UUID)',
  `execution_id` varchar(36) NOT NULL COMMENT '工作流执行ID',
  `node_id` varchar(50) NOT NULL COMMENT '节点ID',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态: PENDING, APPROVED, REJECTED',
  `input_data` json NULL COMMENT '待审核的输入数据',
  `output_data` json NULL COMMENT '待审核的输出数据',
  `reviewer_id` bigint NULL COMMENT '审核人ID',
  `review_comment` text NULL COMMENT '审核意见',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `reviewed_at` datetime NULL COMMENT '审核时间',
  PRIMARY KEY (`task_id`),
  INDEX `idx_execution_id`(`execution_id`),
  INDEX `idx_status`(`status`),
  INDEX `idx_reviewer_id`(`reviewer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人工审核任务表';
```

**执行方式**: 直接修改 `ai_agent.sql` 并执行

**验证**: 
```sql
SHOW CREATE TABLE workflow_human_review_task;
```

---

### 任务 0.5: 修复 conversations 表数据类型

**优先级**: 🟡 P1（数据一致性）

**SQL 修改**:
```sql
ALTER TABLE `conversations` 
  MODIFY COLUMN `user_id` bigint NOT NULL COMMENT '用户ID',
  MODIFY COLUMN `agent_id` bigint NOT NULL COMMENT '智能体ID';

-- 添加外键约束
ALTER TABLE `conversations`
  ADD CONSTRAINT `fk_conversation_user` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_conversation_agent` FOREIGN KEY (`agent_id`) REFERENCES `agent_info` (`id`) ON DELETE CASCADE;
```

**注意**: 需要先清空 conversations 表数据（如果有）

**验证**: 
```sql
SHOW CREATE TABLE conversations;
```

---

### Wave 0 执行方式

**Agent 配置**:
```typescript
delegate_task({
  category: "quick",
  load_skills: [],
  description: "修复数据库表结构",
  prompt: "执行 Wave 0 的所有数据库修复任务...",
  run_in_background: false
})
```

**预计时间**: 30 分钟

**成功标准**:
- ✅ 所有 P0 表创建成功
- ✅ 外键约束建立成功
- ✅ 数据类型修复完成

---

## 🌊 Wave 1: 并行验证无阻塞模块

**目标**: 并行验证3个无需修复即可验证的模块

### 任务 1.1: 验证 Workflow Editor 基础功能

**优先级**: 🔴 P0

**验证内容**:
1. **后端接口验证**
   - `GET /api/agents/{id}` - 获取 Agent 详情
   - `PUT /api/agents/{id}` - 更新工作流图
   - `POST /api/agents/{id}/publish` - 发布工作流
   - `POST /api/agents/{id}/rollback` - 回滚版本

2. **前端功能验证**
   - 工作流编辑器加载
   - 节点拖拽和连线
   - 节点属性配置
   - 工作流保存和发布

3. **前后端联调**
   - 保存工作流图到后端
   - 加载已保存的工作流
   - 发布和版本管理

**Agent 配置**:
```typescript
delegate_task({
  category: "visual-engineering",
  load_skills: ["frontend-ui-ux"],
  description: "验证 Workflow Editor 模块",
  prompt: "验证工作流编辑器的完整功能...",
  run_in_background: true
})
```

**预计时间**: 45 分钟

**成功标准**:
- ✅ 所有后端接口返回正确
- ✅ 前端编辑器功能正常
- ✅ 工作流保存和加载无误

---

### 任务 1.2: 验证 Chat 基础功能

**优先级**: 🔴 P0

**验证内容**:
1. **后端接口验证**
   - `GET /api/chat/conversations` - 获取会话列表
   - `POST /api/chat/conversations` - 创建会话
   - `GET /api/chat/conversations/{id}/messages` - 获取消息历史
   - `DELETE /api/chat/conversations/{id}` - 删除会话

2. **前端功能验证**
   - 会话列表显示
   - 创建新会话
   - 消息历史加载
   - 会话删除

3. **前后端联调**
   - 会话 CRUD 完整流程
   - 消息历史分页加载

**Agent 配置**:
```typescript
delegate_task({
  category: "quick",
  load_skills: [],
  description: "验证 Chat 基础功能",
  prompt: "验证聊天模块的会话管理功能...",
  run_in_background: true
})
```

**预计时间**: 30 分钟

**成功标准**:
- ✅ 会话 CRUD 功能正常
- ✅ 消息历史加载正确
- ✅ 前后端数据一致

---

### 任务 1.3: 验证 Knowledge 基础功能

**优先级**: 🔴 P0

**验证内容**:
1. **后端接口验证**
   - `GET /api/knowledge/datasets` - 获取知识库列表
   - `POST /api/knowledge/datasets` - 创建知识库
   - `POST /api/knowledge/datasets/{id}/documents` - 上传文档
   - `DELETE /api/knowledge/datasets/{id}` - 删除知识库

2. **前端功能验证**
   - 知识库列表显示
   - 创建知识库
   - 文档上传（MinIO）
   - 知识库删除

3. **前后端联调**
   - 知识库 CRUD 完整流程
   - 文件上传到 MinIO
   - 文档列表显示

**Agent 配置**:
```typescript
delegate_task({
  category: "quick",
  load_skills: [],
  description: "验证 Knowledge 基础功能",
  prompt: "验证知识库模块的基础功能...",
  run_in_background: true
})
```

**预计时间**: 30 分钟

**成功标准**:
- ✅ 知识库 CRUD 功能正常
- ✅ 文档上传到 MinIO 成功
- ✅ 文档列表显示正确

---

### Wave 1 并行执行

**并行度**: 3个任务同时执行

**预计总时间**: 45 分钟（最长任务的时间）

---

## 🌊 Wave 2: 实现缺失功能并验证

**目标**: 实现 Dashboard 和 Human Review 的缺失部分

### 任务 2.1: 实现 DashboardController 并验证

**优先级**: 🔴 P0

**实现内容**:
1. **后端开发**
   - 创建 `DashboardController`
   - 实现 `GET /api/dashboard/stats` 接口
   - 实现统计查询逻辑（Agent、Workflow、Chat、Knowledge）
   - 添加 Redis 缓存（5分钟 TTL）

2. **前端修改**
   - 修改 `dashboardService.ts`，替换 Mock 数据
   - 调用真实 API
   - 验证数据显示

3. **验证**
   - 统计数据准确性
   - 缓存机制生效
   - 自动刷新功能

**Agent 配置**:
```typescript
delegate_task({
  category: "unspecified-high",
  load_skills: [],
  description: "实现 DashboardController",
  prompt: "实现 Dashboard 统计接口并验证...",
  run_in_background: true
})
```

**预计时间**: 90 分钟

**成功标准**:
- ✅ DashboardController 实现完整
- ✅ 统计数据准确
- ✅ 前端显示真实数据

---

### 任务 2.2: 实现 Human Review 前端页面并验证

**优先级**: 🔴 P0

**实现内容**:
1. **前端开发**
   - 创建 `HumanReviewPage.tsx`
   - 实现审核队列列表
   - 实现审核操作（批准/拒绝）
   - 添加路由配置

2. **前后端联调**
   - 调用后端审核 API
   - 验证审核流程
   - 测试审核历史

3. **验证**
   - 审核队列显示正确
   - 审批操作生效
   - 工作流恢复执行

**Agent 配置**:
```typescript
delegate_task({
  category: "visual-engineering",
  load_skills: ["frontend-ui-ux"],
  description: "实现 Human Review 前端页面",
  prompt: "实现人工审核页面并验证完整流程...",
  run_in_background: true
})
```

**预计时间**: 90 分钟

**成功标准**:
- ✅ 审核页面实现完整
- ✅ 审核流程正常
- ✅ 工作流暂停/恢复正确

---

### Wave 2 并行执行

**并行度**: 2个任务同时执行

**预计总时间**: 90 分钟

---

## 🌊 Wave 3: 验证高级功能

**目标**: 验证依赖 Wave 0 修复的高级功能

### 任务 3.1: 验证 Workflow 执行功能

**优先级**: 🟡 P1

**验证内容**:
1. **工作流执行**
   - 启动工作流执行
   - SSE 流式日志接收
   - 节点执行日志记录
   - 执行历史查询

2. **执行模式**
   - STANDARD 模式
   - DEBUG 模式
   - DRY_RUN 模式

3. **错误处理**
   - 执行失败处理
   - 取消执行
   - 超时处理

**Agent 配置**:
```typescript
delegate_task({
  category: "quick",
  load_skills: [],
  description: "验证 Workflow 执行功能",
  prompt: "验证工作流执行的完整流程...",
  run_in_background: true
})
```

**预计时间**: 45 分钟

**成功标准**:
- ✅ 工作流执行成功
- ✅ SSE 流式日志正常
- ✅ 执行历史记录正确

---

### 任务 3.2: 验证 Knowledge 向量检索功能

**优先级**: 🟡 P1

**验证内容**:
1. **向量化**
   - 文档分块
   - 向量化处理
   - 存储到 Milvus

2. **检索**
   - 相似度检索
   - 检索结果排序
   - 检索准确性

3. **集成**
   - 工作流中使用知识库
   - RAG 功能验证

**Agent 配置**:
```typescript
delegate_task({
  category: "quick",
  load_skills: [],
  description: "验证 Knowledge 向量检索",
  prompt: "验证知识库向量检索功能...",
  run_in_background: true
})
```

**预计时间**: 45 分钟

**成功标准**:
- ✅ 文档向量化成功
- ✅ 检索功能正常
- ✅ RAG 集成正确

---

### Wave 3 并行执行

**并行度**: 2个任务同时执行

**预计总时间**: 45 分钟

---

## 📊 整体时间估算

| Wave | 任务数 | 并行度 | 预计时间 |
|------|--------|--------|---------|
| Wave 0 | 5 | 1 | 30 分钟 |
| Wave 1 | 3 | 3 | 45 分钟 |
| Wave 2 | 2 | 2 | 90 分钟 |
| Wave 3 | 2 | 2 | 45 分钟 |
| **总计** | **12** | - | **3.5 小时** |

---

## ✅ 验证清单

### 数据库修复
- [ ] workflow_execution 表创建成功
- [ ] knowledge_chunk 表创建成功
- [ ] knowledge_vector_index 表创建成功
- [ ] workflow_human_review_task 表创建成功
- [ ] conversations 表数据类型修复

### Workflow Editor
- [ ] 后端接口全部正常
- [ ] 前端编辑器功能正常
- [ ] 工作流保存和加载正确
- [ ] 版本管理功能正常

### Chat
- [ ] 会话 CRUD 功能正常
- [ ] 消息历史加载正确
- [ ] 前后端数据一致

### Knowledge
- [ ] 知识库 CRUD 功能正常
- [ ] 文档上传成功
- [ ] 向量检索功能正常

### Dashboard
- [ ] DashboardController 实现完整
- [ ] 统计数据准确
- [ ] 前端显示真实数据

### Human Review
- [ ] 审核页面实现完整
- [ ] 审核流程正常
- [ ] 工作流暂停/恢复正确

---

## 🚀 执行命令

### 启动后端服务
```bash
mvn spring-boot:run -pl ai-agent-interfaces -Dspring-boot.run.profiles=local
```

### 启动前端服务
```bash
cd ai-agent-foward
npm run dev
```

### 执行数据库修复
```bash
mysql -h localhost -P 13306 -u root -p ai_agent < ai-agent-infrastructure/src/main/resources/db/ai_agent.sql
```

---

**计划状态**: 🚀 准备执行  
**下一步**: 执行 Wave 0 数据库修复
