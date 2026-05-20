# MCP 传输层重构与 SSE 解析优化

## Goal

对现有 MCP 连接逻辑进行深度重构，采用 **策略模式 (Strategy Pattern)** 和 **工厂模式 (Factory Pattern)**，解决 `event:` 标签解析失败导致的"无法获取工具"问题，并为未来支持 WebSocket 等更多传输协议打下基础。

## Requirements

### 1. 传输策略接口
- 新建 `IMcpTransport` 接口，定义核心契约：
  - `List<McTool> discoverTools()` — 发现工具列表
  - `String executeTool(String toolName, Map<String, Object> arguments)` — 执行工具调用

### 2. 传输策略实现类
- **`StdioMcpTransport`** — 处理本地进程 stdio 通信
- **`HttpMcpTransport`** — 处理标准 HTTP POST JSON-RPC
- **`SseMcpTransport`** — [重点修复] 处理 SSE 流式响应，内置健壮行解析逻辑

### 3. 传输工厂
- **`McpTransportFactory`** — 根据 `McpServerConfig` 的传输类型动态生成对应传输实例

### 4. 重构 McpConnectionPool
- 去除 `McpConnectionPool` 中的 `if-else` 臃肿逻辑
- 通过工厂获取传输策略并委托执行

### 5. SSE 解析优化
在 `SseMcpTransport` 中，抛弃脆弱的字符串包含检查，采用逐行扫描：
- 识别并跳过 `event:` 行
- 累加所有 `data:` 行内容
- 仅在获取完整 JSON 结构后才送入 `ObjectMapper` 解析

### 6. 单元测试
为 `SseMcpTransport` 编写测试用例，覆盖：
- 带 `event:` 指令的标准流
- 紧凑格式 `data:{...}`
- 多行 `data:` 分块传输
- 空行和注释行处理

## Acceptance Criteria

- [ ] `IMcpTransport.java` 接口已创建，位于 `infrastructure/mcp/transport/`
- [ ] `McpTransportFactory.java` 已创建，支持 STDIO/HTTP/SSE 三种传输类型
- [ ] `StdioMcpTransport.java`、`HttpMcpTransport.java`、`SseMcpTransport.java` 均已实现
- [ ] `McpConnectionPool.java` 已重构为委托模式，不再包含 `if-else` 判断传输类型
- [ ] `SseMcpTransport` 的 SSE 行解析能正确处理 `event:` 指令和 `data:` 多行合并
- [ ] `SseMcpTransportTest.java` 单元测试通过，覆盖所有边界场景
- [ ] `mvn clean install -DskipTests` 编译通过
- [ ] 手动验证：重启后端，触发 MCP 服务连接，日志输出 `discovered X tools`

## Technical Notes

- 所有新类放在 `infrastructure/mcp/transport/` 包下
- 遵循 DDD 规范，基础设施层代码
- 使用 `@RequiredArgsConstructor` + `final` 字段注入依赖
- 使用 `@Slf4j` 记录日志，带 `[MCP]` 模块标签
- 现有 `McpConnectionPool` 中已使用的依赖（如 `ObjectMapper`、`McpServerConfig`）需保留并正确注入
- 不引入新的外部依赖，使用现有项目的工具类
