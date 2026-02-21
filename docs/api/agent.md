# Agent API

## 概述
Agent 模块提供智能体的完整生命周期管理，包括创建、更新、发布、版本管理和删除功能。支持工作流图配置（graphJson）和版本历史追溯。

## 认证
所有接口需要 JWT Token 认证（Header: `Authorization: Bearer {token}`）

## 接口列表

### 创建智能体
- **路径**：`POST /api/agent/create`
- **描述**：创建新的智能体，初始版本为 1
- **请求参数**：
  | 参数名 | 类型 | 位置 | 必填 | 说明 |
  |--------|------|------|------|------|
  | name | String | Body | 是 | 智能体名称 |
  | description | String | Body | 否 | 智能体描述 |
  | icon | String | Body | 否 | 图标 URL 或标识 |

- **响应示例**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": 1001
  }
  ```

### 更新智能体
- **路径**：`POST /api/agent/update`
- **描述**：更新智能体信息和工作流图配置，使用乐观锁防止并发冲突
- **请求参数**：
  | 参数名 | 类型 | 位置 | 必填 | 说明 |
  |--------|------|------|------|------|
  | id | Long | Body | 是 | 智能体 ID |
  | name | String | Body | 是 | 智能体名称 |
  | description | String | Body | 否 | 智能体描述 |
  | icon | String | Body | 否 | 图标 URL 或标识 |
  | graphJson | String | Body | 否 | 工作流图 JSON 配置 |
  | version | Integer | Body | 是 | 当前版本号（乐观锁） |

- **响应示例**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": null
  }
  ```

### 发布智能体
- **路径**：`POST /api/agent/publish`
- **描述**：发布智能体当前版本，创建快照并标记为已发布状态
- **请求参数**：
  | 参数名 | 类型 | 位置 | 必填 | 说明 |
  |--------|------|------|------|------|
  | id | Long | Body | 是 | 智能体 ID |

- **响应示例**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": null
  }
  ```

### 回滚智能体
- **路径**：`POST /api/agent/rollback`
- **描述**：将智能体回滚到指定历史版本
- **请求参数**：
  | 参数名 | 类型 | 位置 | 必填 | 说明 |
  |--------|------|------|------|------|
  | id | Long | Body | 是 | 智能体 ID |
  | targetVersion | Integer | Body | 是 | 目标版本号 |

- **响应示例**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": null
  }
  ```

### 删除指定版本
- **路径**：`DELETE /api/agent/{id}/versions/{version}`
- **描述**：删除智能体的指定历史版本（不能删除当前版本和已发布版本）
- **请求参数**：
  | 参数名 | 类型 | 位置 | 必填 | 说明 |
  |--------|------|------|------|------|
  | id | Long | Path | 是 | 智能体 ID |
  | version | Integer | Path | 是 | 版本号 |

- **响应示例**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": null
  }
  ```

### 强制删除智能体
- **路径**：`DELETE /api/agent/{id}/force`
- **描述**：强制删除智能体及其所有版本历史（逻辑删除）
- **请求参数**：
  | 参数名 | 类型 | 位置 | 必填 | 说明 |
  |--------|------|------|------|------|
  | id | Long | Path | 是 | 智能体 ID |

- **响应示例**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": null
  }
  ```

### 获取智能体列表
- **路径**：`GET /api/agent/list`
- **描述**：获取当前用户的所有智能体摘要列表（不包含 graphJson）
- **请求参数**：无

- **响应示例**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": [
      {
        "id": 1001,
        "userId": 1,
        "name": "客服助手",
        "description": "智能客服对话机器人",
        "icon": "robot",
        "status": "PUBLISHED",
        "publishedVersionId": 5,
        "updateTime": "2026-02-18T10:30:00"
      }
    ]
  }
  ```

### 获取智能体详情
- **路径**：`GET /api/agent/{id}`
- **描述**：获取智能体完整详情，包含工作流图配置
- **请求参数**：
  | 参数名 | 类型 | 位置 | 必填 | 说明 |
  |--------|------|------|------|------|
  | id | Long | Path | 是 | 智能体 ID |

- **响应示例**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "id": 1001,
      "name": "客服助手",
      "description": "智能客服对话机器人",
      "icon": "robot",
      "graphJson": "{\"nodes\":[...],\"edges\":[...]}",
      "version": 3,
      "publishedVersionId": 5,
      "status": 1
    }
  }
  ```

### 获取版本历史
- **路径**：`GET /api/agent/{id}/versions`
- **描述**：获取智能体的所有历史版本列表
- **请求参数**：
  | 参数名 | 类型 | 位置 | 必填 | 说明 |
  |--------|------|------|------|------|
  | id | Long | Path | 是 | 智能体 ID |

- **响应示例**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "versions": [
        {
          "id": 101,
          "agentId": 1001,
          "version": 3,
          "description": "优化对话流程",
          "createTime": "2026-02-18T10:30:00"
        },
        {
          "id": 100,
          "agentId": 1001,
          "version": 2,
          "description": "添加知识库检索",
          "createTime": "2026-02-17T15:20:00"
        }
      ]
    }
  }
  ```

## 编排 graphJson 结构与保存链路

### 1. 前端获取节点元数据（节点类型 + 配置字段）
- **请求路径**：`GET /api/meta/node-types`
- **用途**：前端进入编排页面时，先获取可用节点类型和每个节点的动态配置字段，用于渲染右侧配置面板。
- **前端调用位置**：`ai-agent-foward/src/services/metadataService.ts:105`
- **后端接口位置**：`ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/meta/MetadataController.java:37`
- **后端聚合逻辑**：`ai-agent-application/src/main/java/com/zj/aiagent/application/agent/service/MetadataApplicationService.java:37`

返回结构要点（简化）：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "typeCode": "LLM",
      "name": "LLM 节点",
      "configFieldGroups": [
        {
          "groupName": "基础配置",
          "fields": [
            {
              "fieldKey": "model",
              "fieldLabel": "模型",
              "fieldType": "select",
              "isRequired": 1,
              "defaultValue": "gpt-4o-mini"
            }
          ]
        }
      ]
    }
  ]
}
```

### 2. 保存链路（前端组装 -> graphJson 持久化）
- 前端在画布编辑后将节点和边序列化为 `WorkflowGraphDTO`：
  - DTO 定义：`ai-agent-foward/src/types/workflow.ts:60`
  - 构建逻辑：`ai-agent-foward/src/hooks/useWorkflowEditor.ts:148`
- 保存时调用：`POST /api/agent/update`
  - 前端请求位置：`ai-agent-foward/src/services/agentService.ts:36`
  - graphJson 序列化位置：`ai-agent-foward/src/services/workflowService.ts:110`
- 后端接收并更新：
  - Controller：`ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/agent/web/AgentController.java:45`
  - 请求字段：`ai-agent-application/src/main/java/com/zj/aiagent/application/agent/dto/AgentRequest.java:35`
  - 应用服务写入：`ai-agent-application/src/main/java/com/zj/aiagent/application/agent/service/AgentApplicationService.java:80`

### 3. 后端解析与执行时配置读取
1) **解析 graphJson 为领域对象**
- 入口：`ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:110`
- 工厂接口：`ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/service/WorkflowGraphFactory.java:18`
- 工厂实现：`ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/graph/WorkflowGraphFactoryImpl.java:34`

2) **userConfig -> NodeConfig（动态字段映射）**
- 转换器：`ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/graph/converter/NodeConfigConverter.java:48`
- 规则：根据模板和字段映射从 `userConfig` 提取配置，同时保留额外字段以兼容扩展。

3) **执行器读取配置**
- 调度分发：`ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/NodeExecutorFactory.java:36`
- LLM 节点读取 `model/baseUrl/apiKey`：`ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/LlmNodeExecutorStrategy.java:69`
- Condition 节点读取 `routingStrategy/branches`：`ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/ConditionNodeExecutorStrategy.java:85`

### 4. graphJson 完整样例（可直接用于新增 Agent 编排）
```json
{
  "dagId": "dag-demo-001",
  "version": "1.0.0",
  "description": "agent-save-flow-demo",
  "startNodeId": "start-1",
  "nodes": [
    {
      "nodeId": "start-1",
      "nodeName": "开始",
      "nodeType": "START",
      "position": { "x": 120, "y": 120 },
      "inputSchema": [
        {
          "key": "input",
          "label": "用户输入",
          "type": "string",
          "required": true
        }
      ],
      "outputSchema": [
        {
          "key": "input",
          "label": "用户输入",
          "type": "string"
        }
      ],
      "userConfig": {}
    },
    {
      "nodeId": "llm-1",
      "nodeName": "意图识别",
      "nodeType": "LLM",
      "position": { "x": 360, "y": 120 },
      "inputSchema": [
        {
          "key": "input",
          "label": "输入",
          "type": "string",
          "sourceRef": "start-1.output.input"
        }
      ],
      "outputSchema": [
        { "key": "response", "label": "模型输出", "type": "string" },
        { "key": "intent", "label": "意图", "type": "string" }
      ],
      "userConfig": {
        "model": "gpt-4o-mini",
        "baseUrl": "https://api.openai.com/v1",
        "apiKey": "${OPENAI_API_KEY}",
        "systemPrompt": "你是意图识别助手，请输出 intent 字段。",
        "userPromptTemplate": "{{input}}"
      }
    },
    {
      "nodeId": "cond-1",
      "nodeName": "路由判断",
      "nodeType": "CONDITION",
      "position": { "x": 620, "y": 120 },
      "inputSchema": [
        {
          "key": "intent",
          "label": "意图",
          "type": "string",
          "sourceRef": "llm-1.output.intent"
        }
      ],
      "outputSchema": [
        { "key": "selectedTarget", "label": "选中目标", "type": "string" }
      ],
      "userConfig": {
        "routingStrategy": "EXPRESSION",
        "branches": [
          {
            "priority": 1,
            "targetNodeId": "tool-1",
            "description": "命中工具调用",
            "isDefault": false,
            "conditionGroups": [
              {
                "operator": "AND",
                "conditions": [
                  {
                    "leftOperand": "inputs.intent",
                    "operator": "EQUALS",
                    "rightOperand": "TOOL_CALL"
                  }
                ]
              }
            ]
          },
          {
            "priority": 2147483647,
            "targetNodeId": "end-1",
            "description": "默认结束",
            "isDefault": true,
            "conditionGroups": []
          }
        ]
      }
    },
    {
      "nodeId": "tool-1",
      "nodeName": "查询天气工具",
      "nodeType": "TOOL",
      "position": { "x": 900, "y": 60 },
      "inputSchema": [
        {
          "key": "query",
          "label": "查询词",
          "type": "string",
          "sourceRef": "start-1.output.input"
        }
      ],
      "outputSchema": [
        { "key": "toolResult", "label": "工具结果", "type": "string" }
      ],
      "userConfig": {
        "toolName": "weather.search",
        "timeoutMs": 5000
      }
    },
    {
      "nodeId": "end-1",
      "nodeName": "结束",
      "nodeType": "END",
      "position": { "x": 1160, "y": 120 },
      "inputSchema": [
        {
          "key": "finalResult",
          "label": "最终结果",
          "type": "string",
          "sourceRef": "tool-1.output.toolResult"
        }
      ],
      "outputSchema": [],
      "userConfig": {}
    }
  ],
  "edges": [
    {
      "edgeId": "e1",
      "source": "start-1",
      "target": "llm-1",
      "edgeType": "DEPENDENCY"
    },
    {
      "edgeId": "e2",
      "source": "llm-1",
      "target": "cond-1",
      "edgeType": "DEPENDENCY"
    },
    {
      "edgeId": "e3",
      "source": "cond-1",
      "target": "tool-1",
      "edgeType": "CONDITIONAL",
      "label": "命中工具",
      "condition": "#intent == 'TOOL_CALL'"
    },
    {
      "edgeId": "e4",
      "source": "cond-1",
      "target": "end-1",
      "edgeType": "DEFAULT",
      "label": "默认"
    },
    {
      "edgeId": "e5",
      "source": "tool-1",
      "target": "end-1",
      "edgeType": "DEPENDENCY"
    }
  ]
}
```

## 状态码说明
- **status 字段**：
  - `0` - DRAFT（草稿）
  - `1` - PUBLISHED（已发布）
  - `2` - ARCHIVED（已归档）
