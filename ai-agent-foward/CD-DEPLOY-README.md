# 前端应用 CD 部署指南

## 📋 部署脚本

### 1. cd-deploy.sh - 快速部署
**适用场景**: 开发/测试环境
- ✅ 部署速度快
- ⚠️ 有短暂服务中断（约5秒）

### 2. cd-deploy-rolling.sh - 滚动部署
**适用场景**: 生产环境
- ✅ 零停机部署
- ✅ 自动健康检查和回滚

## 🔧 环境变量配置

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `DOCKERNAME` | Docker仓库用户名 | - |
| `DOCKERPASSWORD` | Docker仓库密码 | - |
| `NAMESPACE` | 命名空间 | `zj669` |
| `REPO` | 仓库名称 | `agent-frontend` |
| `BUILD_TAG` | 构建标签 | `2025-12-24-20-08-01` |
| `CONTAINER_NAME` | 容器名称 | `ai-agent-frontend` |
| `PORT` | 服务端口 | `80` |
| **`VITE_API_BASE_URL`** | **后端API地址** | `http://81.69.37.254:8080` |
| `STARTUP_WAIT` | 容器启动等待时间（秒） | `5` |

## 🎯 关键特性

### 运行时配置后端API
前端Dockerfile支持在容器启动时通过环境变量配置后端API地址，无需重新构建镜像：

```bash
docker run -d \
  --name ai-agent-frontend \
  -p 80:80 \
  -e VITE_API_BASE_URL=http://your-backend-api:8080 \
  your-image:tag
```

### 工作原理
1. 容器启动时，`docker-entrypoint.sh` 脚本会生成 `config.js` 文件
2. 前端应用在运行时读取 `window.ENV_CONFIG.VITE_API_BASE_URL`
3. 无需重新构建镜像即可切换后端地址

## 🚀 使用示例

### 快速部署
```bash
NAMESPACE=zj669 \
REPO=agent-frontend \
BUILD_TAG=2025-12-24-20-08-01 \
CONTAINER_NAME=ai-agent-frontend \
PORT=80 \
VITE_API_BASE_URL=http://81.69.37.254:8080 \
./cd-deploy.sh
```

### 滚动部署
```bash
NAMESPACE=zj669 \
REPO=agent-frontend \
BUILD_TAG=2025-12-24-20-08-01 \
CONTAINER_NAME=ai-agent-frontend \
PORT=80 \
VITE_API_BASE_URL=http://81.69.37.254:8080 \
./cd-deploy-rolling.sh
```

## 📦 构建镜像

### 本地构建
```bash
cd app
docker build -t agent-frontend:latest .
```

### 推送到阿里云
```bash
# 打标签
docker tag agent-frontend:latest \
  crpi-gj68k07wqq52fpxi.cn-chengdu.personal.cr.aliyuncs.com/zj669/agent-frontend:2025-12-24-20-08-01

# 推送
docker push crpi-gj68k07wqq52fpxi.cn-chengdu.personal.cr.aliyuncs.com/zj669/agent-frontend:2025-12-24-20-08-01
```

## 🔍 健康检查

前端使用nginx提供服务，健康检查通过访问根路径 `/` 来验证：

```bash
docker exec ai-agent-frontend wget -q -O /dev/null http://localhost:80/
```

## 📝 前端代码修改

为了支持运行时配置，需要在前端代码中读取配置：

### 1. 在 index.html 中引入配置
```html
<script src="/config.js"></script>
```

### 2. 在代码中使用配置
```typescript
// 获取API基础URL
const apiBaseUrl = (window as any).ENV_CONFIG?.VITE_API_BASE_URL || import.meta.env.VITE_API_BASE_URL;

// 使用apiBaseUrl创建axios实例
const api = axios.create({
  baseURL: apiBaseUrl
});
```

## 🛠️ 故障排查

### 容器无法启动
```bash
docker logs ai-agent-frontend
```

### API地址配置错误
```bash
# 进入容器检查配置
docker exec ai-agent-frontend cat /usr/share/nginx/html/config.js
```

### Nginx配置问题
```bash
# 检查nginx配置
docker exec ai-agent-frontend nginx -t
```
