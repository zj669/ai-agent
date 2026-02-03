# Workflow 消息持久化 - 简化版需求

## 1. 架构简化目标

### 1.1 当前问题
- Workflow 执行数据只存在 Redis（48小时过期）
- 用户对话历史查询不到 workflow 的消息
- 架构过于复杂，Redis 和 MySQL 职责不清

### 1.2 简化目标
- **MySQL 作为唯一持久化存储**
- **Redis 仅用于缓存加速**
- **清晰的数据分层**：
  - `workflow_execution` - workflow 执行主记录
  - `workflow_node_execution_log` - 节点执行详情（技术日志）
  - `messages` - 用户对话消息（业务数据）

## 2. 用户故事

### 2.1 作为用户，我希望查看完整的聊天历史
**验收标准：**
- 通过 workflow 执行的对话能在聊天历史中显示
- 用户消息和 AI 响应都能正确保存到 MySQL
- 消息顺序正确，时间戳准确

### 2.2 作为系统，我需要持久化 workflow 执行记录
**验收标准：**
- Workflow 执行记录保存到 MySQL `workflow_execution` 表
- 执行状态、输入输出都能持久化
- 支持历史查询和审计

### 2.3 作为系统，我需要自动生成对话消息
**验收标准：**
- Workflow 完成后自动生成 USER 和 ASSISTANT 消息
- 消息内容从 workflow 输入输出提取
- 消息关联正确的 conversationId 和 executionId

## 3. 功能需求

### 3.1 创建 workflow_execution 表
- 存储 workflow 执行的主记录
- 包含：executionId, agentId, userId, conversationId, status, inputs, outputs 等
- 关联 messages 表（通过 executionId）

### 3.2 Workflow 启动时保存执行记录
- 创建 `workflow_execution` 记录（状态：RUNNING）
- 保存到 MySQL
- Redis 缓存（可选，用于加速查询）

### 3.3 Workflow 完成时更新记录并生成消息
- 更新 `workflow_execution` 状态（SUCCEEDED/FAILED）
- 保存最终输出
- **自动生成 2 条 messages 记录**：
  1. USER 消息 - 从 inputs 提取用户输入
  2. ASSISTANT 消息 - 从 outputs 提取最终响应

### 3.4 查询聊天历史
- 从 `messages` 表查询（包含 workflow 生成的消息）
- 按时间排序
- 支持分页

## 4. 数据模型

### 4.1 workflow_execution 表

```sql
CREATE TABLE `workflow_execution` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `execution_id` varchar(36) NOT NULL COMMENT '执行ID (UUID)',
  `agent_id` bigint NOT NULL COMMENT '智能体ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `conversation_id` varchar(36) NULL COMMENT '会话ID',
  `status` varchar(20) NOT NULL COMMENT '执行状态: PENDING, RUNNING, SUCCEEDED, FAILED, CANCELLED',
  `inputs` json NULL COMMENT '输入参数',
  `outputs` json NULL COMMENT '输出结果',
  `error_message` text NULL COMMENT '错误信息',
  `started_at` datetime NULL COMMENT '开始时间',
  `completed_at` datetime NULL COMMENT '完成时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_execution_id` (`execution_id`),
  KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_agent_user` (`agent_id`, `user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='工作流执行记录表';
```

### 4.2 messages 表（已存在）

在 `meta_data` 字段存储 workflow 相关信息：

```json
{
  "executionId": "uuid",
  "source": "workflow"
}
```

## 5. 非功能需求

### 5.1 性能
- MySQL 作为主存储，Redis 作为缓存
- 执行中的 workflow 可以缓存在 Redis
- 完成后的 workflow 从 MySQL 查询

### 5.2 数据一致性
- 使用事务确保 workflow_execution 和 messages 的一致性
- 失败时能够回滚

### 5.3 兼容性
- 不影响现有的直接聊天功能
- 保持 API 接口不变

## 6. 实现优先级

1. **P0 (必须)**: 创建 workflow_execution 表并持久化执行记录
2. **P0 (必须)**: Workflow 完成后自动生成 messages 记录
3. **P1 (重要)**: Redis 缓存优化
4. **P2 (可选)**: 执行历史查询和统计
