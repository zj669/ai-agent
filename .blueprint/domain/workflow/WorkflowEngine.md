## Metadata
- file: `.blueprint/domain/workflow/WorkflowEngine.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: WorkflowEngine
- 该文件用于描述 WorkflowEngine 的职责边界与协作关系。

## 2) 核心方法
- `start()`
- `advance()`
- `resume()`
- `getReadyNodes()`
- `createCheckpoint()`

## 3) 具体方法
### 3.1 start()
- 函数签名: `List<Node> start(Map<String, Object> inputs)`
- 入参:
  - `inputs`: 工作流全局输入参数
- 出参: 就绪节点列表（入度为0的起始节点）
- 功能含义: 启动工作流执行，校验图结构（检测循环依赖），初始化上下文和节点状态，返回可执行的起始节点。
- 链路作用: 在 SchedulerService.startExecution() 中调用，触发工作流执行的第一步。

### 3.2 advance()
- 函数签名: `List<Node> advance(String nodeId, NodeExecutionResult result)`
- 入参:
  - `nodeId`: 已完成节点的ID
  - `result`: 节点执行结果（包含状态、输出、分支选择等）
- 出参: 下一批就绪节点列表（依赖已满足的节点）
- 功能含义: 推进工作流执行，更新节点状态，存储输出到上下文，处理条件分支剪枝，检查暂停/完成状态，返回下一批可执行节点。
- 链路作用: 在 SchedulerService.onNodeComplete() 中调用，驱动工作流 DAG 遍历。

### 3.3 resume()
- 函数签名: `List<Node> resume(String nodeId, Map<String, Object> additionalInputs)`
- 入参:
  - `nodeId`: 暂停节点的ID
  - `additionalInputs`: 人工审核提供的额外输入/修改
- 出参: 恢复后的就绪节点列表（BEFORE_EXECUTION 返回当前节点，AFTER_EXECUTION 返回空列表）
- 功能含义: 恢复暂停的工作流执行，合并人工审核的输入/输出修改，重置暂停状态，根据 TriggerPhase 决定返回节点。
- 链路作用: 在 SchedulerService.resumeExecution() 中调用，处理人工审核后的恢复逻辑。

### 3.4 getReadyNodes()
- 函数签名: `List<Node> getReadyNodes()`
- 入参: 无
- 出参: 当前就绪节点列表（状态为 PENDING 且有效入度为0）
- 功能含义: 计算有效入度（排除已完成/跳过/失败的前驱），返回所有依赖已满足的待执行节点。
- 链路作用: 在 start() 和 advance() 中调用，提供下一批可调度的节点。

### 3.5 createCheckpoint()
- 函数签名: `Checkpoint createCheckpoint(String nodeId)`
- 入参:
  - `nodeId`: 当前节点ID
- 出参: 检查点对象（包含 executionId、nodeId、context 快照）
- 功能含义: 创建执行检查点，保存当前上下文快照到 Redis，用于暂停/恢复或故障恢复。
- 链路作用: 在 SchedulerService.onNodeComplete() 和 checkPause() 中调用，持久化执行状态。


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全方法签名与语义，从 Execution.java 提取真实实现契约。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
