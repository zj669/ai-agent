---
name: local-dev-startup
description: >-
  基础设施运维 SOP：本地开发环境一键启动。覆盖 Docker 中间件（MySQL, Redis, Milvus, MinIO, etcd）、
  Spring Boot 后端和 Vite React 前端的完整启动流程，含常见坑点排查。触发词：
  "本地启动", "启动服务", "启动项目", "启动开发环境", "run local", "start dev", "开发环境启动", "拉起服务".
---

# 本地开发环境启动 SOP

## 目的

在开发机上以 `local` 环境启动 AI Agent 平台的完整技术栈，供日常开发调试使用。

Docker、Spring Boot、Vite 等实现细节是支撑事实，业务动作是"启动本地开发环境"。

## 触发条件

- 用户提到"本地启动"、"启动服务"、"启动项目"或等价表述。
- 任务涉及让整个开发环境从零运行起来。
- 部分服务异常退出后需要恢复启动。

## 范围

- repos: `ai-agent` (monorepo)
- services: Docker daemon, MySQL 8.0, Redis 7, Milvus 2.3, MinIO, etcd, Spring Boot 3.4, Vite 7
- ports: 8080 (后端), 5173 (前端), 13306 (MySQL), 6379 (Redis), 19530 (Milvus), 9002 (MinIO API), 9001 (MinIO Console), 2379 (etcd)
- databases: ai_agent (MySQL)
- scripts: 无专项脚本，使用 mvn / npm / docker compose 原生命令
- supporting context: `references/infra-ops/index.md`

## 必要输入

- 无特殊输入。假设开发机已安装 Java 21、Maven 3.8+、Node.js 20+、Docker Engine 24+。
- credential policy: 本地开发凭据已在 `application-local.yml` 中硬编码（仅限开发环境），不得用于生产。

## 开工前读取

- workspace entry: `SKILL.md`
- recent logs: `logs/YYYY-MM.md`
- this SOP: `references/infra-ops/local-dev-startup.md`
- infra-ops index: `references/infra-ops/index.md`

## 停止条件

- Docker daemon 未运行。
- 关键端口 (8080, 5173, 13306, 6379, 19530, 9002) 被非本项目进程占用且无法释放。
- 系统可用内存 < 4GB（Milvus 需要较多内存）。
- 凭据缺失（如 Embedding API Key 未配置）。
- 与另一个已在运行的实例冲突。

## 工作流

### 1. 定界

确认本次启动的**环境**（默认 local）、**范围**（Docker + 后端 + 前端 全栈，或指定部分）、
以及**是否有残留旧进程/容器**需要先清理。

### 2. 只读检查

在启动任何服务前，先收集当前状态：

```bash
# 检查端口占用
ss -tlnp | grep -E "8080|5173|13306|6379|19530|9002|9001"

# 检查已有 Docker 容器状态
docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "ai-agent|maxai"

# 检查 Java/Vite 进程
ps aux | grep -E "(spring-boot|ai-agent|vite)" | grep -v grep

# 检查系统内存
free -h
```

**关键判断**：
- 如果 `maxai` 容器在运行且占用 5173 端口 → 需先 `docker stop maxai && docker update --restart=no maxai`
- 如果 `ai-agent-*` 容器处于 `Exited` 状态 → 用 `docker compose up -d` 重新拉起（非 `docker start` 单个）
- 如果后端/前端进程已存在 → 确认是否为本次需要的实例

### 3. 操作计划

按照**严格顺序**启动三层服务：

| 层级 | 动作 | 风险 | 确认要求 |
|------|------|------|----------|
| 1. Docker 中间件 | `docker compose up -d` | 低（本地） | 无需确认 |
| 2. Spring Boot 后端 | `mvn spring-boot:run -pl ai-agent-interfaces -Dspring-boot.run.profiles=local` | 低（本地） | 无需确认 |
| 3. Vite 前端 | `npm run dev` | 低（本地） | 无需确认 |

每一层必须在上一层健康检查通过后才能开始。

### 4. 执行

#### 4.1 启动 Docker 中间件

```bash
cd docker && docker compose up -d
```

**关键坑点**：
- **必须用 `docker compose`（v2 Go 插件），禁止使用 `docker-compose`（v1 Python）**。v1 与新版 Docker Engine 存在 `KeyError: 'ContainerConfig'` 兼容性问题。
- Docker Compose 文件位于仓库根目录的 `docker/docker-compose.yml`，非 `ai-agent-infrastructure/src/main/resources/docker/`。

等待所有容器进入 healthy 状态：

```bash
# 确认所有中间件健康
docker ps --format "table {{.Names}}\t{{.Status}}"
```

**Milvus 特殊注意事项**：16GB 以下内存的机器，Milvus 可能首次启动崩溃（exit code 134，SIGABRT）。如果出现此情况，等待其他容器 healthy 后单独重启：

```bash
docker start ai-agent-milvus
# 等待约 30-60s 直到状态变为 healthy
```

关键端口映射速查：

| 服务 | 宿主机端口 | 容器端口 |
|------|-----------|----------|
| MySQL | 13306 | 3306 |
| Redis | 6379 | 6379 |
| MinIO API | **9002** | 9000 |
| MinIO Console | 9001 | 9001 |
| Milvus gRPC | 19530 | 19530 |
| etcd | 2379 | 2379 |

#### 4.2 启动后端

```bash
mvn spring-boot:run -pl ai-agent-interfaces -Dspring-boot.run.profiles=local -DskipTests
```

等待健康检查返回 200：

```bash
curl -s http://localhost:8080/actuator/health | python3 -m json.tool
```

健康输出应包含 `"status": "UP"`，且 `db` 和 `redis` 组件均为 UP。

**关键坑点**：
- 如果 Milvus 未就绪便启动后端，会抛出 `DEADLINE_EXCEEDED: deadline exceeded` 错误并退出。
- 因为 Spring Boot 启动时会立刻初始化 `MilvusServiceClient` bean（受 `@ConditionalOnProperty(milvus.enabled=true)` 控制），可以通过设置 `MILVUS_ENABLED=false` 临时绕过。

#### 4.3 启动前端

```bash
cd ai-agent-foward && npm run dev
```

Vite 开发服务器默认监听 `http://localhost:5173`。

### 5. 验证

三层验证标准：

```text
直接验证:
- Docker: 所有 5 个容器 (mysql, redis, minio, etcd, milvus) status=healthy
- 后端: curl http://localhost:8080/actuator/health → {"status":"UP"}
- 前端: curl -s -o /dev/null -w "%{http_code}" http://localhost:5173 → 200

依赖链验证:
- 后端健康检查中 db + redis 均为 UP（确认 HikariCP 连接池和 Redisson 正常）
- MinIO Console 可登录: http://localhost:9001 (admin / admin123456)
- 后端日志无 Milvus 连接异常

端到端验证:
- 浏览器打开 http://localhost:5173 能正常加载页面
- 调用一个简单 API 确认数据通路（如 GET /api/agent）
- 检查浏览器 console 无网络错误
```

### 6. 回写

启动完成后：

- 在 `logs/YYYY-MM.md` 中记录本次启动的特殊情况（如 Milvus 崩溃重启、端口冲突处理）。
- 如果发现与已有 SOP 或 index 不一致的事实（如端口号、文件路径），更新对应文件。

## 安全边界

- capabilities: 只启动本地开发服务，不接触生产环境。
- safety: 所有数据在本地 Docker 卷中，容器删除不会丢失（除非手动 `docker compose down -v`）。
- confirmations: 启动流程无需确认。但如果需要 `docker compose down -v` 清数据，必须确认。
- forbidden actions:
  - 禁止 `docker-compose`（v1 Python），必须用 `docker compose`（v2 插件）。
  - 禁止在 Milvus 未 healthy 时强行启动后端（会导致 bean 初始化失败）。
  - 禁止直接编辑 `application-local.yml` 中的凭据（本地开发固定配置）。
  - 禁止使用 `kill -9` 清理后端/前端进程（用正常的 Ctrl+C 或进程管理）。
  - 禁止全局重启 Docker daemon。
- rollback / compensation: 停止所有服务 `docker compose down`（保留数据卷），停止后端/前端进程即可。

## 代码定位规则

用于排查启动失败的代码关键入口：

- Docker Compose: `docker/docker-compose.yml`
- 后端主类: `ai-agent-interfaces/.../AiAgentApplication.java`
- 本地配置: `ai-agent-interfaces/src/main/resources/application-local.yml`
- Milvus 配置: `ai-agent-infrastructure/.../memory/config/MilvusVectorStoreConfig.java`
- MinIO 配置: `ai-agent-infrastructure/.../config/MinIOConfig.java`
- 数据库初始化: `docker/init/mysql/01_init_schema.sql`
- 前端入口: `ai-agent-foward/src/index.tsx`

常用只读排查命令：

```bash
# 查看 Milvus 崩溃日志
docker logs ai-agent-milvus 2>&1 | tail -40

# 查看后端启动失败日志
tail -80 <background-task-output-file>

# 检查端口占用
ss -tlnp | grep <port>
```

## 输出形态

启动完成后输出状态汇总：

```text
| 服务 | 端口 | 状态 |
|------|------|------|
| Docker 中间件 | 13306/6379/9002/19530/2379 | 5/5 healthy |
| Spring Boot 后端 | 8080 | UP |
| Vite React 前端 | 5173 | 200 OK |

- 后端 API: http://localhost:8080
- 前端页面: http://localhost:5173
- MinIO 控制台: http://localhost:9001
```
