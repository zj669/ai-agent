# Blueprint Mirror: ai-agent-application/src/main/java/com/zj/aiagent/application/agent/dto/NodeTemplateDTO.java

## Metadata
- file: `ai-agent-application/src/main/java/com/zj/aiagent/application/agent/dto/NodeTemplateDTO.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-application/src/main/java/com/zj/aiagent/application/agent/dto/NodeTemplateDTO.java
- Type: .java

## Responsibility
- 节点模板聚合 DTO，承载节点元信息与分组后的配置字段。

## Key Symbols / Structure
- 模板字段：`id/typeCode/name/description/icon/category/sortOrder/defaultSchemaPolicy/initialSchema`
- `configFieldGroups`: `List<ConfigFieldGroupDTO>`

## Dependencies
- Jackson `JsonNode`
- `ConfigFieldGroupDTO`
- Lombok `@Data`

## Notes
- 状态: 正常
- 为 workflow 编辑器节点面板提供模板定义。
