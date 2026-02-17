## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/config/DataSourceConfig.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: DataSourceConfig.java
- 组装主数据源、`SqlSessionFactory`、事务管理器与 `TransactionTemplate`，承接 interfaces 层到 infrastructure mapper 的数据库访问基础设施。

## 2) 核心方法
- `primaryDataSourceProperties()`
- `primaryDataSource(...)`
- `primarySqlSessionFactory(...)`
- `primaryTransactionManager(...)`

## 3) 具体方法
### 3.1 primarySqlSessionFactory(DataSource, MybatisPlusInterceptor)
- 函数签名: `primarySqlSessionFactory(DataSource dataSource, MybatisPlusInterceptor mybatisPlusInterceptor) -> SqlSessionFactory`
- 入参: 主数据源、MyBatis Plus 拦截器
- 出参: `SqlSessionFactory`
- 功能含义: 设置 mapper XML 扫描、别名包、插件与 `AutoFillConfig` 全局配置，形成主库会话工厂。
- 链路作用: Mapper 调用 -> SqlSessionFactory -> 拦截器/自动填充/事务链统一生效。

## 4) 变更记录
- 2026-02-15: 基于源码回填主数据源与事务组件装配职责。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
