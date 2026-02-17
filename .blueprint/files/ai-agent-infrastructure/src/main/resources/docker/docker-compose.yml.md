## Metadata
- file: `.blueprint/files/ai-agent-infrastructure/src/main/resources/docker/docker-compose.yml.md`
- source: `ai-agent-infrastructure/src/main/resources/docker/docker-compose.yml`
- version: `1.1`
- status: 正常
- updated_at: 2026-02-15
- owner: backend-blueprint-shadow

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: docker-compose 基础依赖编排
- 源文件: `ai-agent-infrastructure/src/main/resources/docker/docker-compose.yml`
- 文件类型: `.yml`
- 说明:
  - 定义 AI Agent 后端本地/测试环境所需基础中间件：MySQL、Redis、MinIO、etcd、Milvus。
  - 通过统一网络与持久卷配置，保障服务间连通性、数据持久化与可重复启动。
  - 提供容器级健康检查与依赖顺序控制（尤其 Milvus 依赖 etcd/minio）。

## 2) 核心方法
- `orchestrateDataPlane()`：编排数据库、缓存、对象存储、向量库等数据平面依赖。
- `enforceHealthAndDependency()`：通过 healthcheck + depends_on 约束启动就绪链路。
- `stabilizePersistenceAndNetwork()`：统一 volumes/networks，确保重启后状态连续。

## 3) 具体方法
### 3.1 `orchestrateDataPlane()`
- 函数签名: `orchestrateDataPlane(): ComposeServices`
- 入参:
  - 无（通过环境变量注入账户、密码、端口默认值）
- 出参:
  - `ComposeServices` - mysql/redis/minio/etcd/milvus 服务集合
- 功能含义:
  - 在单一 compose 文件中声明后端依赖服务与运行参数，支持一键启动基础设施。
- 链路作用:
  - 上游: 本地开发、联调、测试环境初始化
  - 下游: 后端应用 `application-*.yml` 中的中间件连接配置

### 3.2 `enforceHealthAndDependency()`
- 函数签名: `enforceHealthAndDependency(): StartupGraph`
- 入参:
  - 各服务健康检查命令与间隔配置
- 出参:
  - `StartupGraph` - 依赖就绪有向图
- 功能含义:
  - 通过服务健康检查与 `depends_on.condition: service_healthy` 避免 Milvus 在依赖未就绪时启动失败。
- 链路作用:
  - 上游: `docker compose up -d`
  - 下游: 服务稳定启动与应用连接成功率

### 3.3 `stabilizePersistenceAndNetwork()`
- 函数签名: `stabilizePersistenceAndNetwork(): InfraRuntimeContext`
- 入参:
  - named volumes 与 `ai-agent-network` 配置
- 出参:
  - `InfraRuntimeContext` - 可持久化、可连通的运行上下文
- 功能含义:
  - 抽象并统一存储卷与桥接网络，减少重启、升级、调试过程中的环境漂移。
- 链路作用:
  - 上游: 容器重建/升级操作
  - 下游: 数据连续性与服务寻址稳定性

## 4) 变更记录
- 2026-02-14: 初始化镜像蓝图，自动创建缺失模板。
- 2026-02-15: 回填 docker-compose 编排职责、健康依赖链路与持久化网络语义，清理“待补充”占位文本。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
