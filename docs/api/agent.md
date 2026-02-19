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

## 状态码说明
- **status 字段**：
  - `0` - DRAFT（草稿）
  - `1` - PUBLISHED（已发布）
  - `2` - ARCHIVED（已归档）
