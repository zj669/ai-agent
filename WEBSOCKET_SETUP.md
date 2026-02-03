# WebSocket 配置说明

## 问题背景

之前遇到的错误：
```
org.springframework.web.servlet.resource.NoResourceFoundException: No static resource ws/info
```

**原因：** 前端使用了 WebSocket 来接收实时标题更新，但后端没有配置 WebSocket 支持。

## WebSocket vs SSE 的区别

在本项目中：

- **SSE (Server-Sent Events)**：用于流式传输 AI 聊天消息内容
  - 单向推送（服务器 → 客户端）
  - 适合长时间的流式数据传输
  - 端点：`/api/chat/stream`

- **WebSocket**：用于实时事件通知
  - 双向通信
  - 适合实时推送事件（如标题更新）
  - 端点：`/ws`
  - 订阅主题：`/topic/conversation/{conversationId}/title`

## 已完成的配置

### 1. 添加 WebSocket 依赖

在 `ai-agent-infrastructure/pom.xml` 中添加：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### 2. 创建 WebSocket 配置类

文件：`ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/config/WebSocketConfig.java`

- 启用 STOMP over WebSocket
- 注册 `/ws` 端点（使用 SockJS 作为备用）
- 配置消息代理（`/topic` 和 `/queue` 前缀）

### 3. 创建消息推送服务

文件：`ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/chat/WebSocketMessageService.java`

提供方法：
- `sendTitleUpdate(conversationId, title)` - 推送标题更新

### 4. 集成到标题生成流程

在 `AutoTitleListener.java` 中，当标题生成后自动通过 WebSocket 推送给前端。

## 工作流程

1. 用户发送消息
2. AI 回复消息（通过 SSE 流式传输）
3. 触发 `MessageAppendedEvent` 事件
4. `AutoTitleListener` 监听事件，生成标题
5. 通过 `WebSocketMessageService` 推送标题更新
6. 前端 WebSocket 客户端接收更新，刷新 UI

## 前端订阅示例

前端代码（已存在）：
```typescript
// 订阅标题更新
webSocketService.subscribeToTitle(conversationId, (data) => {
    console.log('Received title update:', data);
    // 更新会话列表中的标题
});
```

## 测试方法

1. 启动后端服务
2. 启动前端服务
3. 创建新会话并发送消息
4. 观察浏览器控制台，应该能看到：
   - `[WS] Connected` - WebSocket 连接成功
   - `Received title update: {...}` - 收到标题更新

## 注意事项

### 生产环境配置

在 `WebSocketConfig.java` 中，需要修改跨域配置：
```java
registry.addEndpoint("/ws")
    .setAllowedOriginPatterns("https://yourdomain.com")  // 改为实际域名
    .withSockJS();
```

### 性能考虑

- 当前使用 Spring 内置的简单消息代理（内存）
- 如果需要集群部署，应该使用外部消息代理（如 RabbitMQ 或 Redis）

## 相关文件

- `ai-agent-infrastructure/pom.xml` - 依赖配置
- `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/config/WebSocketConfig.java` - WebSocket 配置
- `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/chat/WebSocketMessageService.java` - 消息推送服务
- `ai-agent-application/src/main/java/com/zj/aiagent/application/chat/listener/AutoTitleListener.java` - 标题生成监听器
- `app/frontend/src/shared/services/websocketService.ts` - 前端 WebSocket 客户端
