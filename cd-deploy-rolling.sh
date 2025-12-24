# AI Agent 滚动部署脚本（零停机）
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

# 创建数据目录并设置权限（容器内使用UID 1001）
mkdir -p /app/data/log
mkdir -p /app/data
chown -R 1001:1001 /app/data

# 拉取最新镜像
docker pull crpi-gj68k07wqq52fpxi.cn-chengdu.personal.cr.aliyuncs.com/${NAMESPACE}/${REPO}:${BUILD_TAG}

# 启动新容器（使用临时端口）
TEMP_PORT=8081
NEW_CONTAINER="${CONTAINER_NAME}-new"

docker run -d \
  --name ${NEW_CONTAINER} \
  -p ${TEMP_PORT}:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC" \
  -v /app/data/log:/app/data/log \
  -v /app/data:/app/data \
  crpi-gj68k07wqq52fpxi.cn-chengdu.personal.cr.aliyuncs.com/${NAMESPACE}/${REPO}:${BUILD_TAG}

# 健康检查新容器
echo "等待新容器启动..."
sleep ${STARTUP_WAIT}
HEALTH_CHECK_PASSED=false

for i in {1..30}; do
  # 使用docker exec在容器内部执行健康检查
  if docker exec ${NEW_CONTAINER} curl -s -f http://localhost:8080/public/health > /dev/null 2>&1; then
    echo "✅ 新容器健康检查通过"
    HEALTH_CHECK_PASSED=true
    break
  fi
  echo "等待新容器启动... ($i/30)"
  sleep 2
done

# 如果健康检查失败，清理并退出
if [ "$HEALTH_CHECK_PASSED" = false ]; then
  echo "❌ 新容器健康检查失败，回滚"
  docker logs ${NEW_CONTAINER} --tail 50
  docker stop ${NEW_CONTAINER} 2>/dev/null || true
  docker rm ${NEW_CONTAINER} 2>/dev/null || true
  exit 1
fi

# 停止旧容器
echo "停止旧容器..."
docker stop ${CONTAINER_NAME} 2>/dev/null || true
docker rename ${CONTAINER_NAME} ${CONTAINER_NAME}-old 2>/dev/null || true

# 重新启动新容器到生产端口
echo "切换到生产端口..."
docker stop ${NEW_CONTAINER}
docker rm ${NEW_CONTAINER}

docker run -d \
  --name ${CONTAINER_NAME} \
  -p ${PORT}:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC" \
  -v /app/data/log:/app/data/log \
  -v /app/data:/app/data \
  --restart unless-stopped \
  crpi-gj68k07wqq52fpxi.cn-chengdu.personal.cr.aliyuncs.com/${NAMESPACE}/${REPO}:${BUILD_TAG}

# 验证生产端口
echo "验证生产端口..."
sleep ${STARTUP_WAIT}
if docker exec ${CONTAINER_NAME} curl -s -f http://localhost:8080/public/health > /dev/null 2>&1; then
  echo "✅ 滚动部署成功"
  # 清理旧容器
  docker stop ${CONTAINER_NAME}-old 2>/dev/null || true
  docker rm ${CONTAINER_NAME}-old 2>/dev/null || true
  exit 0
else
  echo "❌ 生产端口验证失败"
  docker logs ${CONTAINER_NAME} --tail 50
  exit 1
fi
