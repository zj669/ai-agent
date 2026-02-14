## Metadata
- file: `.blueprint/interfaces/Controllers.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: Controllers
- 该文件描述 REST API 控制器层的职责边界，负责接收 HTTP 请求、参数校验、调用应用服务、返回响应。主要包括 AgentController（智能体管理）和 WorkflowController（工作流执行）。

## 2) 核心方法
- `createAgent()`
- `updateAgent()`
- `startExecution()`
- `getExecution()`
- `stopExecution()`

## 3) 具体方法
### 3.1 createAgent()
- 函数签名: `POST /api/agent/create → Response<Long> createAgent(@RequestBody AgentRequest.SaveAgentRequest req)`
- 入参: `SaveAgentRequest{name, description, icon}`，从 UserContext 获取 userId
- 出参: `Response<Long>` 包含新创建的 agentId
- 功能含义: 创建新智能体，构建 CreateAgentCmd 并委托给 AgentApplicationService
- 链路作用: 接口层入口 → AgentApplicationService.createAgent() → AgentRepository.save()

### 3.2 updateAgent()
- 函数签名: `POST /api/agent/update → Response<Void> updateAgent(@RequestBody AgentRequest.SaveAgentRequest req)`
- 入参: `SaveAgentRequest{id, name, description, icon, graphJson, version}`，从 UserContext 获取 userId
- 出参: `Response<Void>` 表示更新成功
- 功能含义: 更新智能体草稿版本（包括工作流图 graphJson），构建 UpdateAgentCmd 并委托给应用服务
- 链路作用: 接口层入口 → AgentApplicationService.updateAgent() → Agent.updateDraft() → AgentRepository.save()

### 3.3 startExecution()
- 函数签名: `POST /api/workflow/execution/start → SseEmitter startExecution(@RequestBody StartExecutionRequest req)`
- 入参: `StartExecutionRequest{agentId, userId, conversationId, versionId?, inputs, mode}`
- 出参: `SseEmitter` 用于 SSE 流式推送执行事件
- 功能含义: 启动工作流执行并建立 SSE 连接，订阅 Redis 频道接收执行事件（node_start/node_complete/delta/error），异步调用 SchedulerService.startExecution()
- 链路作用: 接口层入口 → 创建 SseEmitter + Redis 监听器 → SchedulerService.startExecution() → 执行引擎 → RedisSsePublisher → SSE 推送

### 3.4 getExecution()
- 函数签名: `GET /api/workflow/execution/{executionId} → ResponseEntity<ExecutionDTO> getExecution(@PathVariable String executionId)`
- 入参: `executionId` 路径参数
- 出参: `ResponseEntity<ExecutionDTO>` 包含执行状态、节点状态、上下文等
- 功能含义: 查询执行详情（调试用），从 ExecutionRepository 加载并转换为 DTO
- 链路作用: 接口层入口 → ExecutionRepository.findById() → 转换为 ExecutionDTO

### 3.5 stopExecution()
- 函数签名: `POST /api/workflow/execution/stop → ResponseEntity<Void> stopExecution(@RequestBody StopExecutionRequest req)`
- 入参: `StopExecutionRequest{executionId}`
- 出参: `ResponseEntity<Void>` 表示取消成功
- 功能含义: 取消正在运行的工作流执行，调用 SchedulerService.cancelExecution() 标记取消状态
- 链路作用: 接口层入口 → SchedulerService.cancelExecution() → WorkflowCancellationPort.markAsCancelled() → Redis 标记


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全所有方法签名、入参、出参、功能含义和链路作用，基于 AgentController 和 WorkflowController 实现。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
