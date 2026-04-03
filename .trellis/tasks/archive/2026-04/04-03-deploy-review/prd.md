# 部署流程 Review 与修复

## Goal

Review 整个 Docker 部署流程，识别并修复所有阻碍「推送镜像到 ghcr.io → 服务器 `docker compose pull` 启动」这一核心流程的问题。

## 问题清单（已识别）

### 🔴 Critical — 阻塞部署流程

#### 1. `docker-compose.prod.yml` 使用 `build:` 而非 `image:`

**现象**：`docker-compose.prod.yml` 中 `backend` 和 `frontend` 服务使用的是 `build:` 配置，
而文档声称「方式一：从 ghcr.io 拉取部署」，使用 `docker compose pull`。

**后果**：
- `docker compose pull` 对 `build:` 无效，不会从 ghcr.io 拉取镜像
- 服务器上执行 `pull` 静默失败（或报错）
- 即使执行 `up`，也是从源码重新构建，而不是用预构建镜像

**正确做法**：改为 `image: ghcr.io/zj669/ai-agent/backend:${IMAGE_TAG:-latest}`，
由 GitHub Actions 在 CI 中构建并推送镜像。

#### 2. 缺少 GitHub Actions 镜像发布流程

**现象**：`.github/workflows/` 只有 `ci-test.yml`（跑测试），没有发布 workflow。
`DEPLOY_SERVER.md` 描述的是「手动本地构建推送」，整个流程依赖人工操作。

**后果**：
- 没有版本化标签（`latest` / `v1.x.y`）的自动管理
- 每次发版必须手动在本地执行 `docker build && docker push`
- 无法在服务器上通过 `git pull` + `docker compose pull` 实现自动化更新

**正确做法**：添加 `.github/workflows/deploy.yml`，在 push 到 `main` 时自动构建并推送镜像。

#### 3. Backend healthcheck 依赖不存在的 `curl`

**现象**：`docker-compose.prod.yml` 的 backend healthcheck：
```yaml
test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
```
但 `eclipse-temurin:21-jre-alpine` 基础镜像默认不包含 `curl`。

**后果**：
- 容器启动后 healthcheck 立即失败
- `docker compose up` 永远无法通过健康检查
- 服务被标记为 `unhealthy`，重启循环

**修复方案**：
- 方案A（推荐）：在 Dockerfile runtime 阶段安装 `curl`：`RUN apk add --no-cache curl`
- 方案B：改用 `wget`（Alpine 默认有）：`wget -qO- http://localhost:8080/actuator/health`
- 方案C：改用 Spring Boot Actuator 的 TCP socket 检查

### 🟡 Medium — 流程完整性缺陷

#### 4. MySQL init script 路径在 prod compose 中可能失效

**现象**：
- dev `docker-compose.yml`（在 `docker/` 内）：`./init/mysql` ✅
- prod `docker-compose.prod.yml`（也在 `docker/` 内）：`../init/mysql`

假设服务从 `docker/` 目录执行：
```
cd /opt/ai-agent/docker
docker compose -f docker-compose.prod.yml up
```
- `./init/mysql` → `/opt/ai-agent/docker/init/mysql` ✅
- `../init/mysql` → `/opt/ai-agent/init/mysql` ❌（不存在）

**后果**：生产环境 MySQL 初始化脚本不会被执行，数据库 schema 和种子数据丢失。

**修复方案**：prod compose 中的 MySQL volume path 应改为 `./init/mysql`（与 dev 一致），
前提是部署时确保 `init/mysql/` 在 `docker/` 目录内（文档已说明上传 docker 目录）。

#### 5. Backend jar 路径可能不存在（单 module 构建产物）

**现象**：Dockerfile 中：
```dockerfile
COPY --from=builder /app/ai-agent-interfaces/target/*.jar app.jar
```
但构建命令 `-pl ai-agent-interfaces -am` 只打包了 interfaces 模块。
需确认 fat-jar 确实生成在 `ai-agent-interfaces/target/` 而非根 `target/`。

**验证**：本地执行 `mvn package -DskipTests -pl ai-agent-interfaces -am` 后检查产物路径。

#### 6. 没有版本化镜像标签管理

**现象**：手动 push 命令同时打 `latest` 和 `v1.0.0` 两个标签，
但没有语义化版本（SemVer）规则或 CHANGELOG 追踪。

**影响**：服务器回滚时无法精准指定版本。

### 🟢 Low — 一致性和最佳实践

#### 7. MinIO 镜像版本不一致

- dev: `minio/minio:RELEASE.2023-03-20T20-16-18Z`（固定版本）
- prod: `minio/minio:latest`

#### 8. `SPRING_PROFILES_ACTIVE: prod` 但 `CORS_ALLOWED_ORIGINS` 默认值问题

`application-prod.yml` 中 `cors.allowed-origins` 默认值是空字符串，
但 `docker-compose.prod.yml` 中设置了 `${CORS_ALLOWED_ORIGINS:-http://localhost}`，
生产环境如不配置会退化为 `localhost`。

#### 9. Backend logs volume 权限

`docker-compose.prod.yml` 挂载 `backend_logs:/app/logs`，
但 Dockerfile 中 runtime 阶段创建了 `/app/logs` 并 chown 给 `appuser`。
named volume 会被 Docker 自动初始化，可能覆盖 chown 效果（取决于 Docker 版本）。

---

## Acceptance Criteria

- [ ] `docker-compose.prod.yml` 使用 `image: ghcr.io/...` 替代 `build:`，服务器可执行 `docker compose pull` 拉取预构建镜像
- [ ] 添加 `.github/workflows/deploy.yml`，push main 时自动构建并推送 backend/frontend 镜像到 ghcr.io
- [ ] Backend healthcheck 在 Alpine 镜像中可正常工作
- [ ] MySQL init volume path 在 prod compose 中使用 `./init/mysql`
- [ ] 验证 backend fat-jar 路径正确
- [ ] DEPLOY_SERVER.md 文档更新，反映正确的使用流程
- [ ] 所有配置通过 `docker/.env.prod` 环境变量注入，无硬编码密码
