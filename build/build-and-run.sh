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

# 获取版本标签参数，默认为test
VERSION=${1:-test}

# 进入build目录
cd "$(dirname "$0")"

# 构建Docker镜像
print_message "开始构建Docker镜像 (版本: $VERSION)..."
docker build -t dockpilot:$VERSION .

if [ $? -ne 0 ]; then
    print_error "Docker镜像构建失败"
    exit 1
fi

# 验证端口配置
print_message "验证镜像端口配置..."
if [ -f "./verify-ports.sh" ]; then
    ./verify-ports.sh dockpilot:$VERSION
    if [ $? -ne 0 ]; then
        print_error "端口配置验证失败，请检查nginx配置"
        exit 1
    fi
else
    print_error "端口验证脚本不存在，跳过验证"
fi

# 停止并删除旧容器
docker stop dockpilot-$VERSION 2>/dev/null || true
docker rm dockpilot-$VERSION 2>/dev/null || true

# 运行新容器
print_message "启动新容器 (版本: $VERSION)..."
docker run -d --privileged \
   -p 8888:8888 \
   --name dockpilot-$VERSION \
   -v /var/run/docker.sock:/var/run/docker.sock \
   -v /:/mnt/host \
   -v /dockpilot-$VERSION:/dockpilot \
   dockpilot:$VERSION

print_message "容器已启动！"
print_message "版本: $VERSION"
print_message "容器名称: dockpilot-$VERSION"
print_message "前端访问地址: http://localhost:8888"
