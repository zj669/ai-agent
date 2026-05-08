# 基础设施运维业务域索引

本文件为基础设施运维业务域提供二级路由，不重复根级全局安全规则。

## 业务范围

Docker Compose 服务管理（MySQL、Redis、Milvus、MinIO、etcd）、Spring Boot 部署、
数据库初始化与变更、日志排查、健康检查。

## 典型触发

- 启动/重启 Docker 服务
- 数据库 schema 变更
- 健康检查失败排查
- Redis 缓存或 Pub/Sub 问题
- Milvus 向量数据库运维

## 服务地址（本地开发）

| 服务 | 地址 | 用途 |
|---|---|---|
| Spring Boot API | http://localhost:8080 | 后端主服务 |
| Vite 开发服务器 | http://localhost:5173 | 前端开发 |
| MySQL | localhost:13306 | 主数据库 |
| Redis | localhost:6379 | 缓存 + Pub/Sub |
| Milvus | localhost:19530 | 向量数据库 |
| MinIO API | http://localhost:9002 | 对象存储 |
| MinIO Console | http://localhost:9001 | 存储管理界面 |
| etcd | localhost:2379 | Milvus 依赖 |

## Docker Compose 位置

`docker/docker-compose.yml`（相对于工作区根目录）

**重要**：必须使用 `docker compose`（v2 Go 插件），禁止使用 `docker-compose`（v1 Python，与新版 Docker Engine 不兼容）。

## 常用命令（支撑事实）

```bash
# 启动所有服务
cd docker && docker compose up -d

# 查看服务状态
docker compose ps

# 查看单个服务日志
docker compose logs -f <service-name>

# 只重启目标服务（禁止一次性重启全部）
docker compose up -d <service-name>

# 后端健康检查
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/prometheus
curl http://localhost:8080/actuator/info

# 后端构建
mvn clean install -DskipTests
mvn spring-boot:run -pl ai-agent-interfaces -Dspring-boot.run.profiles=local
```

## 数据库 Schema 管理

- 位置：`docker/init/mysql/01_init_schema.sql`（相对于工作区根目录）
- 无 Flyway；schema 通过 Docker MySQL 初始化脚本管理
- 修改 schema 前必须评估对已有数据的影响（`requires-confirmation`）

## 安全边界

- 禁止直接 `docker-compose restart` 全部服务（`requires-confirmation`）
- 禁止删除生产数据库数据（`requires-confirmation`）
- 禁止直接编辑服务器版本控制下的配置文件
- 不要重启 Docker daemon

## SOP 列表

| SOP | 文件 | 状态 |
|---|---|---|
| 本地开发环境启动 | `references/infra-ops/local-dev-startup.md` | 已完成 |
| Docker 服务管理 | `references/infra-ops/docker-ops.md` | 待创建 |
| 数据库变更 | `references/infra-ops/db-change.md` | 待创建 |
| 健康检查排查 | `references/infra-ops/health-triage.md` | 待创建 |
