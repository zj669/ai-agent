# I-012 spring-boot-run-stale-snapshot

## 故障签名

- 源码已经修复，但运行日志仍显示旧行为。
- 示例：已修复 TOOL 节点工具名解析后，后端日志仍出现 `Executing MCP tool: null` 或 `mcpToolName not configured`。

## 根因

本地后端通过以下方式启动时：

```bash
mvn spring-boot:run -pl ai-agent-interfaces -Dspring-boot.run.profiles=local -DskipTests
```

运行 classpath 只有 `ai-agent-interfaces/target/classes` 使用当前工作区编译产物，其它模块从本地 Maven 仓库读取 SNAPSHOT jar。只编译 `ai-agent-infrastructure/target/classes` 不会影响运行进程。

## 修复规则

修改 interfaces 以外模块后，先安装 reactor 依赖：

```bash
mvn -pl ai-agent-interfaces -am -DskipTests install
```

然后重启 8080 后端进程。

## 验证

- 检查启动日志来自重启后的进程。
- 复测时确认日志不再出现旧错误签名。
- 对 workflow/MCP 链路优先使用 Browser Relay 真实浏览器验证。
