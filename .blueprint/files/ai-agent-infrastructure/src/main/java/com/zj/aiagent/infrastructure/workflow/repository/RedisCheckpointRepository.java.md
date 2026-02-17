## Metadata
- file: `.blueprint/files/ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/repository/RedisCheckpointRepository.java.md`
- source: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/repository/RedisCheckpointRepository.java`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-15
- owner: backend-blueprint-shadow

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: RedisCheckpointRepository
- 实现 `CheckpointRepository`，负责执行检查点（含暂停点）在 Redis 的序列化存取与过期管理。
- 同时维护通用 checkpoint key 与 pause key，提升恢复流程检索效率。

## 2) 核心方法
- `save(Checkpoint checkpoint)`
- `findLatest(String executionId)`
- `findPausePoint(String executionId)`
- `deleteByExecutionId(String executionId)`

## 3) 具体方法
### 3.1 save(...)
- 函数签名: `public void save(Checkpoint checkpoint)`
- 入参: 检查点对象
- 出参: 无
- 功能含义: 写入 `workflow:checkpoint:*`，若为暂停点额外写入 `workflow:pause:*`。
- 链路作用: 暂停/恢复与故障恢复能力的状态快照基础。

### 3.2 findLatest(...)
- 函数签名: `public Optional<Checkpoint> findLatest(String executionId)`
- 入参: 执行ID
- 出参: 最新检查点
- 功能含义: 按 pattern 扫描键并选取最大 key 对应值反序列化。
- 链路作用: resume 执行时定位最近可恢复位置。

### 3.3 findPausePoint(...)
- 函数签名: `public Optional<Checkpoint> findPausePoint(String executionId)`
- 入参: 执行ID
- 出参: 暂停点
- 功能含义: 直接查询 pause key 并反序列化。
- 链路作用: 人工审核/手动暂停场景的快速恢复入口。

## 4) 变更记录
- 2026-02-15: 回填检查点仓储蓝图语义，补齐 pause key 设计说明。
- 2026-02-14: 初始化镜像蓝图（自动补缺）。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
