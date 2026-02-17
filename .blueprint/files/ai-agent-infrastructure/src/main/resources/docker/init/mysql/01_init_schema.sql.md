## Metadata
- file: `.blueprint/files/ai-agent-infrastructure/src/main/resources/docker/init/mysql/01_init_schema.sql.md`
- source: `ai-agent-infrastructure/src/main/resources/docker/init/mysql/01_init_schema.sql`
- version: `1.1`
- status: 正常
- updated_at: 2026-02-15
- owner: backend-blueprint-shadow

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: Docker MySQL 启动初始化脚本
- 源文件: `ai-agent-infrastructure/src/main/resources/docker/init/mysql/01_init_schema.sql`
- 文件类型: `.sql`
- 说明:
  - 在 MySQL 容器首次启动时创建业务基础表（`IF NOT EXISTS`）。
  - 以分模块注释组织用户、智能体、会话、知识库、元数据、工作流日志等核心结构。
  - 写入节点模板初始化数据，确保元数据模块可开箱使用。

## 2) 核心方法
- `bootstrapSchemaForContainer()`：在容器启动期完成基础 DDL 初始化。
- `seedNodeTemplates()`：注入 START/END/LLM/TOOL/CONDITION/HTTP 节点模板。
- `emitInitResult()`：输出初始化成功提示，便于容器日志确认。

## 3) 具体方法
### 3.1 `bootstrapSchemaForContainer()`
- 函数签名: `bootstrapSchemaForContainer(): InitDdlBatch`
- 入参:
  - 无
- 出参:
  - `InitDdlBatch` - 容器初始化阶段 DDL 批次
- 功能含义:
  - 执行 `USE ai_agent`、`SET NAMES utf8mb4`，并对核心表执行 `CREATE TABLE IF NOT EXISTS`。
- 链路作用:
  - 上游: Docker `mysql` 服务 entrypoint init 机制
  - 下游: 后端应用首次启动可直接访问基础结构

### 3.2 `seedNodeTemplates()`
- 函数签名: `seedNodeTemplates(): SeedBatch`
- 入参:
  - 无
- 出参:
  - `SeedBatch` - 元数据种子数据插入语句
- 功能含义:
  - 插入默认节点模板，并通过 `ON DUPLICATE KEY UPDATE` 保证幂等。
- 链路作用:
  - 上游: 元数据表创建完成
  - 下游: 前端节点面板与工作流图构建默认能力

### 3.3 `emitInitResult()`
- 函数签名: `emitInitResult(): InitMessage`
- 入参:
  - 无
- 出参:
  - `InitMessage` - 初始化结果信息
- 功能含义:
  - 通过 `SELECT '...initialized successfully!'` 输出初始化完成标识。
- 链路作用:
  - 上游: SQL 执行流程结束
  - 下游: 运维排障与容器日志健康检查

## 4) 变更记录
- 2026-02-14: 初始化镜像蓝图，自动创建缺失模板。
- 2026-02-15: 回填 01_init_schema.sql 真实职责与初始化语义，清理“待补充”占位内容。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
