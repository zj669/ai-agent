# Blueprint Mirror: ai-agent-application/src/main/java/com/zj/aiagent/application/agent/dto/ConfigFieldDTO.java

## Metadata
- file: `ai-agent-application/src/main/java/com/zj/aiagent/application/agent/dto/ConfigFieldDTO.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-application/src/main/java/com/zj/aiagent/application/agent/dto/ConfigFieldDTO.java
- Type: .java

## Responsibility
- 节点模板配置字段 DTO，承载字段定义与模板映射覆盖信息。

## Key Symbols / Structure
- 字段定义：`fieldId/fieldKey/fieldLabel/fieldType/options/defaultValue/placeholder/description/validationRules`
- 映射覆盖：`groupName/sortOrder/overrideDefault/isRequired`

## Dependencies
- Jackson `JsonNode`
- Lombok `@Data`

## Notes
- 状态: 正常
- 用于元数据聚合后返回给前端配置面板。
