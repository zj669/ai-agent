# 生产部署指南

## 环境变量

### 必需配置

```bash
# 数据库
DB_HOST=your-mysql-host
DB_PORT=3306
DB_NAME=ai_agent
DB_USERNAME=your-db-user
DB_PASSWORD=your-db-password

# Redis
REDIS_HOST=your-redis-host
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password

# JWT (至少 32 字符)
JWT_SECRET=your-secure-jwt-secret-at-least-32-chars

# SSL 证书
SSL_KEYSTORE_PASSWORD=your-keystore-password

# CORS (多个域名用逗号分隔)
CORS_ALLOWED_ORIGINS=https://your-domain.com,https://www.your-domain.com
```

### 可选配置

```bash
# Milvus 向量数据库
MILVUS_HOST=localhost
MILVUS_PORT=19530

# MinIO 对象存储
MINIO_ENDPOINT=https://your-minio-endpoint
MINIO_ACCESS_KEY=your-access-key
MINIO_SECRET_KEY=your-secret-key
MINIO_BUCKET=ai-agent

# 邮件服务
MAIL_HOST=smtp.your-provider.com
MAIL_PORT=587
MAIL_USERNAME=your-email
MAIL_PASSWORD=your-email-password
```

## 启动命令

### Docker 基础服务

```bash
cd ai-agent-infrastructure/src/main/resources/docker
docker-compose up -d
docker-compose ps  # 验证服务状态
```

### 后端服务

```bash
# 构建
mvn clean package -DskipTests

# 启动 (生产环境)
java -jar ai-agent-interfaces/target/ai-agent-interfaces.jar \
  --spring.profiles.active=prod
```

### 前端服务

```bash
cd ai-agent-foward

# 构建
npm install
npm run build

# 部署 dist/ 目录到 Nginx 或 CDN
```

## 健康检查

| 端点 | 用途 |
|------|------|
| `GET /actuator/health` | 应用健康状态 |
| `GET /actuator/info` | 应用信息 |
| `GET /actuator/prometheus` | Prometheus 指标 |

```bash
# 检查应用状态
curl -k https://localhost:8443/actuator/health
```

## 故障排查

| 问题 | 检查项 |
|------|--------|
| 启动失败 | 检查环境变量是否完整，查看日志 `/var/log/ai-agent/application.log` |
| 数据库连接失败 | 验证 DB_HOST/PORT/PASSWORD，检查网络连通性 |
| Redis 连接失败 | 验证 REDIS_HOST/PORT/PASSWORD |
| CORS 错误 | 检查 CORS_ALLOWED_ORIGINS 是否包含前端域名 |
| SSL 证书错误 | 确认 `keystore/server.p12` 存在且密码正确 |
| 401 未授权 | 检查 JWT_SECRET 配置，确认 Token 未过期 |

## 端口说明

| 服务 | 端口 |
|------|------|
| 后端 HTTPS | 8443 |
| 后端 HTTP (重定向) | 8080 |
| MySQL | 3306 |
| Redis | 6379 |
| Milvus | 19530 |
| MinIO API | 9000 |
