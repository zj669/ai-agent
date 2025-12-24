# 部署到测试服务器 81.69.37.254

## 🚀 快速部署步骤

### 1. 上传项目到服务器

在本地 Windows PowerShell 中执行:

```powershell
# 方式一: 使用 SCP 上传(需要安装 OpenSSH 或使用 Git Bash)
scp -r d:\java\ai-agent root@81.69.37.254:/opt/ai-agent

# 方式二: 使用 WinSCP 或 FileZilla 等工具上传
# 将 d:\java\ai-agent 整个目录上传到服务器的 /opt/ai-agent
```

---

### 2. SSH 登录服务器

```powershell
ssh root@81.69.37.254
```

---

### 3. 检查 Docker 环境

```bash
# 检查 Docker 是否安装
docker --version

# 如果未安装,执行以下命令安装 Docker
curl -fsSL https://get.docker.com | sh
systemctl start docker
systemctl enable docker
```

---

### 4. 执行一键部署

```bash
cd /opt/ai-agent

# 赋予脚本执行权限
chmod +x deploy.sh

# 执行部署
./deploy.sh
```

部署脚本会自动完成:
- ✓ 构建后端镜像
- ✓ 构建前端镜像
- ✓ 停止旧容器
- ✓ 启动新容器
- ✓ 执行健康检查

---

### 5. 验证部署

```bash
# 查看容器状态
docker ps

# 查看后端日志
docker logs -f ai-agent-backend

# 健康检查
curl http://localhost:8080/actuator/health

# 预期输出: {"status":"UP"}
```

---

### 6. 配置防火墙

```bash
# 开放端口 80 和 8080
firewall-cmd --permanent --add-port=80/tcp
firewall-cmd --permanent --add-port=8080/tcp
firewall-cmd --reload

# 或使用 ufw (Ubuntu)
ufw allow 80
ufw allow 8080
```

---

### 7. 访问应用

- **前端**: http://81.69.37.254
- **后端 API**: http://81.69.37.254:8080
- **健康检查**: http://81.69.37.254:8080/actuator/health
- **Prometheus 指标**: http://81.69.37.254:8080/actuator/prometheus

---

## 🔧 常见问题处理

### 问题 1: 端口被占用

```bash
# 查看端口占用
netstat -tlnp | grep 8080

# 停止占用端口的进程
kill -9 <PID>

# 或修改端口映射
docker run -d --name ai-agent-backend -p 8081:8080 ...
```

### 问题 2: 无法连接外部数据库

```bash
# 测试网络连通性
ping 117.72.152.117

# 测试端口连通性
telnet 117.72.152.117 13306

# 如果无法连接,检查服务器出站规则
```

### 问题 3: 容器启动失败

```bash
# 查看详细日志
docker logs ai-agent-backend

# 查看容器详细信息
docker inspect ai-agent-backend

# 进入容器调试
docker exec -it ai-agent-backend sh
```

### 问题 4: 前端无法连接后端

检查前端的 API 配置,确保指向正确的后端地址:

```bash
# 进入前端容器
docker exec -it ai-agent-frontend sh

# 查看 nginx 配置
cat /etc/nginx/nginx.conf
```

如果需要修改前端 API 地址,需要在构建前端镜像前修改环境变量。

---

## 📊 监控和维护

### 查看日志

```bash
# 实时查看后端日志
docker logs -f ai-agent-backend

# 查看最近 100 行
docker logs --tail 100 ai-agent-backend

# 导出日志
docker logs ai-agent-backend > /tmp/backend.log 2>&1
```

### 查看资源使用

```bash
# 查看容器资源使用
docker stats ai-agent-backend ai-agent-frontend

# 查看磁盘使用
df -h
docker system df
```

### 重启容器

```bash
# 重启后端
docker restart ai-agent-backend

# 重启前端
docker restart ai-agent-frontend
```

---

## 🔄 更新应用

### 方式一: 使用部署脚本

```bash
cd /opt/ai-agent

# 重新上传代码后,直接执行
./deploy.sh
```

### 方式二: 手动更新

```bash
# 1. 重新构建镜像
docker build -t ai-agent-backend:latest .

# 2. 停止并删除旧容器
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

## 🛡️ 安全建议

1. **修改默认端口**: 考虑使用非标准端口
2. **配置 HTTPS**: 使用 Nginx 反向代理 + Let's Encrypt SSL 证书
3. **限制访问**: 配置防火墙规则,只允许必要的 IP 访问
4. **定期备份**: 备份数据库和配置文件
5. **监控日志**: 定期检查应用日志,发现异常及时处理

---

## 📞 快速命令参考

```bash
# 查看所有容器
docker ps -a

# 停止所有容器
docker stop $(docker ps -q)

# 删除所有容器
docker rm $(docker ps -aq)

# 清理未使用的镜像
docker image prune -a

# 查看 Docker 磁盘使用
docker system df

# 完全清理 Docker
docker system prune -a --volumes
```

---

## ✅ 部署检查清单

- [ ] 项目已上传到 `/opt/ai-agent`
- [ ] Docker 已安装并运行
- [ ] 执行 `./deploy.sh` 成功
- [ ] 容器状态正常: `docker ps`
- [ ] 后端健康检查通过: `curl http://localhost:8080/actuator/health`
- [ ] 防火墙已开放端口 80 和 8080
- [ ] 可以通过浏览器访问: http://81.69.37.254
- [ ] 后端 API 可访问: http://81.69.37.254:8080/actuator/health
- [ ] 数据库连接成功(查看日志确认)
- [ ] Redis 连接成功(查看日志确认)

---

## 🎯 下一步优化(可选)

1. **配置域名**: 将域名解析到 81.69.37.254
2. **配置 HTTPS**: 使用 Certbot 自动申请 SSL 证书
3. **配置 Nginx 反向代理**: 统一入口,隐藏后端端口
4. **配置日志轮转**: 防止日志文件过大
5. **配置监控告警**: 使用 Prometheus + Grafana 监控应用状态
