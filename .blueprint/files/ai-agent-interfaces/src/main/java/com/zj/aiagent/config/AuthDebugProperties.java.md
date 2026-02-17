## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/config/AuthDebugProperties.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: AuthDebugProperties.java
- 绑定 `auth.debug` 配置并在启动后输出安全提示，用于控制 Debug 认证开关与 Header 名称，是认证拦截链的环境开关入口。

## 2) 核心方法
- `init()`
- `isEnabled()/getHeaderName()`（Lombok 生成访问器）

## 3) 具体方法
### 3.1 init()
- 函数签名: `init() -> void`
- 入参: 无
- 出参: 无
- 功能含义: 在 Bean 初始化后根据 `enabled` 输出警告/提示日志，强调 Debug 认证仅限开发测试环境。
- 链路作用: 配置加载 -> 启动日志提示 -> `LoginInterceptor` 决定是否启用 DEBUG 认证路径。

## 4) 变更记录
- 2026-02-15: 基于源码回填 debug 认证配置职责、字段语义与初始化日志链路。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
