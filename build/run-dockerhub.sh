#!/bin/bash

#================================================================
# DockPilot Docker运行脚本
# 
# 功能：从DockerHub拉取镜像并运行DockPilot容器
# 默认：使用test测试版本，除非明确指定其他版本
#
# 使用方法：
#   ./run-dockerhub.sh                    # 使用test版本，8888端口
#   ./run-dockerhub.sh latest             # 使用latest版本，8888端口  
#   ./run-dockerhub.sh v1.0.0 9999        # 使用v1.0.0版本，9999端口
#
# 功能：
#   - 自动拉取最新镜像
#   - 自动清理旧容器
#   - 显示运行状态和常用命令
#================================================================

# 设置颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m'

print_message() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# 默认参数
VERSION=${1:-test}
CONTAINER_NAME="dockpilot"
IMAGE_NAME="kidself/dockpilot"
HOST_PORT=${2:-8888}

print_message "=========================================="
print_message "DockPilot Docker运行脚本"
print_message "镜像版本: $VERSION"
print_message "容器名称: $CONTAINER_NAME"
print_message "访问端口: $HOST_PORT"
print_message "=========================================="

# 检查Docker是否运行
if ! docker info >/dev/null 2>&1; then
    print_error "Docker未运行或无权限访问，请检查Docker服务状态"
    exit 1
fi

# 更新镜像
print_message "从DockerHub拉取最新镜像..."
if docker pull ${IMAGE_NAME}:${VERSION}; then
    print_message "✅ 镜像拉取成功"
else
    print_error "❌ 镜像拉取失败"
    exit 1
fi

# 清理旧容器
print_message "检查并清理旧容器..."
if docker ps -a --format "{{.Names}}" | grep -q "^${CONTAINER_NAME}$"; then
    print_warning "发现已存在的容器: $CONTAINER_NAME"
    
    # 停止容器（如果正在运行）
    if docker ps --format "{{.Names}}" | grep -q "^${CONTAINER_NAME}$"; then
        print_message "停止正在运行的容器..."
        docker stop $CONTAINER_NAME
    fi
    
    # 删除容器
    print_message "删除旧容器..."
    docker rm $CONTAINER_NAME
    print_message "✅ 旧容器已清理"
else
    print_message "未发现已存在的容器"
fi

# 运行新容器
print_message "启动新容器..."
docker run -d --privileged \
    --name $CONTAINER_NAME \
    -p $HOST_PORT:8888 \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v /:/mnt/host \
    -v dockpilot-data:/dockpilot \
    --restart unless-stopped \
    ${IMAGE_NAME}:${VERSION}

if [ $? -eq 0 ]; then
    print_message "✅ 容器启动成功！"
    print_message "容器名称: $CONTAINER_NAME"
    print_message "访问地址: http://localhost:$HOST_PORT"
    print_message "DockerHub镜像: ${IMAGE_NAME}:${VERSION}"
else
    print_error "❌ 容器启动失败"
    exit 1
fi

# 显示容器状态
echo ""
print_message "容器运行状态："
docker ps --filter "name=$CONTAINER_NAME" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo ""
print_message "常用命令："
echo "查看日志: docker logs -f $CONTAINER_NAME"
echo "停止容器: docker stop $CONTAINER_NAME"
echo "重启容器: docker restart $CONTAINER_NAME"

print_message "=========================================="
print_message "DockPilot 启动完成！"
print_message "==========================================" 