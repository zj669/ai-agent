#!/bin/bash

# 快速部署到测试服务器 81.69.37.254
# 使用方法: ./quick-deploy.sh

SERVER_IP="81.69.37.254"
SERVER_USER="root"
REMOTE_PATH="/opt/ai-agent"
LOCAL_PATH="."

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}   AI Agent 快速部署到测试服务器${NC}"
echo -e "${BLUE}   服务器: ${SERVER_IP}${NC}"
echo -e "${BLUE}=========================================${NC}"
echo ""

# 步骤 1: 上传代码
echo -e "${GREEN}[1/4]${NC} 上传代码到服务器..."
rsync -avz --progress \
  --exclude 'node_modules' \
  --exclude 'target' \
  --exclude '.git' \
  --exclude 'logs' \
  ${LOCAL_PATH}/ ${SERVER_USER}@${SERVER_IP}:${REMOTE_PATH}/

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ 代码上传成功${NC}"
else
    echo -e "${RED}✗ 代码上传失败${NC}"
    exit 1
fi

echo ""

# 步骤 2: 检查 Docker 环境
echo -e "${GREEN}[2/4]${NC} 检查服务器 Docker 环境..."
ssh ${SERVER_USER}@${SERVER_IP} "docker --version" > /dev/null 2>&1

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Docker 已安装${NC}"
else
    echo -e "${YELLOW}! Docker 未安装,正在安装...${NC}"
    ssh ${SERVER_USER}@${SERVER_IP} "curl -fsSL https://get.docker.com | sh && systemctl start docker && systemctl enable docker"
    echo -e "${GREEN}✓ Docker 安装完成${NC}"
fi

echo ""

# 步骤 3: 执行部署
echo -e "${GREEN}[3/4]${NC} 执行部署脚本..."
ssh ${SERVER_USER}@${SERVER_IP} "cd ${REMOTE_PATH} && chmod +x deploy.sh && ./deploy.sh"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ 部署成功${NC}"
else
    echo -e "${RED}✗ 部署失败${NC}"
    exit 1
fi

echo ""

# 步骤 4: 验证部署
echo -e "${GREEN}[4/4]${NC} 验证部署..."
sleep 5

# 检查健康状态
HEALTH_CHECK=$(ssh ${SERVER_USER}@${SERVER_IP} "curl -s http://localhost:8080/actuator/health")

if [[ $HEALTH_CHECK == *"UP"* ]]; then
    echo -e "${GREEN}✓ 后端健康检查通过${NC}"
else
    echo -e "${YELLOW}! 后端健康检查失败,请查看日志${NC}"
fi

echo ""
echo -e "${BLUE}=========================================${NC}"
echo -e "${GREEN}部署完成!${NC}"
echo ""
echo -e "访问地址:"
echo -e "  前端: ${BLUE}http://${SERVER_IP}${NC}"
echo -e "  后端: ${BLUE}http://${SERVER_IP}:8080${NC}"
echo -e "  健康检查: ${BLUE}http://${SERVER_IP}:8080/actuator/health${NC}"
echo ""
echo -e "查看日志:"
echo -e "  ${YELLOW}ssh ${SERVER_USER}@${SERVER_IP} 'docker logs -f ai-agent-backend'${NC}"
echo -e "${BLUE}=========================================${NC}"
