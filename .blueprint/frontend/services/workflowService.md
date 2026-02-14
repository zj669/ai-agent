## Metadata
- file: `.blueprint/frontend/services/workflowService.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: workflowService
- 该文件用于描述 workflowService 的职责边界与协作关系。

## 2) 核心方法
- `startExecution(request, callbacks)`
- `stopExecution(request)`
- `getExecutionContext(executionId)`
- `getExecutionLogs(executionId)`

## 3) 具体方法
### 3.1 startExecution(request, callbacks)
- 函数签名: `async startExecution(request: StartExecutionRequest, onEvent: SSECallbacks): Promise<AbortController>`
- 入参:
  - `request`: 包含 `agentId`, `conversationId`, `userMessage`, `executionMode` 的启动请求对象
  - `onEvent`: SSE 事件回调集合，包含 `onConnected`, `onStart`, `onUpdate`, `onFinish`, `onError`, `onPing`
- 出参: 返回 `AbortController` 实例，用于取消 SSE 连接
- 功能含义: 通过 `/api/workflow/execution/start` 发起 SSE 流式执行，建立长连接并持续接收执行事件。内部使用 `fetch` + `ReadableStream` 解析 SSE 协议（`event:` + `data:` 格式），根据事件类型分发到对应回调。
- 链路作用: 前端执行入口，连接后端 `WorkflowController.startExecution`，驱动整个工作流执行生命周期。历史实现已迁移至 ChatPage 的 SSE 集成逻辑。

### 3.2 stopExecution(request)
- 函数签名: `async stopExecution(request: StopExecutionRequest): Promise<void>`
- 入参: `request` 包含 `executionId` 字符串
- 出参: 无返回值（Promise<void>）
- 功能含义: 调用 `/api/workflow/execution/stop` 终止指定执行实例，触发后端 `Execution.cancel()` 状态转换。
- 链路作用: 用户主动停止执行的控制端点，配合 `AbortController.abort()` 实现前后端双向终止。

### 3.3 getExecutionContext(executionId)
- 函数签名: `async getExecutionContext(executionId: string): Promise<ExecutionContextDTO>`
- 入参: `executionId` 执行实例唯一标识
- 出参: 返回 `ExecutionContextDTO`，包含 `longTermMemory`, `shortTermMemory`, `awareness` 三层记忆结构
- 功能含义: 查询执行上下文的记忆快照，用于调试或恢复场景。对应后端 `ExecutionContext` 的序列化视图。
- 链路作用: 辅助查询接口，支持执行过程中的上下文可观测性。

### 3.4 getExecutionLogs(executionId)
- 函数签名: `async getExecutionLogs(executionId: string): Promise<WorkflowNodeExecutionLogDTO[]>`
- 入参: `executionId` 执行实例唯一标识
- 出参: 返回节点执行日志数组，每条日志包含 `nodeId`, `status`, `startTime`, `endTime`, `input`, `output`, `error`
- 功能含义: 获取执行历史的详细日志，用于审计和问题排查。对应后端 `WorkflowNodeExecution` 表的查询结果。
- 链路作用: 执行完成后的回溯查询接口，补充 SSE 实时流之外的持久化记录。


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全"具体方法"细节，基于历史实现（已删除）提供 SSE 流式执行、停止、上下文查询、日志查询的完整签名与语义。说明其在迁移链路中的定位：原独立 workflowService 已整合至 ChatPage 的 SSE 集成逻辑。
- 2026-02-14: 移除重复方法占位条目，保留唯一契约定义。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
