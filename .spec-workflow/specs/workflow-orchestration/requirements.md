# 需求文档 - 工作流编排模块 (Orchestration Context) v3.2

## 1. 模块概述

### 1.1 简介
工作流编排模块（Core Domain）是 AI Agent 平台的引擎心脏，负责解析、调度和执行基于 DAG（有向无环图）的复杂任务流。本版本重点引入了 **策略模式** 以支持高扩展性，并增强了 **条件路由** 能力以支持复杂的业务决策。

### 1.2 核心价值
- **高扩展性**：通过策略模式将调度逻辑与节点执行解耦，轻松扩展新节点类型。
- **智能路由**：支持基于规则（表达式）和基于语义（大模型）的双模路由。
- **高并发**：支持基于入度分析的节点并行执行。
- **强鲁棒性**：内置自动重试、超时控制和断点恢复机制。

---

## 2. 核心架构需求

### 需求 1：节点执行架构（策略模式）
**用户故事**：作为系统架构师，我希望核心调度器不依赖具体的节点业务逻辑，以便于维护和扩展。

**验收标准**：
1.  **策略接口定义**：定义统一接口 `NodeExecutorStrategy<T extends NodeConfig>`，包含方法 `execute(node, context)`。
2.  **策略工厂**：实现 `ExecutorFactory`，根据 `nodeType`（如 LLM, HTTP, CONDITION）动态路由到对应的 Spring Bean。
3.  **调度解耦**：`SchedulerService` 仅负责计算 DAG 拓扑和状态流转，严禁在调度器中出现 `if (type == LLM)` 这样的硬编码业务逻辑。

### 需求 2：并行调度引擎
**用户故事**：作为用户，我希望没有依赖关系的节点能够同时运行，以减少总等待时间。

**验收标准**：
1.  **入度计算**：基于 DAG 拓扑排序算法，动态维护每个节点的实时入度（In-Degree）。
2.  **并行触发**：当发现多个节点的入度为 0 且前序依赖均成功时，利用线程池（Thread Pool）并发触发执行。
3.  **汇聚等待 (Join)**：对于有多个父节点的节点，必须等待**所有**父节点状态变为 `SUCCESS` 后方可激活。
4.  **短路机制**：若任一父节点状态为 `FAILED`（且无容错配置），后续节点应标记为 `SKIPPED` 或 `CANCELLED`。

### 需求 3：数据上下文与表达式引擎
**用户故事**：作为用户，我希望在节点间灵活传递数据，并使用表达式进行逻辑处理。

**验收标准**：
1.  **全局上下文**：维护 `ExecutionContext`，存储所有已完成节点的输出（Key为 `nodeId`）。
2.  **SpEL 集成**：引入 Spring Expression Language (SpEL)。
3.  **动态参数映射**：
    * 支持 `#{node_a.output.text}` 引用上游数据。
    * 支持 `#{inputs.query}` 引用全局输入。
    * **预处理**：Executor 在执行业务逻辑前，必须先解析并替换配置中的所有 SpEL 占位符。

---

## 3. 节点策略详解

### 需求 4：通用节点行为 (Retry & Fallback, 当节点有配置兜底策略时才有)
**验收标准**：
1.  **自动重试**：所有业务节点支持配置 `maxRetries` (最大重试次数) 和 `retryDelay` (重试间隔)。
2.  **超时控制**：支持配置 `timeout` (毫秒)，执行超时的节点应被强制中断并标记为 `FAILED`。

### 需求 5：业务节点策略 (LLM, HTTP, TOOL)
**验收标准**：
1.  **LLM 节点**：
    * 对接模型服务，支持流式 (Stream) 输出。
    * 输入参数支持 SpEL 动态填充 Prompt 模板。
2.  **HTTP 节点**：
    * 支持配置 URL, Method, Headers, Body。
    * 支持将 Response Body 写入上下文供下游使用。
3.  **TOOL 节点**：
    * 对接 MCP (Model Context Protocol) 协议，执行外部工具。

### 需求 6：控制流节点 - 条件路由 (CONDITION) 【核心增强】
**用户故事**：作为用户，我希望根据前序节点的输出或大模型的语义判断，动态决定后续执行路径。

**验收标准**：
1.  **双模式支持**：节点配置中需包含 `routingStrategy` 字段，支持 `EXPRESSION` (表达式) 和 `LLM` (语义) 两种模式。
2.  **分支定义**：支持配置有序的 `branches` 列表，每个分支包含 `branchId`, `condition/description`。
3.  **模式 A - 表达式路由**：
    * 遍历分支列表，逐个执行 SpEL 表达式（如 `#score > 0.8`）。
    * 命中第一个返回 `true` 的分支，返回其 `branchId`。
4.  **模式 B - LLM 语义路由**：
    * 构建 Prompt，包含用户输入和所有分支的描述。
    * 要求 LLM 选择最匹配的分支，并返回 `branchId`。
    * 包含容错逻辑：若 LLM 返回无效 ID，走默认分支。
5.  **调度器配合**：
    * 调度器接收到 `CONDITION` 节点返回的 `branchId`。
    * **激活**：仅将连接在对应 `sourceHandle` 上的下游节点加入就绪队列。
    * **跳过**：将其他未命中分支的下游节点及其子节点递归标记为 `SKIPPED`。

---

## 4. 状态管理与生命周期

### 需求 7：生命周期事件
**验收标准**：
1.  **状态流转**：节点状态机：`PENDING` -> `RUNNING` -> `SUCCESS` / `FAILED` / `SKIPPED` / `PAUSED`。
2.  **事件推送**：节点状态变更时，通过 `EventPublisher` 发送 SSE 事件（含 `nodeId`, `status`, `outputs`）。

### 需求 8：人工介入 (Human-in-the-loop)
**验收标准**：
1.  **暂停**：当调度器遇到配置了 `humanCheck: true` 的节点，在执行前暂停，状态置为 `PAUSED`。
2.  **恢复**：提供 API `resumeExecution(executionId, nodeId, approve/reject, inputs)`。
3.  **逻辑**：人工审核通过后，恢复调度器运行；拒绝则终止流程。

### 需求 9：检查点 (Checkpointing)
**验收标准**：
1.  **自动存档**：每个节点执行变为终态（Success/Failed/Paused）时，异步保存 `ExecutionContext` 快照到数据库。
2.  **故障恢复**：系统重启时，可读取最近的 Checkpoint，重建内存 DAG 并继续调度未完成的节点。


### 需求 10：健壮性与安全 (Robustness & Security)
**验收标准**：
1.  **DAG 静态检查**：在工作流启动前 (`start()`)，必须进行**环检测** (Cycle Detection) 和**连通性检查**。若发现循环依赖或孤立节点，应拒绝执行并报错。
2.  **SpEL 安全沙箱**：配置 SpEL 的 `EvaluationContext` 为受限模式，禁止调用 `T(System)`、`T(Runtime)` 等危险类，仅允许访问 `inputs`、`outputs` 和特定的工具类方法。
3.  **敏感数据保护**：对于 API Key 等敏感配置，支持通过环境变量或配置中心引用 (如 `#{env.SECRET_KEY}`)，避免明文硬编码在图定义中。

---

## 5. 数据结构参考

### 5.1 Condition 节点配置示例
```json
{
  "nodeId": "router-1",
  "type": "CONDITION",
  "userConfig": {
    "routingStrategy": "LLM", // 或 "EXPRESSION"
    "llmConfig": { "model": "gpt-4", "temperature": 0 },
    "branches": [
      {
        "branchId": "case_refund",
        "label": "退款请求",
        "description": "用户明确表达想要退款或退货",
        "condition": "#intent == 'refund'" // 仅在 EXPRESSION 模式下生效
      },
      {
        "branchId": "case_chat",
        "label": "闲聊",
        "description": "用户只是在打招呼或闲聊",
        "condition": "true" // 默认分支
      }
    ]
  }
}