# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/exception/ConditionConfigurationException.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/exception/ConditionConfigurationException.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/exception/ConditionConfigurationException.java
- Type: .java (exception)
- Status: 正常

## Responsibility
- 条件分支配置异常类型，用于表示条件节点分支定义不合法（如 default 分支缺失或重复）。

## Key Symbols / Structure
- 构造器：
  - `ConditionConfigurationException(String message)`
  - `ConditionConfigurationException(String message, Throwable cause)`

## Dependencies
- `RuntimeException`

## Notes
- 由条件评估与配置校验流程抛出，阻止非法配置进入执行期。
