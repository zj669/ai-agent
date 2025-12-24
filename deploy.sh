#!/bin/bash

# AI Agent 一键部署脚本
# 使用方法: ./deploy.sh

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查 Docker 是否安装
check_docker() {
    log_info "检查 Docker 环境..."
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装,请先安装 Docker"
        exit 1
    fi
    log_info "Docker 版本: $(docker --version)"
}

# 构建后端镜像
build_backend() {
    log_info "开始构建后端镜像..."
    docker build -t ai-agent-backend:latest . || {
        log_error "后端镜像构建失败"
        exit 1
    }
    log_info "后端镜像构建成功"
}

# 构建前端镜像
build_frontend() {
    log_info "开始构建前端镜像..."
    if [ -d "app" ] && [ -f "app/Dockerfile" ]; then
        cd app
        docker build -t ai-agent-frontend:latest . || {
            log_error "前端镜像构建失败"
            exit 1
        }
        cd ..
        log_info "前端镜像构建成功"
    else
        log_warn "未找到前端 Dockerfile,跳过前端构建"
    fi
}

# 停止并删除旧容器
stop_old_containers() {
    log_info "停止旧容器..."
    
    # 停止后端容器
    if docker ps -a | grep -q ai-agent-backend; then
        docker stop ai-agent-backend 2>/dev/null || true
        docker rm ai-agent-backend 2>/dev/null || true
        log_info "已停止旧的后端容器"
    fi
    
    # 停止前端容器
    if docker ps -a | grep -q ai-agent-frontend; then
        docker stop ai-agent-frontend 2>/dev/null || true
        docker rm ai-agent-frontend 2>/dev/null || true
        log_info "已停止旧的前端容器"
    fi
}

# 启动后端容器
start_backend() {
    log_info "启动后端容器..."
    
    # 创建日志目录
    mkdir -p logs
    
    docker run -d \
        --name ai-agent-backend \
        -p 8080:8080 \
        --restart unless-stopped \
        -v "$(pwd)/logs:/app/data/log" \
        ai-agent-backend:latest || {
        log_error "后端容器启动失败"
        exit 1
    }
    
    log_info "后端容器启动成功"
}

# 启动前端容器
start_frontend() {
    if docker images | grep -q ai-agent-frontend; then
        log_info "启动前端容器..."
        
        docker run -d \
            --name ai-agent-frontend \
            -p 80:80 \
            --restart unless-stopped \
            ai-agent-frontend:latest || {
            log_error "前端容器启动失败"
            exit 1
        }
        
        log_info "前端容器启动成功"
    else
        log_warn "前端镜像不存在,跳过前端启动"
    fi
}

# 健康检查
health_check() {
    log_info "等待应用启动..."
    sleep 10
    
    log_info "执行健康检查..."
    
    # 检查后端
    if curl -f http://localhost:8080/actuator/health &> /dev/null; then
        log_info "✓ 后端健康检查通过"
    else
        log_warn "✗ 后端健康检查失败,请查看日志"
    fi
    
    # 检查前端
    if docker ps | grep -q ai-agent-frontend; then
        if curl -f -I http://localhost:80 &> /dev/null; then
            log_info "✓ 前端健康检查通过"
        else
            log_warn "✗ 前端健康检查失败,请查看日志"
        fi
    fi
}

# 显示容器状态
show_status() {
    log_info "容器状态:"
    docker ps | grep ai-agent || log_warn "未找到运行中的容器"
    
    echo ""
    log_info "查看后端日志: docker logs -f ai-agent-backend"
    log_info "查看前端日志: docker logs -f ai-agent-frontend"
    echo ""
    log_info "访问地址:"
    log_info "  前端: http://localhost"
    log_info "  后端: http://localhost:8080"
    log_info "  健康检查: http://localhost:8080/actuator/health"
}

# 主函数
main() {
    echo "========================================="
    echo "   AI Agent 一键部署脚本"
    echo "========================================="
    echo ""
    
    check_docker
    build_backend
    build_frontend
    stop_old_containers
    start_backend
    start_frontend
    health_check
    show_status
    
    echo ""
    log_info "部署完成!"
}

# 执行主函数
main
