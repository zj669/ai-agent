# Bug Report: 工作流编辑器模块

## 测试环境
- 测试时间: 2026-02-11
- 前端地址: http://localhost:5173
- 测试工具: midscene-web MCP

## 测试工具问题

**严重问题**: midscene-web 工具连接极不稳定，无法完成 E2E 测试。

### 问题描述
- `web_connect` 连接成功后，执行任何操作（Tap、take_screenshot）时立即断开
- 错误信息: `Connection lost, reason: client namespace disconnect`
- 多次重试（>10次）均失败
- 问题出现在 Chrome 扩展与 MCP 服务之间的通信

### 错误日志示例
```
Error: Connection lost, reason: client namespace disconnect
    at BridgeServer.emitCall
    at AgentOverChromeBridge.getUIContext
    at Service.contextRetrieverFn
```

## TC-WF-001: 进入工作流编辑器
- **测试结果**: ⚠️ 无法执行 (工具故障)
- **实际表现**: 无法与页面交互，连接在操作前断开
- **阻塞原因**: midscene-web 连接不稳定

## TC-WF-002: 添加节点到画布
- **测试结果**: ⚠️ 无法执行 (工具故障)
- **实际表现**: 未能进入工作流编辑器页面
- **阻塞原因**: 依赖 TC-WF-001，且工具连接问题未解决

## Bug 汇总

| Bug ID | 严重程度 | 模块 | 描述 |
|--------|----------|------|------|
| TOOL-001 | Critical | midscene-web | MCP 工具连接不稳定，无法执行任何浏览器操作 |

## 建议

1. **检查 midscene-web 配置**: 确认 Chrome 扩展已正确安装并启用
2. **检查浏览器状态**: 确保 Chrome 浏览器正常运行，无其他扩展冲突
3. **重启 MCP 服务**: 尝试重启 midscene-web MCP 服务
4. **手动测试**: 在工具修复前，建议进行手动测试

## 后续行动

需要解决 midscene-web 工具的连接稳定性问题后，才能继续执行工作流编辑器的 E2E 测试。
