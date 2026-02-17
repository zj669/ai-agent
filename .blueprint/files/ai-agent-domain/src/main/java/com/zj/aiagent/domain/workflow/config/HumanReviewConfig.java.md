# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/config/HumanReviewConfig.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/config/HumanReviewConfig.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/config/HumanReviewConfig.java
- Type: .java
- Status: 正常

## Responsibility
- 人工审核配置对象，定义是否启用审核、提示词、可编辑字段与触发阶段。

## Key Symbols / Structure
- 字段：`enabled`, `prompt`, `editableFields`, `triggerPhase`。

## Dependencies
- `TriggerPhase`

## Notes
- 由 `NodeConfig` 组合使用，控制节点执行前/后的人审流程。
