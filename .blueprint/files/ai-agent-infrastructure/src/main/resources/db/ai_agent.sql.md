## Metadata
- file: `.blueprint/files/ai-agent-infrastructure/src/main/resources/db/ai_agent.sql.md`
- source: `ai-agent-infrastructure/src/main/resources/db/ai_agent.sql`
- version: `1.1`
- status: 正常
- updated_at: 2026-02-15
- owner: backend-blueprint-shadow

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: AI Agent 全量数据库结构基线
- 源文件: `ai-agent-infrastructure/src/main/resources/db/ai_agent.sql`
- 文件类型: `.sql`
- 说明:
  - 维护 `ai_agent` 数据库的全量 DDL 基线（含 drop/create、索引、外键、注释）。
  - 覆盖用户、智能体、会话、知识库、元数据、工作流执行等核心业务表。
  - 作为本地开发与环境对齐的结构真源，支持 schema 重建与表结构审计。

## 2) 核心方法
- `resetAndCreateSchema()`：通过 drop + create 重建表结构与约束。
- `defineDomainTableContracts()`：定义各业务域表字段、索引与关系契约。
- `guardSqlExecutionContext()`：设置 `utf8mb4` 与外键开关，确保脚本执行上下文稳定。

## 3) 具体方法
### 3.1 `resetAndCreateSchema()`
- 函数签名: `resetAndCreateSchema(): DdlBatch`
- 入参:
  - 无
- 出参:
  - `DdlBatch` - 按顺序执行的 DDL 语句集合
- 功能含义:
  - 先 `DROP TABLE IF EXISTS` 再 `CREATE TABLE`，保证重复执行可获得一致结构。
- 链路作用:
  - 上游: 本地初始化、手动重置数据库
  - 下游: MyBatis Mapper 与 Repository 的持久层契约

### 3.2 `defineDomainTableContracts()`
- 函数签名: `defineDomainTableContracts(): DomainSchemaSet`
- 入参:
  - 无
- 出参:
  - `DomainSchemaSet` - 按领域划分的表结构定义
- 功能含义:
  - 统一声明 `agent_info/agent_version`、`conversations/messages`、`knowledge_*`、`workflow_*` 等表结构与关键索引。
- 链路作用:
  - 上游: 领域模型与持久化对象设计
  - 下游: 查询性能、数据一致性与跨表关联能力

### 3.3 `guardSqlExecutionContext()`
- 函数签名: `guardSqlExecutionContext(): SqlRuntimeGuard`
- 入参:
  - 无
- 出参:
  - `SqlRuntimeGuard` - SQL 执行上下文约束
- 功能含义:
  - 设置 `SET NAMES utf8mb4`、`SET FOREIGN_KEY_CHECKS`，避免字符集和依赖关系导致脚本失败。
- 链路作用:
  - 上游: SQL 客户端/导入工具
  - 下游: 脚本可重复执行与数据完整性校验

## 4) 变更记录
- 2026-02-14: 初始化镜像蓝图，自动创建缺失模板。
- 2026-02-15: 回填 ai_agent.sql 真实职责与结构语义，清理“待补充”占位内容。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
