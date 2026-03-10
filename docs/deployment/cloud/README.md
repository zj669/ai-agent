# 云端 HTTP 重置部署清单

> 适用场景：单机 Docker、按 IP 先落地、全量重置旧的 `ai-agent` 半成品部署，再迁入本地最新数据。

## 1. 本地准备发布物

```bash
cd E:/WorkSpace/repo/ai-agent

# 后端发布包
mvn -pl ai-agent-interfaces -am package "-Dmaven.test.skip=true"

# 前端静态产物
cd ai-agent-foward
npm ci
npm run build
cd ..
```

## 2. 组装 release 目录

建议在本地生成如下目录结构：

```text
release/
├── backend/
│   └── ai-agent-interfaces-1.0.0-SNAPSHOT.jar
├── frontend-dist/
├── ai-agent-foward/
│   └── nginx.conf
├── docker/
│   ├── docker-compose.yml
│   └── init/mysql/...
├── docker-compose.app.yml
└── .env.http
```

其中：
- `backend/ai-agent-interfaces-1.0.0-SNAPSHOT.jar` 来自 `ai-agent-interfaces/target/`
- `frontend-dist/` 来自 `ai-agent-foward/dist/`
- `docker/` 直接复制 `ai-agent-infrastructure/src/main/resources/docker/`
- `docker-compose.app.yml` 取仓库根目录同名文件
- `nginx.conf` 取 `ai-agent-foward/nginx.conf`
- `.env.http` 可参考 `docs/deployment/cloud/.env.http.example`

## 3. 远端准备

```bash
mkdir -p /opt/ai-agent
cd /opt/ai-agent
mkdir -p release
```

上传 `release/` 目录后执行：

```bash
bash release/preflight-verify.sh all /opt/ai-agent/release/.env.http /opt/ai-agent/release
```

## 4. 启动基础设施

基础设施沿用仓库里的开发版 compose，但统一指定项目名 `docker`，让应用层 compose 可以复用现成网络 `docker_ai-agent-network`：

```bash
cd /opt/ai-agent/release/docker
docker compose -p docker up -d
docker compose -p docker ps
# 若宿主机只有 v1，可将上述命令替换为 docker-compose
```

## 5. 启动前后端

```bash
cd /opt/ai-agent/release
docker compose --env-file .env.http up -d
docker compose ps
# 若宿主机只有 v1，可将上述命令替换为 docker-compose
```

## 6. 健康检查

```bash
bash /opt/ai-agent/release/check-cloud-deploy.sh all \
  http://127.0.0.1:8080/actuator/health \
  http://127.0.0.1/
```

## 7. 数据恢复顺序

推荐顺序：

1. 启动基础设施
2. 导入 MySQL dump
3. 恢复 MinIO `knowledge-files` 数据
4. 启动应用层 compose
5. 做页面、登录、流式接口验收
