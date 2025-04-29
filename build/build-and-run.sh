#!/bin/bash

# 设置颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

print_message() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 进入build目录
cd "$(dirname "$0")"

# 构建Docker镜像
print_message "开始构建Docker镜像..."
docker build -t docker-manager .

# 停止并删除旧容器
docker stop docker-manager-container 2>/dev/null || true
docker rm docker-manager-container 2>/dev/null || true

# 运行新容器
print_message "启动新容器..."
docker run -d --privileged \
    -p 8081:80 \
    -p 8082:8080 \
    --name docker-manager-container \
    -v /var/run/docker.sock:/var/run/docker.sock \
    docker-manager

print_message "容器已启动！"
print_message "前端访问地址: http://localhost:8081"
print_message "后端API地址: http://localhost:8082/api"