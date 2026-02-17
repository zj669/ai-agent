## Metadata
- file: `.blueprint/files/ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/adapter/RedisWorkflowCancellationAdapter.java.md`
- source: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/adapter/RedisWorkflowCancellationAdapter.java`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-15
- owner: backend-blueprint-shadow

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: RedisWorkflowCancellationAdapter
- 实现 `WorkflowCancellationPort`，在 Redis 写入/查询取消标记实现跨线程取消感知。
- 使用 `workflow:cancel:{executionId}` 键并设置 TTL 防止长期堆积。

## 2) 核心方法
- `markAsCancelled(String executionId)`
- `isCancelled(String executionId)`

## 3) 具体方法
### 3.1 markAsCancelled(...)
- 函数签名: `public void markAsCancelled(String executionId)`
- 入参: executionId
- 出参: 无
- 功能含义: 写入值 `true` 且 TTL=1h。
- 链路作用: stop/cancel 请求下发后的持久化标记步骤。

### 3.2 isCancelled(...)
- 函数签名: `public boolean isCancelled(String executionId)`
- 入参: executionId
- 出参: 是否已取消
- 功能含义: 检查 Redis 键是否存在，异常时降级为 false。
- 链路作用: 节点推进前的中断判定依据。

## 4) 变更记录
- 2026-02-15: 回填取消适配器蓝图语义，补齐 TTL 与降级策略说明。
- 2026-02-14: 初始化镜像蓝图（自动补缺）。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
