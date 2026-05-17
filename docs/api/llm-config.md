# LLM Config API

## 概述

LLM Config 模块提供用户级模型配置管理。工作流 LLM 节点当前优先读取节点配置中的 `llmConfigId`，再兼容旧字段 `model/baseUrl/apiKey` 或 `llm_model/llm_base_url/llm_api_key`。

- Base URL: `/api/llm-config`
- Controller: `LlmConfigController`
- 返回风格: 统一 `Response<T>`
- 认证: 需要登录态，用户 ID 从 `UserContext` 获取

## 接口列表

### 1. 查询配置列表

```http
GET /api/llm-config
```

返回当前用户的模型配置列表。DTO 不包含 `apiKey`。

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "OpenAI 主配置",
      "provider": "openai",
      "baseUrl": "https://api.openai.com/v1",
      "model": "gpt-4o-mini",
      "isDefault": true,
      "status": 1,
      "createdAt": "2026-05-14T10:00:00",
      "updatedAt": "2026-05-14T10:00:00"
    }
  ]
}
```

### 2. 创建配置

```http
POST /api/llm-config
```

请求体：

```json
{
  "name": "OpenAI 主配置",
  "provider": "openai",
  "baseUrl": "https://api.openai.com/v1",
  "apiKey": "sk-***",
  "model": "gpt-4o-mini"
}
```

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": 1
}
```

### 3. 更新配置

```http
PUT /api/llm-config/{id}
```

请求体：

```json
{
  "name": "OpenAI 默认配置",
  "baseUrl": "https://api.openai.com/v1",
  "apiKey": "sk-***",
  "model": "gpt-4o-mini",
  "isDefault": true
}
```

实现备注：

- `isDefault=true` 时，服务会清理同用户其他默认配置。
- DTO 输出不返回 `apiKey`，但当前 SQL 注释说明 MVP 阶段仍是明文存储，后续才考虑加密。

### 4. 删除配置

```http
DELETE /api/llm-config/{id}
```

### 5. 测试配置

```http
POST /api/llm-config/{id}/test
```

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "ok": true,
    "latencyMs": 523,
    "error": null
  }
}
```

## 相关代码

| 位置 | 说明 |
|------|------|
| `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/llm/LlmConfigController.java` | HTTP 接口 |
| `ai-agent-application/src/main/java/com/zj/aiagent/application/llm/LlmConfigService.java` | 应用服务 |
| `ai-agent-domain/src/main/java/com/zj/aiagent/domain/llm/entity/LlmProviderConfig.java` | 领域实体 |
| `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/LlmNodeExecutorStrategy.java` | LLM 节点读取配置并调用模型 |
