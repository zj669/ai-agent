# AI Agent 服务器部署指南

## 📋 部署前准备

### 环境要求

- **服务器**: Linux (推荐 Ubuntu 20.04+)
- **Docker**: 20.10+
- **Docker Compose**: 1.29+ (可选)
- **端口**: 8080 (后端), 80 (前端)

### 网络要求

确保服务器可以访问以下外部服务:
- 数据库服务器: `117.72.152.117:13306` (MySQL)
- 向量数据库: `117.72.152.117:5432` (PostgreSQL)
- Redis: `117.72.152.117:16379`
- OpenAI API: `https://globalai.vip`

---

## 🚀 快速部署

### 方式一: 使用部署脚本(推荐)

```bash
# 1. 上传项目到服务器
scp -r d:\java\ai-agent user@your-server:/opt/ai-agent

# 2. SSH 登录服务器
ssh user@your-server

# 3. 进入项目目录
cd /opt/ai-agent

# 4. 赋予脚本执行权限
chmod +x deploy.sh

# 5. 执行部署
./deploy.sh
```

### 方式二: 手动部署

#### 后端部署

```bash
# 1. 构建后端镜像
cd /opt/ai-agent
docker build -t ai-agent-backend:latest .

# 2. 停止旧容器(如果存在)
docker stop ai-agent-backend 2>/dev/null || true
docker rm ai-agent-backend 2>/dev/null || true

# 3. 启动新容器
docker run -d \
  --name ai-agent-backend \
  -p 8080:8080 \
  --restart unless-stopped \
  -v /opt/ai-agent/logs:/app/data/log \
  ai-agent-backend:latest

# 4. 查看日志
docker logs -f ai-agent-backend
```

#### 前端部署

```bash
# 1. 构建前端镜像
cd /opt/ai-agent/app
docker build -t ai-agent-frontend:latest .

# 2. 停止旧容器
docker stop ai-agent-frontend 2>/dev/null || true
docker rm ai-agent-frontend 2>/dev/null || true

# 3. 启动新容器
docker run -d \
  --name ai-agent-frontend \
  -p 80:80 \
  --restart unless-stopped \
  ai-agent-frontend:latest

# 4. 查看日志
docker logs -f ai-agent-frontend
```

---

## ✅ 验证部署

### 健康检查

```bash
# 检查后端健康状态
curl http://localhost:8080/actuator/health

# 预期输出:
# {"status":"UP"}

# 检查前端
curl -I http://localhost:80

# 预期输出:
# HTTP/1.1 200 OK
```

### 查看容器状态

```bash
# 查看所有容器
docker ps

# 查看后端日志
docker logs -f ai-agent-backend --tail 100

# 查看前端日志
docker logs -f ai-agent-frontend --tail 100
```

### 访问应用

- **前端**: `http://your-server-ip`
- **后端 API**: `http://your-server-ip:8080`
- **健康检查**: `http://your-server-ip:8080/actuator/health`
- **Prometheus 指标**: `http://your-server-ip:8080/actuator/prometheus`

---

## 🔧 常见问题排查

### 1. 容器启动失败

```bash
# 查看详细日志
docker logs ai-agent-backend

# 常见原因:
# - 端口被占用: 修改 -p 参数
# - 内存不足: 调整 JAVA_OPTS
# - 配置文件错误: 检查 application-dev.yml
```

### 2. 数据库连接失败

```bash
# 检查网络连通性
docker exec ai-agent-backend ping -c 3 117.72.152.117

# 检查端口连通性
docker exec ai-agent-backend nc -zv 117.72.152.117 13306

# 如果无法连接,检查:
# - 服务器防火墙规则
# - 数据库服务器白名单
```

### 3. 应用无法访问

```bash
# 检查容器是否运行
docker ps | grep ai-agent

# 检查端口映射
docker port ai-agent-backend

# 检查服务器防火墙
sudo ufw status
sudo ufw allow 8080
sudo ufw allow 80
```

### 4. 前端无法连接后端

检查前端环境变量配置,确保 API 地址正确:

```bash
# 进入前端容器
docker exec -it ai-agent-frontend sh

# 查看 nginx 配置
cat /etc/nginx/nginx.conf
```

---

## 🔄 更新部署

### 更新后端

```bash
cd /opt/ai-agent
git pull  # 或重新上传代码
./deploy.sh  # 使用部署脚本自动更新
```

### 手动更新

```bash
# 1. 重新构建镜像
docker build -t ai-agent-backend:latest .

# 2. 停止旧容器
docker stop ai-agent-backend
docker rm ai-agent-backend

# 3. 启动新容器
docker run -d \
  --name ai-agent-backend \
  -p 8080:8080 \
  --restart unless-stopped \
  -v /opt/ai-agent/logs:/app/data/log \
  ai-agent-backend:latest
```

---

## 📊 监控和维护

### 查看资源使用

```bash
# 查看容器资源使用情况
docker stats ai-agent-backend ai-agent-frontend

# 查看磁盘使用
docker system df
```

### 日志管理

```bash
# 查看实时日志
docker logs -f ai-agent-backend

# 查看最近 100 行日志
docker logs --tail 100 ai-agent-backend

# 导出日志
docker logs ai-agent-backend > backend.log 2>&1
```

### 清理旧镜像

```bash
# 清理未使用的镜像
docker image prune -a

# 清理所有未使用的资源
docker system prune -a
```

---

## 🛡️ 安全建议

1. **使用环境变量**: 不要在代码中硬编码敏感信息
2. **配置防火墙**: 只开放必要的端口
3. **定期更新**: 及时更新 Docker 和系统补丁
4. **日志审计**: 定期检查应用日志
5. **备份数据**: 定期备份数据库和配置文件

---

## 📞 技术支持

如遇到问题,请检查:
1. 容器日志: `docker logs ai-agent-backend`
2. 系统日志: `/var/log/syslog`
3. 应用日志: `/opt/ai-agent/logs/`

常用调试命令:
```bash
# 进入容器调试
docker exec -it ai-agent-backend sh

# 查看容器详细信息
docker inspect ai-agent-backend

# 查看网络配置
docker network inspect bridge
```
