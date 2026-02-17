## Metadata
- file: `.blueprint/files/ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/persistence/WorkflowNodeExecutionLogRepositoryImpl.java.md`
- source: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/persistence/WorkflowNodeExecutionLogRepositoryImpl.java`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-15
- owner: backend-blueprint-shadow

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: WorkflowNodeExecutionLogRepositoryImpl
- 实现节点执行日志仓储，负责日志实体的 MySQL 持久化、查询与序列化字段映射。
- 通过 `ObjectMapper` 将 inputs/outputs 在 Domain Map 与 PO JsonNode 间转换。

## 2) 核心方法
- `save(WorkflowNodeExecutionLog logDomain)`
- `findByExecutionId(String executionId)`
- `findByExecutionIdAndNodeId(String executionId, String nodeId)`
- `findByExecutionIdOrderByEndTime(String executionId)`
- `toPO(...)` / `toDomain(...)`

## 3) 具体方法
### 3.1 save(...)
- 函数签名: `public void save(WorkflowNodeExecutionLog logDomain)`
- 入参: 节点执行日志领域对象
- 出参: 无（回填日志主键）
- 功能含义: 领域对象转 PO 后写库，并回写自增 ID。
- 链路作用: 节点执行生命周期可追踪的落库入口。

### 3.2 findByExecutionId(...)
- 函数签名: `public List<WorkflowNodeExecutionLog> findByExecutionId(String executionId)`
- 入参: 执行ID
- 出参: 日志列表
- 功能含义: 按 startTime 升序查询并映射回领域对象。
- 链路作用: 执行详情与回放视图的核心数据源。

## 4) 变更记录
- 2026-02-15: 回填节点日志仓储蓝图语义，补齐序列化映射说明。
- 2026-02-14: 初始化镜像蓝图（自动补缺）。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
