## Metadata
- file: `.blueprint/files/ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/config/WebSocketConfig.java.md`
- source: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/config/WebSocketConfig.java`
- version: `1.1`
- status: 正常
- updated_at: 2026-02-15
- owner: backend-blueprint-shadow

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: STOMP WebSocket 通道配置
- 源文件: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/config/WebSocketConfig.java`
- 文件类型: `.java`
- 说明:
  - 启用 `@EnableWebSocketMessageBroker` 并实现 `WebSocketMessageBrokerConfigurer`。
  - 配置简单内存 Broker（`/topic`、`/queue`）与应用目的地前缀（`/app`）。
  - 注册 `/ws` 端点并开启 SockJS 回退，支持浏览器端实时消息链路。

## 2) 核心方法
- `configureMessageBroker(...)`：配置服务端消息路由前缀与内存 Broker。
- `registerStompEndpoints(...)`：注册 STOMP 握手入口与跨域/SockJS 策略。

## 3) 具体方法
### 3.1 `configureMessageBroker(...)`
- 函数签名: `public void configureMessageBroker(MessageBrokerRegistry config)`
- 入参:
  - `config` - Spring WebSocket 消息代理注册器
- 出参:
  - `void`
- 功能含义:
  - 启用 `/topic`、`/queue` 两类下行消息通道，并约束客户端上行到 `/app`。
- 链路作用:
  - 上游: WebSocket/STOMP 基础设施启动
  - 下游: 会话标题更新、通知类实时事件路由

### 3.2 `registerStompEndpoints(...)`
- 函数签名: `public void registerStompEndpoints(StompEndpointRegistry registry)`
- 入参:
  - `registry` - STOMP 端点注册器
- 出参:
  - `void`
- 功能含义:
  - 暴露 `/ws` 端点，允许跨域并支持 SockJS 兼容连接。
- 链路作用:
  - 上游: 前端建立 WebSocket 连接
  - 下游: STOMP 会话建立与消息双向传输

## 4) 变更记录
- 2026-02-14: 初始化镜像蓝图，自动创建缺失模板。
- 2026-02-15: 回填 WebSocketConfig 真实职责与方法语义，清理“待补充”占位内容。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
