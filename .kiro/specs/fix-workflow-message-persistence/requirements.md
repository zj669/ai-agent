# Workflow 消息持久化优化 - 需求文档

## 1. 问题描述

当前系统的上下文管理和消息存储过于复杂，需要简化架构：

**现状问题：**
1. Workflow 执行时，消息只保存在 `Execution` 和 `ExecutionContext` 中
2. `ChatController.getMessages()` 从 `Message` 表查询，但 workflow 的消息从未写入
3. 使用 Redis 存储临时数据，增加了系统复杂度
4. 前端无法获取完整的聊天历史记录

**优化目标：**
1. **messages 表** - 存储用户和 AI 的对话消息，用于上下文加载
2. **workflow_node_execution_log 表** - 存储节点执行日志，用于前端展示思维链
3. **去掉 Redis 依赖** - 所有数据持久化到 MySQL

## 2. 数据模型设计

### 2.1 messages 表（对话消息）
**用途：** 存储用户和 AI 的对话消息，用于加载上下文

**字段说明：**
- `id` - 消息 ID (UUID)
- `conversation_id` - 会话 ID
- `role` - 角色 (USER/ASSISTANT)
- `content` - 消息内容（用户输入或 AI 最终响应）
- `meta_data` - 元数据（包含 executionId）
- `status` - 消息状态 (PENDING/COMPLETED/FAILED)
- `created_at` - 创建时间

**关键点：**
- 只存储**最终的对话结果**，不存储中间过程
- USER 消息：用户的输入
- ASSISTANT 消息：AI 的最终响应（从最后一个 LLM 节点提取）

### 2.2 workflow_node_execution_log 表（节点执行日志）
**用途：** 存储每个节点的执行详情，用于前端展示思维链

**字段说明：**
- `id` - 日志 ID
- `execution_id` - 工作流执行 ID
- `node_id` - 节点 ID
- `node_name` - 节点名称
- `node_type` - 节点类型 (LLM/HTTP/CONDITION 等)
- `render_mode` - 渲染模式 (MESSAGE/HIDDEN/TRACE)
- `status` - 执行状态
- `inputs` - 输入参数 (JSON)
- `outputs` - 输出结果 (JSON)
- `error_message` - 错误信息
- `start_time` / `end_time` - 执行时间

**关键点：**
- 存储**每个节点的执行详情**
- 用于前端可视化展示思维链
- 包含所有中间步骤
- **这是唯一持久化到 MySQL 的 workflow 执行数据**

### 2.3 Execution 实体（Redis 临时存储）
**用途：** 工作流执行的运行时状态，存储在 Redis 中

**关键点：**
- 存储在 Redis，48 小时 TTL
- 包含 `assistantMessageId` 字段用于关联消息
- 使用 Jackson 自动序列化
- 不持久化到 MySQL

## 3. 用户故事

### 3.1 作为用户，我希望能查看完整的聊天历史
**验收标准：**
- 通过 workflow 执行的对话能在聊天历史中显示
- 只显示用户消息和 AI 最终响应
- 消息顺序正确，时间戳准确

### 3.2 作为用户，我希望能查看 AI 的思考过程
**验收标准：**
- 点击消息可以展开查看思维链
- 显示每个节点的执行详情
- 包含输入、输出、执行时间等信息

### 3.3 作为系统，我需要在 workflow 执行时保存数据
**验收标准：**
- Workflow 启动时保存用户消息到 messages 表
- 每个节点执行完成后保存日志到 workflow_node_execution_log 表
- Workflow 执行完成后保存 AI 响应到 messages 表

## 4. 功能需求

### 4.1 Workflow 启动时保存用户消息
- 在 `SchedulerService.startExecution()` 中
- 提取用户输入 (input/query/message)
- 调用 `ChatApplicationService.appendUserMessage()` 保存到 messages 表
- 初始化 PENDING 状态的 Assistant 消息

### 4.2 节点执行时保存日志
- 在 `SchedulerService.onNodeComplete()` 中
- 已有的 `NodeCompletedEvent` 监听器会保存到 workflow_node_execution_log 表
- 保持现有逻辑不变

### 4.3 Workflow 执行完成后保存 AI 响应
- 在 workflow 执行完成时 (SUCCEEDED 状态)
- 从 workflow_node_execution_log 表查询最后一个 LLM 节点的输出
- 更新 messages 表中的 Assistant 消息状态为 COMPLETED
- 填充最终响应内容

### 4.4 前端查询接口
- `GET /api/chat/conversations/{conversationId}/messages` - 查询对话历史（从 messages 表）
- `GET /api/workflow/execution/{executionId}/logs` - 查询思维链（从 workflow_node_execution_log 表）

## 5. 非功能需求

### 5.1 性能
- 消息保存不应阻塞 workflow 执行
- 使用事务确保数据一致性

### 5.2 数据一致性
- 确保消息和 execution 的关联正确
- messages.meta_data 中存储 executionId
- workflow_node_execution_log.execution_id 关联到 execution

### 5.3 简化架构
- **去掉 Redis 依赖** - 所有数据持久化到 MySQL
- 减少系统复杂度
- 便于数据查询和维护

## 6. 验收测试场景

### 场景 1: 基本对话保存
1. 用户发送消息 "你好"
2. Workflow 执行并返回响应
3. 查询 `GET /api/chat/conversations/{conversationId}/messages`
4. 能看到 2 条消息：用户消息 + AI 响应

### 场景 2: 查看思维链
1. 用户发送消息
2. Workflow 执行（包含多个节点）
3. 查询 `GET /api/workflow/execution/{executionId}/logs`
4. 能看到所有节点的执行日志

### 场景 3: 多轮对话
1. 用户发送第一条消息
2. 查看历史，有 2 条消息（用户 + AI）
3. 用户发送第二条消息
4. 查看历史，有 4 条消息（用户 + AI + 用户 + AI）
5. 消息顺序正确

### 场景 4: 上下文加载
1. 用户发送第一条消息 "我叫张三"
2. AI 响应 "你好，张三"
3. 用户发送第二条消息 "我叫什么名字？"
4. Workflow 启动时从 messages 表加载历史
5. AI 能正确回答 "你叫张三"

## 7. 实现优先级

1. **P0 (必须)**: 保存用户消息和 AI 响应到 messages 表
2. **P1 (重要)**: 从 workflow_node_execution_log 提取最终响应
3. **P2 (重要)**: 去掉 Redis 依赖，简化架构
4. **P3 (可选)**: 优化查询性能，添加索引
