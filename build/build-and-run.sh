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
docker build -t dockpilot .

# 停止并删除旧容器
docker stop dockpilot 2>/dev/null || true
docker rm dockpilot 2>/dev/null || true

# 运行新容器
print_message "启动新容器..."
docker run -d --privileged \
   -p 8888:80 \
   --name dockpilot \
   -v /var/run/docker.sock:/var/run/docker.sock \
   -v /:/mnt/host \
   kidself/dockpilot:latest

print_message "容器已启动！"
print_message "前端访问地址: http://localhost:8888"
