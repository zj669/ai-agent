# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/model/enums/IBaseEnum.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/model/enums/IBaseEnum.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/model/enums/IBaseEnum.java
- Type: .java

## Responsibility
- 定义业务枚举统一契约（`value`/`label`）与常用映射工具。
- 统一不同枚举类型的值、标签互转方式。

## Key Symbols / Structure
- `getValue()` / `getLabel()`：枚举实现接口。
- `getEnumByValue(...)`：值转枚举。
- `getEnumByLabel(...)`：标签转枚举。
- `getLabelByValue(...)`：值转标签。
- `getValueByLabel(...)`：标签转值。

## Dependencies
- JDK `EnumSet`、`Objects`。

## Notes
- 泛型约束：`E extends Enum<E> & IBaseEnum`。
