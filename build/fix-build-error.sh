#!/bin/bash

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

VERSION=${1:-test}

print_message "=========================================="
print_message "Docker构建错误修复脚本"
print_message "版本: $VERSION"
print_message "=========================================="

# 1. 检查磁盘空间
print_message "检查磁盘空间..."
df -h
echo

# 2. 检查Docker磁盘使用
print_message "检查Docker磁盘使用..."
docker system df
echo

# 3. 清理Docker系统
print_message "清理Docker系统..."
docker system prune -a -f
echo

# 4. 清理构建缓存
print_message "清理构建缓存..."
docker builder prune -a -f
echo

# 5. 停止并删除可能冲突的容器
print_message "停止并删除可能冲突的容器..."
docker stop dockpilot-$VERSION 2>/dev/null || true
docker rm dockpilot-$VERSION 2>/dev/null || true
echo

# 6. 删除可能存在的镜像
print_message "删除可能存在的旧镜像..."
docker rmi dockpilot:$VERSION 2>/dev/null || true
docker rmi kidself/dockpilot:$VERSION 2>/dev/null || true
echo

# 7. 检查所需文件是否存在
print_message "检查构建文件..."
if [ ! -d "dist" ]; then
    print_error "dist目录不存在！请先构建前端。"
    exit 1
fi

if [ ! -f "docker-manager-back-1.0.0.jar" ]; then
    print_error "docker-manager-back-1.0.0.jar不存在！请先构建后端。"
    exit 1
fi

if [ ! -f "nginx.conf" ]; then
    print_error "nginx.conf不存在！"
    exit 1
fi

if [ ! -f "start.sh" ]; then
    print_error "start.sh不存在！"
    exit 1
fi

print_message "所有必需文件已确认存在"
echo

# 8. 重新构建镜像（不使用缓存）
print_message "重新构建Docker镜像（不使用缓存）..."
docker build --no-cache -t dockpilot:$VERSION .

if [ $? -eq 0 ]; then
    print_message "Docker镜像构建成功！"
    print_message "镜像: dockpilot:$VERSION"
    
    # 询问是否运行容器
    read -p "是否现在运行容器？(y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_message "启动容器..."
        docker run -d --privileged \
           -p 8888:8888 \
           --name dockpilot-$VERSION \
           -v /var/run/docker.sock:/var/run/docker.sock \
           -v /:/mnt/host \
           -v /dockpilot-$VERSION:/dockpilot \
           dockpilot:$VERSION
        
        print_message "容器已启动！"
        print_message "访问地址: http://localhost:8888"
    fi
else
    print_error "Docker镜像构建失败！"
    print_error "如果问题仍然存在，请尝试重启Docker daemon:"
    print_error "sudo systemctl restart docker"
    exit 1
fi

print_message "=========================================="
print_message "修复完成！"
print_message "==========================================" 