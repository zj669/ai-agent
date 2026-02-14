## Metadata
- file: `.blueprint/application/SchedulerService.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: SchedulerService
- 该文件用于描述 SchedulerService 的职责边界与协作关系。

## 2) 核心方法
- `cancelExecution()`
- `scheduleNodes()`
- `onExecutionComplete()`
- `hydrateMemory()`

## 3) 具体方法
### 3.1 cancelExecution()
- 函数签名: `void cancelExecution(String executionId)`
- 入参:
  - `executionId`: 执行ID
- 出参: 无（void）
- 功能含义: 取消工作流执行，调用 WorkflowCancellationPort.markAsCancelled() 标记取消状态到 Redis，后续节点调度会检查取消标记并停止执行。
- 链路作用: 在 WorkflowController.cancelExecution() 中调用，执行工作流取消逻辑。

### 3.2 scheduleNodes()
- 函数签名: `void scheduleNodes(String executionId, List<Node> nodes, String parentId)`
- 入参:
  - `executionId`: 执行ID
  - `nodes`: 待调度节点列表
  - `parentId`: 父节点ID（用于并行节点分组）
- 出参: 无（void）
- 功能含义: 调度节点列表，检查取消标记，为并行节点生成 parentId，遍历节点调用 scheduleNode() 执行。
- 链路作用: 在 startExecution()、onNodeComplete()、resumeExecution() 中调用，驱动工作流节点调度。

### 3.3 onExecutionComplete()
- 函数签名: `void onExecutionComplete(Execution execution)`
- 入参:
  - `execution`: 执行实体（状态为 SUCCEEDED/FAILED）
- 出参: 无（void）
- 功能含义: 工作流执行完成回调，提取最终响应（从 END 节点输出或执行日志），构建思维链（从 WorkflowNodeExecutionLog），调用 ChatApplicationService.finalizeMessage() 更新 Assistant 消息。
- 链路作用: 在 onNodeComplete() 中调用，处理工作流完成后的消息更新逻辑。

### 3.4 hydrateMemory()
- 函数签名: `void hydrateMemory(Execution execution, Map<String, Object> inputs)`
- 入参:
  - `execution`: 执行实体
  - `inputs`: 全局输入参数
- 出参: 无（void）
- 功能含义: 记忆水合，在工作流启动前加载记忆，包括 LTM（从 VectorStore 检索长期记忆）和 STM（从 ConversationRepository 加载历史对话），注入到 ExecutionContext。
- 链路作用: 在 startExecution() 中调用，为工作流执行提供记忆上下文。


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全方法签名与语义，从 SchedulerService.java 提取真实实现契约。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
