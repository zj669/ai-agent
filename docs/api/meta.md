# Metadata API

## 概述
Metadata 模块提供工作流节点类型的元数据查询接口，用于前端工作流编辑器获取可用节点类型、配置字段和默认值。

## 认证
所有接口需要 JWT Token 认证（Header: `Authorization: Bearer {token}`）

## 接口列表

### 获取节点模板列表
- **路径**：`GET /api/meta/node-templates`
- **描述**：获取所有可用的工作流节点模板，包含节点类型、配置字段、默认值等元数据
- **请求参数**：无

- **响应示例**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": [
      {
        "id": 1,
        "typeCode": "START",
        "name": "开始节点",
        "description": "工作流起始节点",
        "icon": "play-circle",
        "category": "CONTROL",
        "sortOrder": 1,
        "defaultSchemaPolicy": {
          "type": "object",
          "properties": {}
        },
        "initialSchema": {},
        "configFieldGroups": [
          {
            "groupName": "基础配置",
            "fields": [
              {
                "fieldName": "name",
                "label": "节点名称",
                "type": "string",
                "required": true,
                "defaultValue": "开始"
              }
            ]
          }
        ]
      },
      {
        "id": 2,
        "typeCode": "LLM",
        "name": "LLM 节点",
        "description": "调用大语言模型",
        "icon": "robot",
        "category": "AI",
        "sortOrder": 10,
        "defaultSchemaPolicy": {
          "type": "object",
          "properties": {
            "model": { "type": "string" },
            "prompt": { "type": "string" }
          }
        },
        "initialSchema": {
          "model": "gpt-4",
          "temperature": 0.7
        },
        "configFieldGroups": [
          {
            "groupName": "模型配置",
            "fields": [
              {
                "fieldName": "model",
                "label": "模型名称",
                "type": "select",
                "required": true,
                "options": ["gpt-4", "gpt-3.5-turbo"]
              },
              {
                "fieldName": "temperature",
                "label": "温度",
                "type": "number",
                "defaultValue": 0.7
              }
            ]
          },
          {
            "groupName": "提示词配置",
            "fields": [
              {
                "fieldName": "prompt",
                "label": "提示词模板",
                "type": "textarea",
                "required": true
              }
            ]
          }
        ]
      }
    ]
  }
  ```

### 获取节点类型列表
- **路径**：`GET /api/meta/node-types`
- **描述**：获取节点类型列表（与 `/api/meta/node-templates` 功能相同，提供别名接口）
- **请求参数**：无

- **响应示例**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": [
      {
        "id": 1,
        "typeCode": "START",
        "name": "开始节点",
        "description": "工作流起始节点",
        "icon": "play-circle",
        "category": "CONTROL",
        "sortOrder": 1,
        "defaultSchemaPolicy": {},
        "initialSchema": {},
        "configFieldGroups": []
      }
    ]
  }
  ```

## 节点类型说明
系统支持以下节点类型（typeCode）：
- **START** - 工作流起始节点
- **END** - 工作流结束节点
- **LLM** - 大语言模型调用节点
- **CONDITION** - 条件分支节点（支持 EXPRESSION 和 LLM 两种模式）
- **HTTP** - HTTP 请求节点
- **TOOL** - 工具调用节点

## 字段说明
- **defaultSchemaPolicy**：节点配置的 JSON Schema 定义，用于前端表单验证
- **initialSchema**：节点创建时的默认配置值
- **configFieldGroups**：配置字段分组，用于前端渲染配置表单
- **category**：节点分类（CONTROL、AI、INTEGRATION 等）
- **sortOrder**：节点在工具栏中的排序顺序
