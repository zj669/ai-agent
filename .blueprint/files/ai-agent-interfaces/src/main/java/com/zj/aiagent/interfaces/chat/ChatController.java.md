## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/ChatController.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: ChatController.java
- 对外提供会话创建、会话分页、消息分页与会话删除接口，负责分页排序参数解析和 DTO 映射。

## 2) 核心方法
- `createConversation(String userId, String agentId)`
- `getConversations(String userId, String agentId, int page, int size)`
- `getMessages(String conversationId, String userId, int page, int size, String order)`
- `deleteConversation(String conversationId, String userId)`

## 3) 具体方法
### 3.1 getMessages(...)
- 函数签名: `getMessages(String conversationId, String userId, int page, int size, String order) -> List<MessageResponse>`
- 入参: 会话标识、用户标识、分页和排序参数
- 出参: 消息响应列表
- 功能含义: 构造 `Pageable`（按 `createdAt` 升/降序），调用带权限校验的应用服务并映射为接口 DTO。
- 链路作用: 历史消息查询 -> 权限校验 -> 领域消息 -> API 响应对象。

## 4) 变更记录
- 2026-02-15: 基于源码回填聊天控制器接口职责与分页映射逻辑。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
