# Windows 快速部署指南

## 🚀 一键部署(推荐)

### 使用 PowerShell 脚本

```powershell
# 1. 打开 PowerShell(以管理员身份运行)
# 2. 进入项目目录
cd d:\java\ai-agent

# 3. 允许执行脚本(首次使用需要)
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# 4. 执行部署脚本
.\quick-deploy.ps1
```

脚本会自动完成:
- ✓ 检查 SSH 连接
- ✓ 上传代码到服务器
- ✓ 检查并安装 Docker
- ✓ 构建镜像并启动容器
- ✓ 验证部署状态
- ✓ 显示访问地址

---

## 📋 前置要求

### 1. 安装 OpenSSH 客户端

Windows 10/11 通常已内置,检查方法:

```powershell
# 检查是否已安装
ssh -V

# 如果未安装,在 PowerShell(管理员)中执行:
Add-WindowsCapability -Online -Name OpenSSH.Client~~~~0.0.1.0
```

### 2. 配置 SSH 密钥(可选,推荐)

```powershell
# 生成 SSH 密钥
ssh-keygen -t rsa -b 4096

# 复制公钥到服务器
type $env:USERPROFILE\.ssh\id_rsa.pub | ssh root@81.69.37.254 "cat >> ~/.ssh/authorized_keys"
```

配置后可免密登录,部署更方便。

---

## 🔧 手动部署方式

### 方式一: 使用 WinSCP 上传

1. **下载 WinSCP**: https://winscp.net/
2. **连接服务器**:
   - 主机: `81.69.37.254`
   - 用户名: `root`
   - 密码: (您的密码)
3. **上传项目**: 将 `d:\java\ai-agent` 上传到 `/opt/ai-agent`
4. **SSH 登录并部署**:

```powershell
ssh root@81.69.37.254
cd /opt/ai-agent
chmod +x deploy.sh
./deploy.sh
```

### 方式二: 使用 SCP 命令

```powershell
# 上传代码
scp -r d:\java\ai-agent root@81.69.37.254:/opt/ai-agent

# SSH 登录
ssh root@81.69.37.254

# 执行部署
cd /opt/ai-agent
chmod +x deploy.sh
./deploy.sh
```

---

## ✅ 验证部署

### 在 PowerShell 中验证

```powershell
# 检查后端健康状态
Invoke-WebRequest -Uri "http://81.69.37.254:8080/actuator/health" | Select-Object -Expand Content

# 在浏览器中打开
Start-Process "http://81.69.37.254"
Start-Process "http://81.69.37.254:8080/actuator/health"
```

### 查看服务器日志

```powershell
# 查看后端日志
ssh root@81.69.37.254 "docker logs -f ai-agent-backend"

# 查看容器状态
ssh root@81.69.37.254 "docker ps"
```

---

## 🔄 更新部署

```powershell
# 直接运行部署脚本即可
cd d:\java\ai-agent
.\quick-deploy.ps1
```

脚本会自动:
1. 上传最新代码
2. 停止旧容器
3. 重新构建镜像
4. 启动新容器

---

## ❓ 常见问题

### 1. PowerShell 脚本无法执行

**错误**: "无法加载文件,因为在此系统上禁止运行脚本"

**解决**:
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### 2. SSH 连接失败

**错误**: "Connection refused" 或 "Connection timed out"

**解决**:
- 检查服务器 IP 是否正确
- 检查网络连接
- 确认服务器 SSH 服务已启动
- 检查防火墙设置

### 3. SCP 上传失败

**错误**: "Permission denied"

**解决**:
- 确认用户名和密码正确
- 确认目标目录有写入权限
- 尝试使用 WinSCP 等图形化工具

### 4. 需要输入密码多次

**解决**: 配置 SSH 密钥认证(见上方"配置 SSH 密钥"部分)

---

## 📊 管理命令

### 查看容器状态

```powershell
ssh root@81.69.37.254 "docker ps"
```

### 重启容器

```powershell
# 重启后端
ssh root@81.69.37.254 "docker restart ai-agent-backend"

# 重启前端
ssh root@81.69.37.254 "docker restart ai-agent-frontend"
```

### 查看日志

```powershell
# 实时查看后端日志
ssh root@81.69.37.254 "docker logs -f ai-agent-backend"

# 查看最近 100 行
ssh root@81.69.37.254 "docker logs --tail 100 ai-agent-backend"
```

### 停止容器

```powershell
ssh root@81.69.37.254 "docker stop ai-agent-backend ai-agent-frontend"
```

---

## 🌐 访问地址

部署成功后,可通过以下地址访问:

- **前端**: http://81.69.37.254
- **后端 API**: http://81.69.37.254:8080
- **健康检查**: http://81.69.37.254:8080/actuator/health
- **Prometheus 指标**: http://81.69.37.254:8080/actuator/prometheus

---

## 🛡️ 安全提示

1. **修改默认密码**: 部署后及时修改服务器 root 密码
2. **配置防火墙**: 只开放必要的端口(80, 8080)
3. **使用 SSH 密钥**: 禁用密码登录,只允许密钥认证
4. **定期更新**: 及时更新系统和 Docker

---

## 📞 技术支持

如遇到问题:

1. **查看脚本输出**: 脚本会显示详细的错误信息
2. **查看服务器日志**: `ssh root@81.69.37.254 "docker logs ai-agent-backend"`
3. **检查网络连接**: 确保可以 ping 通服务器
4. **参考详细文档**: [deploy-to-test-server.md](deploy-to-test-server.md)
