# AI Agent Platform 部署指南

本文档提供 AI Agent Platform 的完整部署指南，包括开发环境、测试环境和生产环境的部署步骤。

## 目录

- [环境要求](#环境要求)
- [开发环境部署](#开发环境部署)
- [测试环境部署](#测试环境部署)
- [生产环境部署](#生产环境部署)
- [配置说明](#配置说明)
- [常见问题](#常见问题)
- [运维脚本](#运维脚本)

---

## 环境要求

### 软件要求

| 软件 | 版本要求 | 说明 |
|------|---------|------|
| Java | 21+ | 后端运行环境 |
| Node.js | 18+ | 前端构建环境 |
| Maven | 3.8+ | 后端构建工具 |
| Docker | 20.10+ | 容器运行环境 |
| Docker Compose | 2.0+ | 容器编排工具 |

### 服务依赖

| 服务 | 版本 | 端口 | 说明 |
|------|------|------|------|
| MySQL | 8.0 | 13306 | 主数据库 |
| Redis | 7.0 | 6379 | 缓存和消息队列 |
| Milvus | 2.3 | 19530 | 向量数据库 |
| MinIO | latest | 9000/9001 | 对象存储 |
| etcd | 3.5 | 2379 | Milvus依赖 |

### 硬件要求

**开发环境**:
- CPU: 4核+
- 内存: 8GB+
- 磁盘: 20GB+

**生产环境**:
- CPU: 8核+
- 内存: 16GB+
- 磁盘: 100GB+
- 网络: 100Mbps+

---

## 开发环境部署

### 1. 克隆代码

```bash
git clone https://github.com/your-org/ai-agent.git
cd ai-agent
```

### 2. 启动依赖服务

```bash
cd ai-agent-infrastructure/src/main/resources/docker
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

### 3. 初始化数据库

```bash
# 连接MySQL
mysql -h 127.0.0.1 -P 13306 -u root -p

# 创建数据库
CREATE DATABASE IF NOT EXISTS ai_agent DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 导入完整初始化脚本（与 Docker 首次启动时保持一致）
USE ai_agent;
SOURCE docker/init/mysql/01_init_schema.sql;
```

说明：

- `ai-agent-infrastructure/src/main/resources/docker/init/mysql/01_init_schema.sql` 是当前部署唯一使用的 MySQL 初始化脚本，包含 swarm / writing / workflow / knowledge 等当前完整表结构。
- 仓库中的数据库 schema 真相源统一为 `ai-agent-infrastructure/src/main/resources/docker/init/mysql/01_init_schema.sql`。
- 如果用 Docker 首次初始化 MySQL，现在只会自动执行 `docker/init/mysql/01_init_schema.sql`。
- 项目当前不再维护单独的 migration SQL；需要重建环境时，直接清空 MySQL 数据卷并重新执行这份初始化脚本即可。

### 4. 配置后端

编辑 `ai-agent-interfaces/src/main/resources/application-local.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:13306/ai_agent
    username: root
    password: root123456
  data:
    redis:
      host: localhost
      port: 6379
      password: redis123456

jwt:
  secret: your-secret-key-change-in-production
  expiration: 7200000  # 2 hours
```

### 5. 启动后端服务

```bash
# 方式1: 使用Maven
mvn clean install -DskipTests
mvn spring-boot:run -pl ai-agent-interfaces -Dspring-boot.run.profiles=local

# 方式2: 使用IDE
# 在IDEA中运行 AiAgentApplication.java
```

### 6. 启动前端服务

```bash
cd ai-agent-foward

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 访问 http://localhost:5173
```

### 7. 验证部署

```bash
# 检查后端健康状态
curl http://localhost:8080/actuator/health

# 检查前端
curl http://localhost:5173
```

---

## 测试环境部署

### 1. 服务器准备

```bash
# 更新系统
sudo apt update && sudo apt upgrade -y

# 安装Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER

# 安装Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

### 2. 部署依赖服务

```bash
# 创建部署目录
mkdir -p /opt/ai-agent
cd /opt/ai-agent

# 复制docker-compose.yml
cp ai-agent-infrastructure/src/main/resources/docker/docker-compose.yml .

# 启动服务
docker-compose up -d
```

### 3. 部署后端

```bash
# 构建JAR包
mvn clean package -DskipTests

# 复制到服务器
scp ai-agent-interfaces/target/ai-agent-interfaces-1.0.0-SNAPSHOT.jar user@server:/opt/ai-agent/

# 创建systemd服务
sudo tee /etc/systemd/system/ai-agent.service > /dev/null <<EOF
[Unit]
Description=AI Agent Platform
After=network.target

[Service]
Type=simple
User=aiagent
WorkingDirectory=/opt/ai-agent
ExecStart=/usr/bin/java -jar -Dspring.profiles.active=test /opt/ai-agent/ai-agent-interfaces-1.0.0-SNAPSHOT.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# 启动服务
sudo systemctl daemon-reload
sudo systemctl enable ai-agent
sudo systemctl start ai-agent

# 查看状态
sudo systemctl status ai-agent
```

### 4. 部署前端

```bash
# 构建前端
cd ai-agent-foward
npm run build

# 复制到服务器
scp -r dist/* user@server:/var/www/ai-agent/

# 配置Nginx
sudo tee /etc/nginx/sites-available/ai-agent > /dev/null <<EOF
server {
    listen 80;
    server_name test.ai-agent.com;
    root /var/www/ai-agent;
    index index.html;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
    }
}
EOF

# 启用站点
sudo ln -s /etc/nginx/sites-available/ai-agent /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

---

## 生产环境部署

### 1. 架构设计

```
                    ┌─────────────┐
                    │   Nginx     │
                    │  (SSL/LB)   │
                    └──────┬──────┘
                           │
              ┌────────────┴────────────┐
              │                         │
         ┌────▼────┐              ┌────▼────┐
         │ Backend │              │ Backend │
         │ Node 1  │              │ Node 2  │
         └────┬────┘              └────┬────┘
              │                         │
              └────────────┬────────────┘
                           │
         ┌─────────────────┴─────────────────┐
         │                                   │
    ┌────▼────┐  ┌────────┐  ┌────────┐  ┌──▼───┐
    │  MySQL  │  │ Redis  │  │ Milvus │  │MinIO │
    │ Master  │  │Cluster │  │Cluster │  │      │
    └────┬────┘  └────────┘  └────────┘  └──────┘
         │
    ┌────▼────┐
    │  MySQL  │
    │  Slave  │
    └─────────┘
```

### 2. SSL证书配置

```bash
# 安装certbot
sudo apt install certbot python3-certbot-nginx

# 获取证书
sudo certbot --nginx -d ai-agent.com -d www.ai-agent.com

# 自动续期
sudo certbot renew --dry-run
```

### 3. 生产环境配置

使用 `docs/deployment/docker-compose-prod.yml` 部署依赖服务。

创建 `application-prod.yml`:

```yaml
server:
  port: 8080
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12
  servlet:
    session:
      cookie:
        secure: true
        http-only: true
        same-site: strict

spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:3306/ai_agent?useSSL=true
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
  data:
    redis:
      host: ${REDIS_HOST}
      port: 6379
      password: ${REDIS_PASSWORD}
      lettuce:
        pool:
          max-active: 20
          max-idle: 10

jwt:
  secret: ${JWT_SECRET}
  access-token:
    expiration: 7200000  # 2 hours
  refresh-token:
    expiration: 604800000  # 7 days

logging:
  level:
    root: INFO
    com.zj.aiagent: INFO
  file:
    name: /var/log/ai-agent/application.log
```

### 4. 环境变量配置

创建 `/opt/ai-agent/.env`:

```bash
# 数据库配置
DB_HOST=mysql-master.internal
DB_USERNAME=aiagent
DB_PASSWORD=<strong-password>

# Redis配置
REDIS_HOST=redis-cluster.internal
REDIS_PASSWORD=<strong-password>

# JWT配置
JWT_SECRET=<random-256-bit-key>

# SSL配置
SSL_KEYSTORE_PASSWORD=<keystore-password>

# 邮件配置
MAIL_HOST=smtp.example.com
MAIL_USERNAME=noreply@ai-agent.com
MAIL_PASSWORD=<mail-password>
```

### 5. 负载均衡配置

Nginx配置 (`/etc/nginx/sites-available/ai-agent-prod`):

```nginx
upstream backend {
    least_conn;
    server backend1.internal:8080 max_fails=3 fail_timeout=30s;
    server backend2.internal:8080 max_fails=3 fail_timeout=30s;
}

server {
    listen 443 ssl http2;
    server_name ai-agent.com www.ai-agent.com;

    ssl_certificate /etc/letsencrypt/live/ai-agent.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/ai-agent.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # HSTS
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # 前端静态文件
    root /var/www/ai-agent;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # 后端API
    location /api {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # SSE支持
    location /api/workflow/execution/stream {
        proxy_pass http://backend;
        proxy_http_version 1.1;
        proxy_set_header Connection '';
        proxy_buffering off;
        proxy_cache off;
        chunked_transfer_encoding off;
    }
}

# HTTP重定向到HTTPS
server {
    listen 80;
    server_name ai-agent.com www.ai-agent.com;
    return 301 https://$server_name$request_uri;
}
```

### 6. 监控配置

使用Prometheus + Grafana监控:

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'ai-agent'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend1:8080', 'backend2:8080']
```

---

## 配置说明

### 数据库配置

**连接池配置**:
- 开发环境: 10个连接
- 测试环境: 20个连接
- 生产环境: 50个连接

**索引优化**:
确保以下索引存在:
- `agent_info`: idx_user_id, idx_status
- `conversation`: idx_user_id, idx_updated_at
- `workflow_execution`: idx_user_id, idx_status

### Redis配置

**键命名规范**:
- 用户Token: `user:token:{userId}`
- Refresh Token: `user:refresh_token:{userId}:{deviceId}`
- 工作流状态: `workflow:execution:{executionId}`
- 对话历史: `conversation:messages:{conversationId}`

**过期时间**:
- Access Token: 2小时
- Refresh Token: 7天
- 工作流状态: 24小时
- 对话历史: 30天

### Milvus配置

**Collection配置**:
- 向量维度: 1536 (OpenAI embedding)
- 索引类型: IVF_FLAT
- 相似度度量: COSINE

---

## 常见问题

### 1. 后端服务启动失败

**问题**: 端口被占用
```bash
# 检查端口占用
lsof -i :8080

# 杀死进程
kill -9 <PID>
```

**问题**: 数据库连接失败
```bash
# 检查MySQL状态
docker-compose ps mysql

# 查看MySQL日志
docker-compose logs mysql

# 测试连接
mysql -h 127.0.0.1 -P 13306 -u root -p
```

### 2. 前端无法访问后端

**问题**: CORS错误
- 检查 `application.yml` 中的CORS配置
- 确保前端请求URL正确

**问题**: 代理配置错误
- 检查 `vite.config.ts` 中的proxy配置
- 确保后端服务正在运行

### 3. SSE连接失败

**问题**: Nginx缓冲导致
- 确保Nginx配置了 `proxy_buffering off`
- 确保使用HTTP/1.1

### 4. Milvus连接失败

**问题**: etcd未启动
```bash
# 检查etcd状态
docker-compose ps etcd

# 重启Milvus
docker-compose restart milvus-standalone
```

### 5. MinIO访问失败

**问题**: 访问密钥错误
- 检查 `application.yml` 中的MinIO配置
- 确保accessKey和secretKey正确

---

## 运维脚本

### 部署脚本

使用 `scripts/deploy.sh` 进行一键部署。

### 健康检查脚本

使用 `scripts/health-check.sh` 检查所有服务状态。

### 备份脚本

```bash
#!/bin/bash
# backup.sh

BACKUP_DIR="/backup/ai-agent/$(date +%Y%m%d)"
mkdir -p $BACKUP_DIR

# 备份MySQL
docker exec mysql mysqldump -u root -proot123456 ai_agent > $BACKUP_DIR/mysql.sql

# 备份Redis
docker exec redis redis-cli --rdb $BACKUP_DIR/redis.rdb

# 备份MinIO
mc mirror minio/ai-agent $BACKUP_DIR/minio/

echo "Backup completed: $BACKUP_DIR"
```

### 日志清理脚本

```bash
#!/bin/bash
# cleanup-logs.sh

# 清理30天前的日志
find /var/log/ai-agent -name "*.log" -mtime +30 -delete

# 清理Docker日志
docker system prune -af --filter "until=720h"

echo "Log cleanup completed"
```

---

## 安全建议

1. **定期更新依赖**: 使用 `mvn versions:display-dependency-updates` 检查更新
2. **定期备份**: 每天备份数据库和重要文件
3. **监控告警**: 配置Prometheus告警规则
4. **日志审计**: 定期审查访问日志和错误日志
5. **密钥轮换**: 定期更换JWT密钥和数据库密码

---

## 性能优化

1. **数据库优化**:
   - 使用连接池
   - 添加适当索引
   - 定期分析慢查询

2. **缓存优化**:
   - 使用Redis缓存热点数据
   - 设置合理的过期时间
   - 使用缓存预热

3. **应用优化**:
   - 启用JVM参数优化
   - 使用异步处理
   - 限流和熔断

---

## 联系支持

如有问题，请联系:
- 技术支持: support@ai-agent.com
- 文档: https://docs.ai-agent.com
- GitHub: https://github.com/your-org/ai-agent
