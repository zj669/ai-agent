## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-15
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: WorkflowController.java
- 该控制器暴露工作流执行入口与调试查询接口：启动 SSE 执行流、停止执行、查询执行详情、节点日志、会话历史与上下文快照。
- 在接口层负责 HTTP/SSE 协议编排与资源清理（Redis 订阅、心跳任务、Emitter 生命周期），实际调度交给 `SchedulerService`。
- 当前阶段主链路不要求手动暂停的对外契约。

## 2) 核心方法
- `startExecution(StartExecutionRequest request)`
- `stopExecution(StopExecutionRequest request)`
- `getExecution(String executionId)`
- `getNodeExecutionLog(String executionId, String nodeId)`
- `getExecutionLogs(String executionId)`
- `getHistory(String conversationId)`
- `getExecutionContext(String executionId)`

## 3) 具体方法
### 3.1 startExecution(StartExecutionRequest request)
- 函数签名: `public SseEmitter startExecution(@RequestBody StartExecutionRequest request)`
- 入参: agent/user/conversation/version/inputs/mode
- 出参: `SseEmitter`
- 功能含义: 创建 executionId，建立 SSE 连接；注册 Redis 频道监听并桥接事件到 emitter；启动心跳与清理钩子；异步调用 `SchedulerService.startExecution(...)`。
- 链路作用: 前端发起执行的主入口，连接“HTTP 请求 → Redis 事件 → SSE 流式回传”。

### 3.2 stopExecution(StopExecutionRequest request)
- 函数签名: `public ResponseEntity<Void> stopExecution(@RequestBody StopExecutionRequest request)`
- 入参: `executionId`
- 出参: `200 OK`
- 功能含义: 调用 `schedulerService.cancelExecution` 标记执行取消。
- 链路作用: 人工中断执行链路的外部控制点。

### 3.3 getExecution(String executionId)
- 函数签名: `public ResponseEntity<ExecutionDTO> getExecution(@PathVariable String executionId)`
- 功能含义: 从 `ExecutionRepository` 查询执行并映射 DTO。

### 3.4 getExecutionLogs(String executionId)
- 函数签名: `public ResponseEntity<List<WorkflowNodeExecutionLogDTO>> getExecutionLogs(@PathVariable String executionId)`
- 功能含义: 查询执行下所有节点日志并转换为接口 DTO 返回。

## 4) 变更记录
- 2026-03-02: 收敛蓝图范围，移除手动暂停主链路契约，聚焦“暂停/恢复 + 人审通过继续”。
- 2026-02-15: 回填蓝图语义，补充真实职责、核心接口与执行流说明。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
