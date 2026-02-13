# 前端应用部署脚本
# 环境变量说明：
# ${DOCKERNAME} - Docker仓库用户名
# ${DOCKERPASSWORD} - Docker仓库密码
# ${NAMESPACE} - 命名空间，值为: zj669
# ${REPO} - 仓库名称，值为: agent-frontend
# ${BUILD_TAG} - 构建标签（时间戳）
# ${CONTAINER_NAME} - 容器名称，值为: ai-agent-frontend
# ${PORT} - 服务端口，值为: 80
# ${STARTUP_WAIT} - 容器启动等待时间（秒），默认: 5

# 设置默认值
STARTUP_WAIT=${STARTUP_WAIT:-5}

# 登录阿里云镜像仓库
echo ${DOCKERPASSWORD} | docker login --username ${DOCKERNAME} --password-stdin crpi-gj68k07wqq52fpxi.cn-chengdu.personal.cr.aliyuncs.com

# 停止并删除旧容器
docker stop ${CONTAINER_NAME} 2>/dev/null || true
docker rm ${CONTAINER_NAME} 2>/dev/null || true

# 拉取最新镜像
docker pull crpi-gj68k07wqq52fpxi.cn-chengdu.personal.cr.aliyuncs.com/${NAMESPACE}/${REPO}:${BUILD_TAG}

# 启动新容器
docker run -d \
  --name ${CONTAINER_NAME} \
  -p ${PORT}:80 \
  --restart unless-stopped \
  crpi-gj68k07wqq52fpxi.cn-chengdu.personal.cr.aliyuncs.com/${NAMESPACE}/${REPO}:${BUILD_TAG}

# 健康检查
echo "等待容器启动..."
sleep ${STARTUP_WAIT}
for i in {1..20}; do
  # 检查nginx是否响应
  if docker exec ${CONTAINER_NAME} wget -q -O /dev/null http://localhost:80/ 2>/dev/null; then
    echo "✅ 部署成功"
    exit 0
  fi
  echo "等待服务启动... ($i/20)"
  sleep 2
done

echo "❌ 健康检查失败"
docker logs ${CONTAINER_NAME} --tail 50
exit 1
