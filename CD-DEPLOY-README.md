# 部署脚本使用说明

## 📦 可用的CD部署脚本

### 1. cd-deploy.sh - 快速部署
**适用场景**: 开发/测试环境
- ✅ 部署速度快
- ⚠️ 有短暂服务中断（约10-15秒）
- ✅ 容器内部健康检查

### 2. cd-deploy-rolling.sh - 滚动部署
**适用场景**: 生产环境
- ✅ 零停机部署
- ✅ 自动健康检查和回滚
- ✅ 容器内部健康检查

## 🔧 环境变量配置

| 变量名 | 说明 | 当前项目值 |
|--------|------|-----------|
| `DOCKERNAME` | Docker仓库用户名 | (配置您的用户名) |
| `DOCKERPASSWORD` | Docker仓库密码 | (配置您的密码) |
| `NAMESPACE` | 命名空间 | `zj669` |
| `REPO` | 仓库名称 | `agent` |
| `BUILD_TAG` | 构建标签 | (由CI自动生成) |
| `CONTAINER_NAME` | 容器名称 | `ai-agent-backend` |
| `PORT` | 服务端口 | `8080` |

## 🎯 健康检查说明

两个脚本都使用 `docker exec` 在容器内部执行健康检查，避免Docker网络配置问题：

```bash
docker exec ${CONTAINER_NAME} curl -s -f http://localhost:8080/actuator/health
```

这种方式不依赖宿主机的网络配置，更加可靠。

## 🚀 使用建议

- **开发/测试环境**: 使用 `cd-deploy.sh`
- **生产环境**: 使用 `cd-deploy-rolling.sh`
