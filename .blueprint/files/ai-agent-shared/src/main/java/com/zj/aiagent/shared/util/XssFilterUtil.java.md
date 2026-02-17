# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/util/XssFilterUtil.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/util/XssFilterUtil.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/util/XssFilterUtil.java
- Type: .java

## Responsibility
- 提供 XSS 风险内容检测、过滤与 HTML 清理能力。
- 用于基础输入净化，不承担完整安全网关职责。

## Key Symbols / Structure
- `XSS_PATTERNS`：危险脚本/标签/事件正则集合。
- `filter(String)`：执行危险模式移除与实体转义。
- `containsXss(String)`：检测输入是否命中危险模式。
- `stripHtml(String)`：移除 HTML 标签。
- `escapeHtml(String)`：内部字符实体转义。

## Dependencies
- JDK `Pattern` 与字符串处理。

## Notes
- 基于黑名单，适合作为第一层防护。
