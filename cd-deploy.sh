# AI Agent 部署脚本
# 环境变量说明：
# ${DOCKERNAME} - Docker仓库用户名
# ${DOCKERPASSWORD} - Docker仓库密码
# ${NAMESPACE} - 命名空间，值为: zj669
# ${REPO} - 仓库名称，值为: agent
# ${BUILD_TAG} - 构建标签（时间戳）
# ${CONTAINER_NAME} - 容器名称，值为: ai-agent-backend
# ${PORT} - 服务端口，值为: 8080
# ${STARTUP_WAIT} - 容器启动等待时间（秒），默认: 10

# 设置默认值
STARTUP_WAIT=${STARTUP_WAIT:-10}

# 登录阿里云镜像仓库
echo ${DOCKERPASSWORD} | docker login --username ${DOCKERNAME} --password-stdin crpi-gj68k07wqq52fpxi.cn-chengdu.personal.cr.aliyuncs.com

# 停止并删除旧容器
docker stop ${CONTAINER_NAME} 2>/dev/null || true
docker rm ${CONTAINER_NAME} 2>/dev/null || true

# 创建数据目录并设置权限（容器内使用UID 1001）
mkdir -p /app/data/log
mkdir -p /app/data
chown -R 1001:1001 /app/data

# 拉取最新镜像
docker pull crpi-gj68k07wqq52fpxi.cn-chengdu.personal.cr.aliyuncs.com/${NAMESPACE}/${REPO}:${BUILD_TAG}

# 启动新容器
docker run -d \
  --name ${CONTAINER_NAME} \
  -p ${PORT}:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC" \
  -v /app/data/log:/app/data/log \
  -v /app/data:/app/data \
  --restart unless-stopped \
  crpi-gj68k07wqq52fpxi.cn-chengdu.personal.cr.aliyuncs.com/${NAMESPACE}/${REPO}:${BUILD_TAG}

# 健康检查
echo "等待容器启动..."
sleep ${STARTUP_WAIT}
for i in {1..30}; do
  # 使用docker exec在容器内部执行健康检查
  if docker exec ${CONTAINER_NAME} curl -s -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ 部署成功"
    exit 0
  fi
  echo "等待服务启动... ($i/30)"
  sleep 2
done

echo "❌ 健康检查失败"
docker logs ${CONTAINER_NAME} --tail 50
exit 1
