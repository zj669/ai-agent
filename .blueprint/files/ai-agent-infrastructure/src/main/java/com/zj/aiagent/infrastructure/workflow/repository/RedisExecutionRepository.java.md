## Metadata
- file: `.blueprint/files/ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/repository/RedisExecutionRepository.java.md`
- source: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/repository/RedisExecutionRepository.java`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-15
- owner: backend-blueprint-shadow

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: RedisExecutionRepository
- 实现 `ExecutionRepository` 的 Redis 热数据存储，负责执行聚合的保存、查询、更新、删除。
- 维护会话维度二级索引 `workflow:conversation:{conversationId}:executions`，支持按会话回查执行历史。

## 2) 核心方法
- `save(Execution execution)`
- `findById(String executionId)`
- `findByConversationId(String conversationId)`
- `update(Execution execution)`
- `delete(String executionId)`

## 3) 具体方法
### 3.1 save(...)
- 函数签名: `public void save(Execution execution)`
- 入参: 执行聚合
- 出参: 无
- 功能含义: 序列化执行对象写入 Redis，并更新会话索引集合与 TTL。
- 链路作用: 调度器启动执行后落盘热状态的入口。

### 3.2 update(...)
- 函数签名: `public void update(Execution execution)`
- 入参: 已变更执行聚合
- 出参: 无
- 功能含义: 读取旧版本做乐观锁校验（version-1），通过后覆盖写入。
- 链路作用: 防止并发推进下的执行状态覆写冲突。

### 3.3 findByConversationId(...)
- 函数签名: `public List<Execution> findByConversationId(String conversationId)`
- 入参: 会话ID
- 出参: 执行列表（按创建时间倒序）
- 功能含义: 通过二级索引批量读取执行键并反序列化。
- 链路作用: 支撑会话维度执行历史展示与追踪。

## 4) 变更记录
- 2026-02-15: 回填 Redis 执行仓储蓝图语义，补齐索引与并发控制说明。
- 2026-02-14: 初始化镜像蓝图（自动补缺）。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
