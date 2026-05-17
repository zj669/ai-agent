# AI Agent Platform - 服务器 Docker 部署指南

> 适用于在远程服务器上通过 Docker Compose 部署。
>
> **镜像发布**: 需要开发者在本地构建并手动推送镜像至 ghcr.io，或在服务器上直接构建。
> GitHub Actions workflow 当前仅用于 CI 测试，不负责镜像构建。

---

## 目录

1. [环境准备](#1-环境准备)
2. [方式一：Docker Compose 拉取部署（推荐）](#2-方式一docker-compose-拉取部署推荐)
3. [方式二：手动构建部署](#3-方式二手动构建部署)
4. [配置说明](#4-配置说明)
5. [健康检查与日志](#5-健康检查与日志)
6. [数据持久化](#6-数据持久化)
7. [更新与回滚](#7-更新与回滚)
8. [HTTPS 配置](#8-https-配置)
9. [常见问题](#9-常见问题)

---

## 1. 环境准备

### 服务器要求

| 项目 | 最低要求 | 推荐配置 |
|------|---------|---------|
| CPU | 2 核 | 4+ 核 |
| 内存 | 4 GB | 8+ GB |
| 磁盘 | 40 GB | 100 GB SSD |
| 系统 | Ubuntu 20.04+ / CentOS 8+ | Ubuntu 22.04 LTS |
| Docker | 20.10+ | 24.0+ |
| Docker Compose | v2.0+ | v2.20+ |

### 安装 Docker & Docker Compose

```bash
# Ubuntu / Debian
sudo apt update && sudo apt install -y docker.io docker-compose-v2
sudo systemctl enable docker && sudo systemctl start docker

# 验证安装
docker --version
docker compose version
```

### 登录 GitHub Container Registry

推送镜像后，在服务器上登录 ghcr.io（需 GitHub Personal Access Token）：

```bash
echo $GITHUB_TOKEN | docker login ghcr.io -u USERNAME --password-stdin
```

> Token 需要 `packages:read` 和 `packages:write` 权限。
> 在 GitHub → Settings → Developer settings → Personal access tokens 生成。

---

## 2. 方式一：本地构建推送 → 服务器拉取（推荐）

> 在本地构建镜像并推送至 ghcr.io，然后在服务器拉取启动。
> 需要 GitHub Classic Personal Access Token（拥有 `packages:write` 权限）。

### 2.1 准备 GitHub Personal Access Token

1. 访问 https://github.com/settings/tokens
2. Generate new token (Classic)，勾选 `write:packages`
3. 妥善保存 token（后续两处都需要）

### 2.2 本地登录 ghcr.io

```bash
# 用刚才生成的 token 替换 YOUR_TOKEN
echo "YOUR_TOKEN" | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin
```

### 2.3 构建并推送镜像

```bash
cd /path/to/ai-agent

# 构建并推送后端（需要 Java 21 + Maven，或使用 Docker 纯容器方式）
docker build \
  -f ai-agent-interfaces/Dockerfile \
  -t ghcr.io/zj669/ai-agent/ai-agent-backend:latest \
  .

docker push ghcr.io/zj669/ai-agent/ai-agent-backend:latest

# 构建并推送前端
docker build \
  -f ai-agent-foward/Dockerfile.prod \
  -t ghcr.io/zj669/ai-agent/ai-agent-frontend:latest \
  ./ai-agent-foward

docker push ghcr.io/zj669/ai-agent/ai-agent-frontend:latest
```

> **纯 Docker 方式构建后端**（无需本地 Java）：在构建后端时可以用 `--build-arg MAVEN_OPTS=-Xmx2048m`，Dockerfile 内置 Maven 构建。

### 2.4 服务器拉取部署

```bash
sudo mkdir -p /opt/ai-agent/docker
cd /opt/ai-agent

# 上传 docker/ 目录（包含 docker-compose.prod.yml + .env.prod）
scp -r user@your-server:/path/to/docker /opt/ai-agent/

# 或直接克隆仓库并只使用 docker/ 子目录
git clone https://github.com/zj669/ai-agent.git .
# 仅保留 docker 目录，其余可删除（节省空间）
```

### 2.3 创建环境变量文件

```bash
cd /opt/ai-agent/docker
sudo cp .env.prod.example .env.prod
sudo nano .env.prod   # 填写真实密码和配置
sudo chmod 600 .env.prod
```

**必须修改的配置项：**

```bash
# MySQL 密码（必填）
PRIMARY_DB_PASSWORD=your-strong-mysql-password

# Redis 密码（必填）
REDIS_PASSWORD=your-strong-redis-password

# MinIO 密码
MINIO_ROOT_PASSWORD=your-minio-password

# JWT 密钥（必填，建议 64+ 字符随机字符串）
JWT_SECRET=$(openssl rand -base64 64)

# 邮件 SMTP
MAIL_HOST=smtp.qq.com
MAIL_USERNAME=your-email@qq.com
MAIL_PASSWORD=your-email-auth-code

# 允许的域名
CORS_ALLOWED_ORIGINS=https://your-domain.com
```

### 2.4 拉取最新镜像

```bash
# 登录 GHCR（如尚未登录）
echo $GITHUB_TOKEN | docker login ghcr.io -u USERNAME --password-stdin

# Docker Compose V2（配置文件在 docker/ 目录，需在目录下执行）
docker compose --env-file .env.prod -f docker-compose.prod.yml pull

# Docker Compose V1
docker-compose --env-file .env.prod -f docker-compose.prod.yml pull
```

### 2.5 启动服务

```bash
# 前台启动（首次验证）
docker compose --env-file .env.prod -f docker-compose.prod.yml up

# 后台运行（确认无误后）
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d

# 查看状态
docker compose --env-file .env.prod -f docker-compose.prod.yml ps
```

### 2.6 验证服务

```bash
# 健康检查
curl http://localhost:8090/actuator/health
curl http://localhost/  # 前端

# 查看日志
docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f backend
docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f frontend
```

---

## 3. 方式二：在服务器上从源码构建（GitHub Actions 不可用时的备选方案）

适用于 ghcr.io 无法访问，或需要自定义构建参数的场景。

### 3.1 在服务器上构建

```bash
cd /opt/ai-agent

# 克隆仓库（或通过 scp 上传）
git clone https://github.com/zj669/ai-agent.git .

# 构建后端镜像（镜像名需与 docker-compose.prod.yml 中一致）
docker build \
  -f ai-agent-interfaces/Dockerfile \
  -t ghcr.io/zj669/ai-agent/ai-agent-backend:latest \
  .

# 构建前端镜像
docker build \
  -f ai-agent-foward/Dockerfile.prod \
  -t ghcr.io/zj669/ai-agent/ai-agent-frontend:latest \
  ./ai-agent-foward

# 启动（在 docker/ 目录下执行）
cd docker
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d
```

---

## 4. 配置说明

### 4.1 端口映射

| 服务 | 容器端口 | 默认主机端口 | 说明 |
|------|---------|------------|------|
| Frontend | 80 | `FRONTEND_PORT` (默认 80) | 主访问入口 |
| Backend | 8090 | 127.0.0.1:8090 | 由 nginx 代理 |
| MySQL | 3306 | 不暴露 | 仅容器内访问 |
| Redis | 6379 | 不暴露 | 仅容器内访问 |
| MinIO API | 9000 | `MINIO_API_PORT` (默认 9000) | S3 兼容 API |
| MinIO Console | 9001 | `MINIO_CONSOLE_PORT` (默认 9001) | Web 管理界面 |
| Milvus gRPC | 19530 | `MILVUS_GRPC_PORT` (默认 19530) | Java SDK 连接 |
| Milvus Metrics | 9091 | `MILVUS_METRICS_PORT` (默认 9091) | Prometheus 指标 |

如需修改默认端口，在 `.env.prod` 中覆盖：

```bash
FRONTEND_PORT=8080
MILVUS_GRPC_PORT=19531
```

### 4.2 关键环境变量

| 变量名 | 必须 | 默认值 | 说明 |
|--------|------|--------|------|
| `PRIMARY_DB_PASSWORD` | ✅ | - | MySQL root 密码 |
| `REDIS_PASSWORD` | ✅ | - | Redis 访问密码 |
| `JWT_SECRET` | ✅ | - | JWT 签名密钥（64+字符） |
| `MAIL_HOST` | ✅ | smtp.example.com | SMTP 服务器 |
| `MAIL_USERNAME` | ✅ | - | 发送邮件账号 |
| `MAIL_PASSWORD` | ✅ | - | 邮件密码/授权码 |
| `CORS_ALLOWED_ORIGINS` | ✅ | - | 前端域名，逗号分隔 |
| `CSDN_COOKIE` | | - | CSDN 文章发布 MCP 使用的浏览器 Cookie，启用该 MCP 时填写 |
| `MINIO_ROOT_USER` | | admin | MinIO 访问密钥 |
| `MINIO_ROOT_PASSWORD` | | admin123456 | MinIO 密文密钥 |
| `MILVUS_ENABLED` | | true | 是否启用 Milvus |

### 4.3 配置 CSDN 文章发布 MCP

生产环境的 stdio MCP 子进程在 `backend` 容器内运行。后端镜像已内置 Node.js 和项目内的 `mcp-servers/` 目录，CSDN 文章发布 MCP 的容器内路径为：

```text
/app/mcp-servers/csdn-article-publisher/server.js
```

1. 在 `docker/.env.prod` 填入 CSDN 登录态 Cookie：

```bash
CSDN_COOKIE='UserName=...; UserToken=...; SESSION=...'
```

2. 重建并启动后端镜像，使 `backend` 容器拿到新的环境变量：

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build backend
```

如果使用 GHCR 拉取镜像部署，需要先重新构建并推送后端镜像，然后在服务器执行：

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml pull backend
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d backend
```

3. 用户注册或登录后，查询目标用户 ID：

```bash
docker exec -it ai-agent-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" ai_agent -e "SELECT id, username, email FROM user_info ORDER BY id DESC;"'
```

4. 为该用户注册 MCP server 配置：

```bash
./scripts/register-csdn-mcp.sh <user_id>
```

脚本会在 `mcp_server_config` 中创建或更新如下 stdio 配置：

```json
{
  "type": "stdio",
  "command": "node",
  "args": ["/app/mcp-servers/csdn-article-publisher/server.js"],
  "env": {}
}
```

Cookie 不写入数据库；MCP 子进程从 `backend` 容器环境继承 `CSDN_COOKIE`。注册完成后，在前端「MCP 管理」页面点击连接，工具列表中应出现 `send_article`。

---

## 5. 健康检查与日志

### 5.1 健康检查

```bash
# 全部服务
docker compose --env-file .env.prod -f docker-compose.prod.yml ps

# 后端健康状态
curl -s http://localhost:8090/actuator/health | jq .

# Prometheus 指标
curl http://localhost:8090/actuator/prometheus | head -20
```

### 5.2 日志查看

```bash
# 实时查看所有日志
docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f

# 实时查看后端日志
docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f backend

# 查看最近 100 行后端日志
docker compose --env-file .env.prod -f docker-compose.prod.yml logs --tail=100 backend

# 查找错误
docker compose --env-file .env.prod -f docker-compose.prod.yml logs backend | grep -i error
```

---

## 6. 数据持久化

所有数据通过 Docker named volumes 持久化，**`docker compose down` 不会丢失数据**。

```bash
# 查看 volumes
docker volume ls | grep ai-agent

# 备份 MySQL 数据
docker run --rm \
  -v ai-agent_mysql_data:/data \
  -v $(pwd)/backup:/backup \
  alpine tar czf /backup/mysql_backup_$(date +%Y%m%d).tar.gz -C /data .

# 备份 MinIO 数据
docker run --rm \
  -v ai-agent_minio_data:/data \
  -v $(pwd)/backup:/backup \
  alpine tar czf /backup/minio_backup_$(date +%Y%m%d).tar.gz -C /data .

# 恢复 MySQL（停止服务后操作）
docker compose --env-file .env.prod -f docker-compose.prod.yml down mysql
docker run --rm \
  -v ai-agent_mysql_data:/data \
  -v $(pwd)/backup:/backup \
  alpine tar xzf /backup/mysql_backup_20260403.tar.gz -C /data
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d
```

> ⚠️ `docker compose down -v` 会**删除所有数据卷**，务必谨慎使用！

---

## 7. 更新与回滚

### 7.1 更新到最新版本

```bash
cd /opt/ai-agent

# 拉取最新镜像
docker compose --env-file .env.prod -f docker-compose.prod.yml pull

# 滚动更新（自动重启服务）
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d

# 如需重新构建（代码变更后）
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

### 7.2 回滚到指定版本

```bash
# 指定版本拉取（需要该 tag 已在本地或远程存在）
docker compose --env-file .env.prod -f docker-compose.prod.yml pull

# 指定 IMAGE_TAG 启动
IMAGE_TAG=v1.0.0 docker compose --env-file .env.prod -f docker-compose.prod.yml up -d
```

---

## 8. HTTPS 配置

生产环境强烈建议通过 **Nginx/Traefik 反向代理**处理 HTTPS，当前 Docker Compose 后端保持 8090 明文。

### 8.1 Nginx 反向代理方案

在服务器上部署 Nginx（独立于 Docker）：

```nginx
# /etc/nginx/sites-available/ai-agent
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;

    # 前端静态资源
    location / {
        proxy_pass http://127.0.0.1:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # 后端 API
    location /api/ {
        proxy_pass http://127.0.0.1:8090/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket (SSE)
    location /ws/ {
        proxy_pass http://127.0.0.1:8090/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_read_timeout 3600s;
    }
}
```

### 8.2 使用 Let's Encrypt 免费证书

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com
sudo systemctl enable certbot.timer  # 自动续期
```

### 8.3 CORS 配置

在 `.env.prod` 中配置允许的域名：

```bash
CORS_ALLOWED_ORIGINS=https://your-domain.com
```

---

## 9. 常见问题

### Q1: 启动后端报 `Communications link failure`

**原因**：MySQL 容器尚未就绪，后端就启动了。

**解决**：后端有 `depends_on` + `healthcheck`，等待约 30 秒即可。如持续出现：

```bash
# 检查 MySQL 是否健康
docker compose --env-file .env.prod -f docker-compose.prod.yml ps mysql
docker compose --env-file .env.prod -f docker-compose.prod.yml logs mysql | tail -20

# 手动测试连接
docker exec -it ai-agent-mysql mysql -uroot -p -h mysql
```

### Q2: Milvus 连接失败

**原因**：etcd 或 MinIO 未就绪，或版本不兼容。

**解决**：
```bash
# Milvus 有 90s start_period，首次启动耐心等待
docker compose --env-file .env.prod -f docker-compose.prod.yml logs milvus | tail -30
```

### Q3: 前端 502 Bad Gateway

**原因**：后端未启动或 `backend` 服务名在 nginx 中不解析。

**解决**：
```bash
# 确认 backend 健康
curl http://backend:8090/actuator/health

# 在 frontend 容器内测试连通性
docker exec ai-agent-frontend wget -qO- http://backend:8090/actuator/health
```

### Q4: 镜像拉取失败 (manifest unknown)

**原因**：GitHub Token 权限不足或未登录。

**解决**：
```bash
docker logout ghcr.io
echo $GITHUB_TOKEN | docker login ghcr.io -u USERNAME --password-stdin
```

### Q5: 如何查看各服务版本？

```bash
# Backend
curl http://localhost:8090/actuator/info | jq .app.version

# MySQL
docker exec ai-agent-mysql mysql -V

# Redis
docker exec ai-agent-redis redis-server --version

# Milvus
docker exec ai-agent-milvus milvus --version
```
