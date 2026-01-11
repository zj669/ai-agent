

```markdown
# AI-Agent 智能对话模块需求文档 (v2.0)

## 1. 模块概述

### 1.1 模块名称
智能对话模块（Conversation Context）

### 1.2 模块定位
**交互域（Interaction Domain）** - 负责用户与 Agent 的实时交互、状态推送与调试观测。

### 1.3 核心价值
- **流式对话体验**：基于 SSE 实现打字机效果。
- **深度思考可视化**：支持类似 o1 模型的“思维链”折叠展示。
- **全链路观测（Debug）**：支持在调试模式下查看节点输入/输出、状态流转 DAG。
- **会话状态管理**：维护多轮对话上下文与历史记录。

---

## 2. 功能需求

### 2.1 发起对话（核心）

**功能描述**：用户发送消息触发工作流，支持“正常模式”和“调试模式”。

**业务规则**：
- **模式区分**：
    - `debug=false`（默认）：仅推送 `visibility != HIDDEN` 的节点事件。
    - `debug=true`：推送所有节点事件，且包含详细的 `input/output` 数据。
- **可见性控制**：后端根据节点配置的 `VisibilityConfig` 决定 SSE 事件中的 `renderMode`（HIDDEN/THOUGHT/MESSAGE）。
- **流式响应**：通过 SSE 实时推送节点状态变更（Started/Streaming/Completed）。

**输入参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| agentId | String | 是 | Agent ID |
| conversationId | String | 否 | 会话ID（新会话可不传） |
| message | String | 是 | 用户消息内容 |
| **debug** | Boolean | 否 | **是否开启调试模式**（默认 false） |
| inputs | Map | 否 | 额外的开始节点参数（用于测试） |

**输出**：SSE 事件流 (Content-Type: text/event-stream)

**SSE 事件类型概览**：
| 事件类型 | 说明 | 触发时机 |
|----------|------|----------|
| `dag_start` | 工作流启动 | 收到请求并开始执行时 |
| `node_start` | 节点开始运行 | 调度器选中节点开始执行时 |
| `node_chunk` | 节点流式输出 | LLM 生成 Token 时 |
| `node_end` | 节点执行完成 | 节点执行成功并产出 Output 时 |
| `dag_end` | 工作流结束 | 所有节点执行完毕时 |
| `error` | 异常 | 发生阻断性错误时 |

**接口路径**：`POST /client/chat/agent`

---

### 2.2 查询对话历史（含执行细节）

**功能描述**：获取历史消息列表。支持“普通视图”和“调试视图”。

**业务规则**：
- **普通视图**：仅返回 `renderMode` 为 `MESSAGE` 的最终回复，以及 `THOUGHT` 类型的思考过程摘要。
- **调试视图**：返回该次对话触发的所有节点执行日志（`workflow_node_execution_log`），包含输入输出。

**输入参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| conversationId | String | 是 | 会话ID |
| withDebugDetails | Boolean | 否 | 是否包含节点详细 I/O 数据（默认 false） |

**输出结构**：
```json
[
  {
    "messageId": "msg-001",
    "role": "USER",
    "content": "帮我分析财报",
    "createdAt": "..."
  },
  {
    "messageId": "msg-002",
    "role": "ASSISTANT",
    "content": "根据分析...", 
    "createdAt": "...",
    // 执行轨迹（用于渲染思考过程或调试图）
    "traceLog": [
      {
        "nodeId": "node-reasoning",
        "nodeName": "深度思考",
        "renderMode": "THOUGHT", // 前端据此渲染为折叠框
        "content": "正在读取数据库...\n发现异常数据...",
        "status": "SUCCEEDED",
        "duration": 1200
      },
      {
        "nodeId": "node-final",
        "renderMode": "MESSAGE",
        "content": "根据分析..."
      }
    ]
  }
]

```

**接口路径**：`GET /client/chat/history/{conversationId}`

---

### 2.3 获取单节点执行详情（调试用）

**功能描述**：在调试模式下，用户点击 DAG 图上的节点，异步加载该节点的完整输入输出（避免列表接口报文过大）。

**业务规则**：

* 查询 `workflow_node_execution_log` 表。
* 返回完整的 JSON 结构输入输出。

**输入参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| executionId | String | 是 | 工作流执行ID |
| nodeId | String | 是 | 节点ID |

**输出**：

```json
{
  "nodeId": "tool-weather",
  "status": "SUCCEEDED",
  "inputs": { "city": "Beijing", "date": "today" },
  "outputs": { "temp": 25, "condition": "Sunny" },
  "error": null,
  "startTime": 1704866400000,
  "endTime": 1704866400500
}

```

**接口路径**：`GET /client/chat/execution/{executionId}/node/{nodeId}`

---

## 3. SSE 事件协议详细定义

为了支持“思考模式”和“调试模式”，SSE 协议负载（Payload）必须携带 **渲染配置（RenderConfig）**。

### 3.1 通用字段定义

所有 SSE `data` 字段均包含以下元数据：

```json
{
  "executionId": "exec-123", // 执行实例ID
  "nodeId": "llm-node-1",    // 当前节点ID
  "nodeType": "LLM",         // 节点类型
  "timestamp": 1700000000,
  
  // 【核心】前端渲染控制
  "renderConfig": {
    // 显示模式：
    // - HIDDEN: 隐藏（调试模式下可见灰度节点）
    // - THOUGHT: 思考过程（前端渲染为折叠/流式输出）
    // - MESSAGE: 正式回复（前端追加到气泡）
    "mode": "THOUGHT", 
    "title": "正在分析意图..." // 思考框的标题
  }
}

```

### 3.2 关键事件示例

#### (1) `node_start` - 节点开始

*前端动作：调试模式下点亮 DAG 节点；正常模式下如果 mode=THOUGHT，显示“正在思考”加载条。*

```json
event: node_start
data: {
  "nodeId": "step-1",
  "nodeName": "意图识别",
  "renderConfig": { "mode": "HIDDEN" }, // 用户不可见，仅后台跑
  "inputs": { ... } // 仅 debug=true 时返回
}

```

#### (2) `node_chunk` - 流式输出

*前端动作：在对应的思考框或消息气泡中追加文本。*

```json
event: node_chunk
data: {
  "nodeId": "step-2",
  "renderConfig": { "mode": "THOUGHT", "title": "深度思考中" },
  "delta": "需要查询...", // 增量文本
  "content": "需要查询数据库" // (可选) 当前全量文本
}

```

#### (3) `node_end` - 节点完成

*前端动作：调试模式下标记节点成功；正常模式下将思考框标记为完成/折叠。*

```json
event: node_end
data: {
  "nodeId": "step-2",
  "status": "SUCCEEDED",
  "renderConfig": { "mode": "THOUGHT" },
  "outputs": { ... }, // 仅 debug=true 时返回
  "usage": { "tokens": 150 }
}

```

---

## 4. 数据库设计要求（支撑运行时）

为支持上述 API，需要记录详细的流水日志。

### 4.1 表结构：`workflow_node_execution_log`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | 主键 |
| execution_id | String | 关联一次对话执行 |
| node_id | String | 节点ID |
| node_name | String | 节点快照名称 |
| **render_mode** | String | 快照当时的显示模式 (HIDDEN/THOUGHT/MESSAGE) |
| status | Integer | 0:Running, 1:Success, 2:Failed |
| **inputs** | JSON | **完整输入参数** (调试用) |
| **outputs** | JSON | **完整输出结果** (调试用) |
| start_time | DateTime | 开始时间 |
| end_time | DateTime | 结束时间 |

---

## 5. 前端交互逻辑参考

### 5.1 正常模式 (Normal Mode)

* **监听**：只关注 `renderConfig.mode` 为 `THOUGHT` 或 `MESSAGE` 的事件。
* **UI展示**：
* `THOUGHT`：在消息流中插入一个 `<Accordion title="模型思考中...">`，将 `node_chunk` 内容打入其中。节点完成后自动收起。
* `MESSAGE`：标准的对话气泡追加。



### 5.2 调试模式 (Debug Mode)

* **布局**：左侧显示 DAG 流程图，右侧显示对话窗口。
* **左侧 DAG**：
* 监听 `node_start` / `node_end` 事件，实时变更节点颜色（灰->蓝->绿/红）。
* 鼠标点击节点，调用 `2.3` 接口获取详情，侧边弹窗显示 Input/Output JSON。


* **右侧 对话**：
* 显示**所有**节点产生的文本（包括 HIDDEN 节点产生的中间日志），通常以灰色小字或系统提示的形式展示，方便开发者追踪中间变量。



---

## 6. 非功能需求

### 6.1 实时性

* 节点状态变更事件延迟 < 50ms。
* 流式 Token 推送不丢包、不乱序（需前端通过 index 排序或后端保证顺序）。

### 6.2 数据存储

* `workflow_node_execution_log` 数据量较大，建议设置保留策略（如保留 7 天），或将历史数据归档至对象存储/冷备库。

### 6.3 安全性

* 在非 Debug 模式下，**严禁**将节点的 `inputs/outputs` 敏感数据推送到前端 SSE 流中，必须在后端进行字段过滤。

```

```