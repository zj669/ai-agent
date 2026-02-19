# Dashboard API

## 概述
Dashboard 模块提供系统统计数据查询接口，用于展示用户的智能体、对话、知识库和工作流执行情况的统计信息。

## 认证
所有接口需要 JWT Token 认证（Header: `Authorization: Bearer {token}`）

## 接口列表

### 获取统计数据
- **路径**：`GET /api/dashboard/stats`
- **描述**：获取当前用户的系统统计数据，包括资源数量和执行情况
- **请求参数**：无

- **响应示例**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "agentCount": 5,
      "workflowCount": 5,
      "conversationCount": 23,
      "knowledgeDatasetCount": 3,
      "totalExecutions": 156,
      "successfulExecutions": 142,
      "failedExecutions": 14,
      "avgResponseTime": 1250.5
    }
  }
  ```

## 字段说明
- **agentCount**：用户创建的智能体总数
- **workflowCount**：工作流数量（当前等于 agentCount，因为每个 Agent 对应一个工作流）
- **conversationCount**：对话总数
- **knowledgeDatasetCount**：知识库数据集总数
- **totalExecutions**：工作流总执行次数
- **successfulExecutions**：成功执行次数
- **failedExecutions**：失败执行次数
- **avgResponseTime**：平均响应时间（单位：毫秒）

## 使用场景
该接口通常用于：
- Dashboard 首页数据展示
- 用户使用情况概览
- 系统健康度监控
