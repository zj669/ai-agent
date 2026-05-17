# Metadata API

## 概述
Metadata 模块提供工作流节点类型的元数据查询接口，用于前端工作流编辑器获取可用节点类型、配置字段和默认值。

## 认证
`/api/meta/**` 当前在 `WebMvcConfig` 中放行，可作为前端工作流编辑器的公共元数据入口。

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
                "fieldId": 1,
                "fieldKey": "name",
                "fieldLabel": "节点名称",
                "fieldType": "INPUT",
                "isRequired": 1,
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
                "fieldId": 10,
                "fieldKey": "llmConfigId",
                "fieldLabel": "模型配置",
                "fieldType": "SELECT",
                "isRequired": 1,
                "options": []
              },
              {
                "fieldId": 11,
                "fieldKey": "temperature",
                "fieldLabel": "温度",
                "fieldType": "NUMBER",
                "defaultValue": "0.7"
              }
            ]
          },
          {
            "groupName": "提示词配置",
            "fields": [
              {
                "fieldId": 12,
                "fieldKey": "systemPrompt",
                "fieldLabel": "系统提示词",
                "fieldType": "TEXTAREA",
                "isRequired": 0
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
- **KNOWLEDGE** - 知识库检索节点

## 字段说明
- **defaultSchemaPolicy**：节点创建时的默认输入/输出 schema 策略
- **initialSchema**：节点创建时的初始 schema
- **configFieldGroups**：配置字段分组，用于前端渲染配置表单
- **fieldKey**：字段写入 `userConfig` 时使用的 key
- **fieldType**：字段类型，来自 `sys_config_field_def.field_type`
- **isRequired**：当前模板映射上的必填标记，`1` 表示必填
- **category**：节点分类，例如 `CONTROL`、`AI`、`INTEGRATION`
- **sortOrder**：节点在工具栏中的排序顺序

## 数据来源

当前元数据由三张初始化表联查生成：

1. `node_template`
2. `sys_config_field_def`
3. `node_template_config_mapping`

初始化 SQL：`docker/init/mysql/01_init_schema.sql`

## 更新记录

- 2026-05-14：对齐当前 DTO 字段名，补充 `KNOWLEDGE` 节点，并说明 `/api/meta/**` 当前被认证拦截器放行。
