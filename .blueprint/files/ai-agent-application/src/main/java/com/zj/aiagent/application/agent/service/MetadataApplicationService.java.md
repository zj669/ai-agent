# Blueprint Mirror: ai-agent-application/src/main/java/com/zj/aiagent/application/agent/service/MetadataApplicationService.java

## Metadata
- file: `ai-agent-application/src/main/java/com/zj/aiagent/application/agent/service/MetadataApplicationService.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-application/src/main/java/com/zj/aiagent/application/agent/service/MetadataApplicationService.java
- Type: .java

## Responsibility
- 聚合节点模板元数据：模板定义 + 字段定义 + 模板字段映射。
- 输出前端可直接渲染的 `NodeTemplateDTO` 与按组字段结构。

## Key Symbols / Structure
- `getAllNodeTemplates()`
  - 查询启用模板（按 `sortOrder`）
  - 批量读取字段定义与映射关系
  - 组装 `ConfigFieldDTO`
  - 依据 `groupName` 聚合为 `ConfigFieldGroupDTO`
  - 生成 `NodeTemplateDTO` 列表

## Dependencies
- MyBatis Mapper:
  - `NodeTemplateMapper`
  - `SysConfigFieldDefMapper`
  - `NodeTemplateConfigMappingMapper`
- PO:
  - `NodeTemplatePO`, `SysConfigFieldDefPO`, `NodeTemplateConfigMappingPO`
- DTO:
  - `NodeTemplateDTO`, `ConfigFieldDTO`, `ConfigFieldGroupDTO`

## Notes
- 状态: 正常
- 默认分组名为 `其他`（当 `groupName` 为空）。
