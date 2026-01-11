# AI-Agent Agent管理模块需求文档

## 1. 模块概述

### 1.1 模块名称
Agent管理模块（Agent Context）

### 1.2 模块定位
支撑域（Supporting Domain）- 管理智能代理的配置与生命周期

### 1.3 核心价值
- 用户可视化创建和配置 AI Agent
- 管理 Agent 从草稿到发布的完整生命周期
- 提供工作流图的定义与存储能力

---

## 2. 功能需求

### 2.1 查询用户Agent列表

**功能描述**：获取当前登录用户创建的所有 Agent 列表。

**业务规则**：
- 只返回当前用户拥有的 Agent
- 按更新时间倒序排列

**输入参数**：无（从 Token 解析用户ID）

**输出**：
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | Agent ID |
| name | String | Agent 名称 |
| description | String | Agent 描述 |
| status | Integer | 状态（0:草稿 1:已发布 2:已停用） |
| createTime | DateTime | 创建时间 |
| updateTime | DateTime | 更新时间 |

**接口路径**：`GET /client/agent/list`

---

### 2.2 查询Agent详情

**功能描述**：获取指定 Agent 的完整配置信息，包含工作流图定义。

**业务规则**：
- 鉴权：只有 Agent 所有者可查看
- 返回完整的 graphJson（工作流图 JSON 定义）

**输入参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| agentId | Long | 是 | Agent ID |

**输出**：Agent 完整信息 + graphJson

**接口路径**：`GET /client/agent/{agentId}`

---

### 2.3 保存Agent

**功能描述**：创建新 Agent 或更新现有 Agent 的配置。

**业务规则**：
- 新建：agentId 为空时创建新 Agent，状态默认为"草稿"
- 更新：agentId 有值时更新现有 Agent
- 鉴权：只有所有者可更新
- graphJson 包含完整的 DAG 工作流定义

**输入参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| agentId | String | 否 | Agent ID（更新时必填） |
| name | String | 是 | Agent 名称 |
| description | String | 否 | Agent 描述 |
| graphJson | String | 是 | 工作流图 JSON 定义 |
| status | Integer | 否 | 状态 |

**输出**：Agent ID

**接口路径**：`POST /client/agent/save`

---

### 2.4 删除Agent

**功能描述**：删除指定的 Agent。

**业务规则**：
- 鉴权：只有所有者可删除
- 物理删除（非软删除）
- 删除后相关对话历史保留

**输入参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| agentId | Long | 是 | Agent ID |

**输出**：成功/失败状态

**接口路径**：`DELETE /client/agent/{agentId}`

---

### 2.5 发布Agent

**功能描述**：将 Agent 从草稿状态发布为可执行状态。

**业务规则**：
- 只有草稿状态可发布
- 发布前校验 graphJson 有效性
- 发布后 Agent 可被用于对话

**输入参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| agentId | Long | 是 | Agent ID |

**输出**：成功/失败状态

**接口路径**：`POST /client/agent/{agentId}/publish`

---

## 3. 工作流图定义（graphJson）

### 3.1 整体结构

```json
{
  "dagId": "dag-xxx",
  "version": "3.0",
  "description": "Agent描述",
  "startNodeId": "start",
  "nodes": [...],
  "edges": [...]
}
```

### 3.2 节点定义（NodeDefinition）

每个节点包含 **5 个核心配置层**：

| 层级 | 字段 | 类型 | 说明 |
|------|------|------|------|
| 1️⃣ | nodeId | String | 节点唯一标识 |
| 2️⃣ | nodeName | String | 节点显示名称 |
| 3️⃣ | inputSchema | List\<FieldSchema\> | 输入字段定义（声明节点期望的输入） |
| 4️⃣ | outputSchema | List\<FieldSchema\> | 输出字段定义（声明节点产出的输出） |
| 5️⃣ | userConfig | NodeUserConfig | 用户可配置项 |

**附加字段**：
| 字段 | 类型 | 说明 |
|------|------|------|
| nodeType | String | 节点类型枚举 |
| templateId | String | 可选，引用节点模板 |
| position | Position | 前端可视化位置 |

---

### 3.3 FieldSchema 定义

`inputSchema` 和 `outputSchema` 中的字段结构：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| key | String | 是 | 字段键名 |
| label | String | 是 | 显示标签 |
| type | String | 是 | 数据类型：`string`、`number`、`boolean`、`array`、`object` |
| description | String | 否 | 字段描述 |
| required | Boolean | 否 | 是否必填，默认 false |
| defaultValue | Any | 否 | 默认值 |
| sourceRef | String | 否 | 数据来源引用（仅 inputSchema），如 `state.user_input`、`node.llm-1.output.result` |
| reducerType | String | 否 | 合并策略：`overwrite`、`append`、`addMessages` |

---

### 3.4 NodeUserConfig 定义

用户可配置的节点参数，按节点类型区分：

#### 通用配置

| 字段 | 类型 | 说明 |
|------|------|------|
| timeout | Integer | 超时时间（秒） |
| maxRetries | Integer | 最大重试次数 |

#### LLM 节点配置

| 字段 | 类型 | 说明 |
|------|------|------|
| model | String | 模型名称（如 gpt-4） |
| temperature | Double | 温度（0-1） |
| maxTokens | Integer | 最大 token 数 |
| systemPrompt | String | 系统提示词 |
| userPromptTemplate | String | 用户提示词模板，支持 `{{fieldKey}}` 变量 |

#### 工具节点配置

| 字段 | 类型 | 说明 |
|------|------|------|
| mcpTools | List\<String\> | MCP 工具列表 |
| toolParameters | Map | 工具调用参数 |

#### 人工审核节点配置

| 字段 | 类型 | 说明 |
|------|------|------|
| reviewTitle | String | 审核标题 |
| reviewDescription | String | 审核说明 |
| editableFields | List\<String\> | 可编辑字段列表 |
| timeoutAction | String | 超时处理：`REJECT`、`APPROVE`、`SKIP` |

#### 条件节点配置

| 字段 | 类型 | 说明 |
|------|------|------|
| conditionExpression | String | 条件表达式 |
| branches | List\<Branch\> | 分支定义 |

---

### 3.5 节点类型枚举

| 类型 | 说明 |
|------|------|
| START | 开始节点 |
| END | 结束节点 |
| LLM | 大模型节点 |
| TOOL | 工具节点 |
| HUMAN | 人工介入节点 |
| CONDITION | 条件分支节点 |
| PARALLEL | 并行节点 |

---

### 3.6 完整节点示例

```json
{
  "nodeId": "analysis-001",
  "nodeName": "数据分析节点",
  "nodeType": "LLM",
  "inputSchema": [
    {
      "key": "userMessage",
      "label": "用户消息",
      "type": "string",
      "required": true,
      "sourceRef": "state.user_input",
      "description": "用户输入的原始消息"
    },
    {
      "key": "context",
      "label": "历史上下文",
      "type": "array",
      "required": false,
      "sourceRef": "state.messages",
      "reducerType": "addMessages"
    }
  ],
  "outputSchema": [
    {
      "key": "analysisResult",
      "label": "分析结果",
      "type": "string",
      "description": "AI 生成的分析结果"
    },
    {
      "key": "confidence",
      "label": "置信度",
      "type": "number",
      "description": "分析置信度 0-1"
    }
  ],
  "userConfig": {
    "model": "gpt-4",
    "temperature": 0.7,
    "maxTokens": 2000,
    "systemPrompt": "你是一个数据分析专家...",
    "userPromptTemplate": "请分析以下内容：{{userMessage}}"
  },
  "position": { "x": 300, "y": 200 }
}
```

---

### 3.7 边定义（EdgeDefinition）

```json
{
  "edgeId": "edge-001",
  "source": "node1",
  "target": "node2",
  "label": "成功",
  "condition": "可选的条件表达式",
  "edgeType": "DEPENDENCY"
}
```

**edgeType 枚举**：
| 类型 | 说明 |
|------|------|
| DEPENDENCY | 标准依赖边（默认） |
| LOOP_BACK | 循环边，不参与拓扑排序 |
| CONDITIONAL | 条件边，由节点动态决定是否激活 |

---


## 4. 非功能需求

### 4.1 安全性
- 所有操作需鉴权，确保用户只能操作自己的 Agent

### 4.2 数据校验
- graphJson 格式校验（JSON 解析有效性）
- 图结构校验（无孤立节点、有明确开始结束节点）

### 4.3 性能
- 列表查询响应时间 < 100ms
- 详情查询响应时间 < 50ms

---

## 5. 领域模型

### 5.1 聚合根：Agent

```java
Agent {
    id: Long              // Agent ID
    userId: Long          // 所有者用户ID
    name: String          // Agent 名称
    description: String   // Agent 描述
    graphJson: String     // 工作流图 JSON
    status: AgentStatus   // 状态枚举
    createTime: DateTime  // 创建时间
    updateTime: DateTime  // 更新时间
    
    // 领域行为
    publish()             // 发布
    disable()             // 停用
    updateConfiguration() // 更新配置
    isOwnedBy(userId)     // 权限校验
}
```

### 5.2 值对象

| 值对象 | 说明 |
|--------|------|
| AgentStatus | 枚举：DRAFT(0), PUBLISHED(1), DISABLED(2) |
| GraphDefinition | 封装 graphJson 解析，提供节点/边访问方法 |

---

## 6. 依赖关系

### 6.1 对外提供
- IAgentRepository：Agent 数据访问接口

### 6.2 依赖外部
- 用户认证模块：获取当前用户身份