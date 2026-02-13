# AI Agent Platform - 5个核心模块验证计划草稿

## 背景调查结果汇总

### 后端实现现状
- ✅ **Workflow Editor**: 完整实现（AgentController + WorkflowController，SSE 流式）
- ✅ **Knowledge**: 完整实现（知识库 + 文档 + 向量检索）
- ✅ **Human Review**: 完整实现（审核队列 + 审批流程）
- ⚠️ **Chat**: 部分实现（缺少消息发送接口，需通过 Workflow API）
- ❌ **Dashboard**: 完全缺失（无 DashboardController）

### 前端实现现状
- ✅ **Workflow Editor**: 完整实现（React Flow 可视化编辑器，SSE 流式日志）
- ✅ **Knowledge**: 完整实现（知识库管理 + 文档上传 + 检索测试）
- ✅ **Chat**: 完整实现（会话管理 + SSE 流式消息）
- ⚠️ **Dashboard**: 部分实现（UI 完整，但使用 Mock 数据）
- ❌ **Human Review**: 完全缺失（无审核页面）

### 数据库表结构现状
- ❌ **严重缺失**: `workflow_execution` 表（影响 Workflow、Dashboard、Human Review）
- ❌ **数据类型不一致**: `conversations.user_id/agent_id` 是 varchar，应为 bigint
- ❌ **缺少关键表**: `knowledge_chunk`, `knowledge_vector_index`, `workflow_human_review_task`
- ⚠️ **缺少外键约束**: 多处表关联未建立外键

---

## 关键发现

### 架构设计问题
1. **Chat 模块架构分离**
   - 消息发送通过 `WorkflowController.startExecution()` (SSE)
   - 历史查询通过 `ChatController.getMessages()`
   - 前端需要理解两个接口的协作关系

2. **Dashboard 数据聚合策略**
   - 后端无专用统计接口
   - 前端通过多个 Service 拼凑数据（agentService, knowledgeService）
   - 缺少工作流执行统计、对话统计等数据源

3. **Human Review 功能标记存在但无 UI**
   - 节点属性面板有 `requiresHumanReview` 开关
   - 后端有完整的审核 API
   - 前端完全缺失审核页面

### 数据完整性风险
1. **workflow_execution 表缺失**
   - `workflow_node_execution_log.execution_id` 无外键约束
   - 无法追踪整体工作流执行状态
   - Dashboard 无法统计执行数据

2. **conversations 表数据类型不一致**
   - 无法建立外键约束
   - 数据孤岛风险

---

## 模块依赖关系分析

### 依赖图
```
workflow_execution (缺失)
    ↓
    ├─→ workflow_node_execution_log (存在)
    ├─→ workflow_human_review_record (存在)
    └─→ Dashboard 统计 (缺失)

agent_info (存在)
    ↓
    ├─→ agent_version (存在)
    ├─→ conversations (存在，但类型不一致)
    └─→ knowledge_dataset (存在)

knowledge_dataset (存在)
    ↓
    ├─→ knowledge_document (存在)
    └─→ knowledge_chunk (缺失)
         ↓
         └─→ knowledge_vector_index (缺失)
```

### 阻塞关系
- **Dashboard 验证** 阻塞于：
  - `workflow_execution` 表创建
  - DashboardController 实现
  
- **Human Review 验证** 阻塞于：
  - `workflow_human_review_task` 表创建
  - 前端审核页面实现

- **Knowledge 向量检索验证** 阻塞于：
  - `knowledge_chunk` 表创建
  - `knowledge_vector_index` 表创建

---

## 验证策略

### 立即可验证（无阻塞）
1. **Workflow Editor 基础功能**
   - 工作流编辑、保存、加载
   - 节点配置、连线
   - 前后端接口对齐

2. **Chat 基础功能**
   - 会话创建、列表、删除
   - 消息历史查询
   - 前后端接口对齐

3. **Knowledge 基础功能**
   - 知识库 CRUD
   - 文档上传
   - 前后端接口对齐

### 需要修复后验证
4. **Workflow 执行功能**
   - 需要：创建 `workflow_execution` 表
   - 验证：SSE 流式执行、节点日志、执行历史

5. **Dashboard 统计功能**
   - 需要：创建 `workflow_execution` 表 + DashboardController
   - 验证：统计数据准确性、自动刷新

6. **Human Review 完整流程**
   - 需要：创建 `workflow_human_review_task` 表 + 前端审核页面
   - 验证：审核队列、审批操作、审核历史

7. **Knowledge 向量检索**
   - 需要：创建 `knowledge_chunk` 和 `knowledge_vector_index` 表
   - 验证：向量化、检索准确性

---

## 决策点

### 1. Dashboard 实现策略
**选项 A**: 创建专用 DashboardController
- 优点：架构清晰，性能优化（缓存）
- 缺点：需要额外开发

**选项 B**: 前端拼凑现有 API
- 优点：快速验证
- 缺点：性能差，多次请求

**建议**: 选项 A（长期方案）

### 2. Chat 架构确认
**选项 A**: 保持现状（消息发送通过 Workflow API）
- 优点：复用工作流引擎
- 缺点：架构不直观

**选项 B**: 创建专用 ChatController.sendMessage()
- 优点：架构清晰
- 缺点：需要额外开发

**建议**: 选项 A（已实现，仅需文档说明）

### 3. Human Review 优先级
**选项 A**: 立即实现（P0）
- 理由：功能标记已存在，用户期望完整

**选项 B**: 延后实现（P1）
- 理由：非核心功能

**建议**: 选项 A（功能完整性）

---

## 用户决策确认

✅ **已确认所有决策点**：

1. **Dashboard**: A - 创建专用 DashboardController
2. **Chat**: A - 保持现状（消息发送复用工作流引擎）
3. **Human Review**: A - P0 立即实现前端审核页面
4. **数据库**: A - 立即修复所有 P0 表

## 下一步
- ✅ 决策点已确认
- 🚀 正在生成详细的并行验证计划
