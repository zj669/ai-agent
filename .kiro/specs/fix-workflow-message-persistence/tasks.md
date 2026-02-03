# Workflow 消息持久化优化 - 任务列表

## Phase 1: 数据库和基础设施层

### 1.1 数据库迁移
- [x] 创建迁移文件 `V1_4__add_assistant_message_id_to_execution.sql`
  - 在 `workflow_execution` 表添加 `assistant_message_id` 字段
  - 添加索引 `idx_assistant_message_id`
  - 文件路径: `ai-agent-infrastructure/src/main/resources/db/migration/`

### 1.2 添加索引优化查询
- [x] 在 `workflow_node_execution_log` 表添加复合索引
  ```sql
  CREATE INDEX idx_execution_type_time ON workflow_node_execution_log(execution_id, node_type, end_time);
  ```

## Phase 2: Domain 层修改

### 2.1 修改 Execution 实体
- [x] 添加 `assistantMessageId` 字段
- [x] 添加 getter/setter 方法
- [ ] 文件: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java`

### 2.2 修改 WorkflowNodeExecutionLogRepository 接口
- [x] 添加方法 `WorkflowNodeExecutionLog findByExecutionIdAndNodeId(String executionId, String nodeId)`
- [x] 添加方法 `List<WorkflowNodeExecutionLog> findByExecutionIdOrderByEndTime(String executionId)`
- [ ] 文件: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/WorkflowNodeExecutionLogRepository.java`

## Phase 3: Infrastructure 层修改

### 3.1 修改 ExecutionPO
- [ ] 添加 `assistantMessageId` 字段
- [ ] 文件: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/po/ExecutionPO.java`

### 3.2 修改 ExecutionRepositoryImpl
- [x] 确保 `assistantMessageId` 字段正确映射
- [x] 测试保存和查询功能

### 3.3 实现 WorkflowNodeExecutionLogRepository
- [x] 实现 `findByExecutionIdAndNodeId()` 方法
- [x] 实现 `findByExecutionIdOrderByEndTime()` 方法
- [x] 使用 MyBatis Plus 的 LambdaQueryWrapper
- [x] 按 `end_time` 升序排序
- [ ] 文件: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/repository/WorkflowNodeExecutionLogRepositoryImpl.java`

## Phase 4: Application 层修改

### 4.1 修改 SchedulerService.startExecution()
- [x] 提取用户输入 `extractUserQuery(inputs)`
- [x] 调用 `ChatApplicationService.appendUserMessage()` 保存用户消息
- [x] 调用 `ChatApplicationService.initAssistantMessage()` 初始化 Assistant 消息
- [x] 将 `assistantMessageId` 保存到 Execution
- [x] 添加异常处理，不阻塞 workflow 执行
- [ ] 文件: `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java`

### 4.2 新增 SchedulerService.onExecutionComplete()
- [x] 检查执行状态（SUCCEEDED/FAILED）
- [x] 调用 `extractFinalResponseFromLogs()` 提取最终响应
- [x] 调用 `ChatApplicationService.finalizeMessage()` 更新消息
- [x] 处理失败情况，更新消息状态为 FAILED
- [x] 添加异常处理和日志

### 4.3 新增 SchedulerService.extractFinalResponseFromLogs()
- [x] 优先查询 END 节点的输出 `findByExecutionIdAndNodeId(executionId, "END")`
- [x] 如果 END 节点没有输出，查询最后执行的节点 `findByExecutionIdOrderByEndTime()`
- [x] 提取 `response`、`text`、`output` 或 `result` 字段
- [x] 返回默认值 "执行完成" 如果没有找到

### 4.4 修改 SchedulerService.onNodeComplete()
- [x] 在执行完成时调用 `onExecutionComplete()`
- [x] 确保在状态更新和事件发布后调用

### 4.5 去掉 Redis 依赖（可选）
- [ ] 修改 `cancelExecution()` 方法，使用数据库状态
- [ ] 修改 `isCancelled()` 方法，查询数据库
- [ ] 去掉人工审核的 Redis Set，改用数据库查询
- [ ] 保留 SSE 流式推送的 Redis 使用

## Phase 5: 接口层修改

### 5.1 添加查询思维链接口
- [x] 新增 `GET /api/workflow/execution/{executionId}/logs`
- [x] 调用 `workflowNodeExecutionLogRepository.findByExecutionId()`
- [x] 返回节点执行日志列表
- [ ] 文件: `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java`

### 5.2 验证现有接口
- [ ] 确认 `GET /api/chat/conversations/{conversationId}/messages` 正常工作
- [ ] 测试分页功能
- [ ] 测试排序（按 created_at 升序）

## Phase 6: 测试

### 6.1 单元测试
- [ ] 测试 `extractFinalResponseFromLogs()` 方法
  - 有 END 节点输出
  - END 节点无输出，使用最后一个节点
  - 没有任何节点输出
  - 测试不同的响应字段名（response/text/output/result）
- [ ] 测试消息保存逻辑
  - 保存用户消息
  - 初始化 Assistant 消息
  - 更新 Assistant 消息

### 6.2 集成测试
- [ ] 测试完整的 workflow 执行 + 消息保存流程
  - 启动 workflow
  - 执行节点
  - 保存日志
  - 更新消息
- [ ] 测试多轮对话
  - 第一轮对话
  - 查询历史
  - 第二轮对话（带上下文）
- [ ] 测试异常情况
  - 消息保存失败
  - 日志查询失败
  - 缺少 conversationId

### 6.3 端到端测试
- [ ] 前端发起对话
- [ ] 查询聊天历史
- [ ] 查看思维链
- [ ] 验证消息完整性和顺序
- [ ] 验证上下文加载

## Phase 7: 文档和日志

### 7.1 添加关键日志
- [x] 消息保存成功/失败日志
- [x] Workflow 执行完成日志
- [x] 消息更新状态日志
- [x] 从日志提取响应的日志

### 7.2 更新 API 文档
- [x] 说明消息保存机制
- [x] 更新 `/api/chat/conversations/{conversationId}/messages` 接口文档
- [x] 添加 `/api/workflow/execution/{executionId}/logs` 接口文档

## Phase 8: 验证和部署

### 8.1 本地验证
- [ ] 启动完整服务（MySQL, MinIO, Milvus）
- [ ] 测试对话功能
- [ ] 查询聊天历史
- [ ] 查看思维链
- [ ] 测试多轮对话

### 8.2 代码审查
- [ ] 检查代码质量
- [ ] 确保符合 DDD 架构规范
- [ ] 验证异常处理
- [ ] 检查日志完整性

### 8.3 性能测试
- [ ] 测试消息保存耗时
- [ ] 测试日志查询性能
- [ ] 验证索引效果

### 8.4 部署准备
- [ ] 准备数据库迁移脚本
- [ ] 准备回滚方案
- [ ] 更新部署文档
- [ ] 准备监控告警

## 任务优先级

### P0 (必须完成)
- Phase 1: 数据库迁移
- Phase 2: Domain 层修改
- Phase 3: Infrastructure 层修改
- Phase 4.1-4.4: Application 层核心功能

### P1 (重要)
- Phase 5: 接口层修改
- Phase 6: 测试
- Phase 7: 文档和日志

### P2 (可选)
- Phase 4.5: 去掉 Redis 依赖
- Phase 8.3: 性能测试

## 预估工作量

- Phase 1-3: 2 小时
- Phase 4: 3 小时
- Phase 5: 1 小时
- Phase 6: 2 小时
- Phase 7-8: 1 小时

**总计: 约 9 小时**
