## Metadata
- file: `.blueprint/files/ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/stream/RedisSseStreamPublisher.java.md`
- source: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/stream/RedisSseStreamPublisher.java`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-15
- owner: backend-blueprint-shadow

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: RedisSseStreamPublisher
- `StreamPublisher` 的 Redis 实现，按节点生命周期发布 START/UPDATE/FINISH/ERROR 事件。
- 基于 `StreamContext` 补齐 execution/node 元数据，统一前端渲染模式（THOUGHT/MARKDOWN/TEXT/JSON_EVENT）。

## 2) 核心方法
- `publishStart()` / `publishDelta(...)` / `publishThought(String thought)`
- `publishFinish(NodeExecutionResult result)`
- `publishError(String errorMessage)`
- `publishData(Object data, String renderMode)`
- `publishEvent(String eventType, Map<String, Object> payload)`

## 3) 具体方法
### 3.1 publishDelta(...)
- 函数签名: `public void publishDelta(String delta)` / `publishDelta(String delta, boolean isThought)`
- 入参: 增量内容、是否 thought
- 出参: 无
- 功能含义: 组装 UPDATE 事件并携带 delta。
- 链路作用: LLM 流式输出实时反馈到前端。

### 3.2 publishFinish(...)
- 函数签名: `public void publishFinish(NodeExecutionResult result)`
- 入参: 节点执行结果
- 出参: 无
- 功能含义: 提取 response/text，发布 FINISH 事件。
- 链路作用: 节点完成态收敛，驱动前端状态更新。

### 3.3 publishError(...)
- 函数签名: `public void publishError(String errorMessage)`
- 入参: 错误消息
- 出参: 无
- 功能含义: 发布 ERROR 事件并设置 renderMode=TEXT。
- 链路作用: 执行异常可观测与用户可见反馈。

## 4) 变更记录
- 2026-02-15: 回填 SSE StreamPublisher 蓝图语义。
- 2026-02-14: 初始化镜像蓝图（自动补缺）。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
